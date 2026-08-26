package dev.maicra.pickrelay.client;

import dev.maicra.pickrelay.client.inventory.InventoryRelayManager;
import dev.maicra.pickrelay.client.progress.MiningProgressTracker;
import dev.maicra.pickrelay.client.safety.SafetyMonitor;
import dev.maicra.pickrelay.client.tool.ToolTracker;
import dev.maicra.pickrelay.session.RelayEntry;
import dev.maicra.pickrelay.session.RelayEntryStatus;
import dev.maicra.pickrelay.session.RelayQueue;
import dev.maicra.pickrelay.session.RelayMiningMode;
import dev.maicra.pickrelay.session.RelayState;
import dev.maicra.pickrelay.session.RelayValidationResult;
import dev.maicra.pickrelay.session.RelayValidator;
import dev.maicra.pickrelay.session.StopReason;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.ClientHooks;

import java.util.HashSet;
import java.util.Set;

public final class PickRelayController {
    private static final RelayQueue CONFIGURED_QUEUE = new RelayQueue();
    private static final MiningProgressTracker PROGRESS = new MiningProgressTracker();
    private static final SafetyMonitor SAFETY = new SafetyMonitor();

    private static RelayState state = RelayState.IDLE;
    private static StopReason lastStopReason;
    private static int activeIndex = -1;
    private static int transitionCooldownTicks;
    private static boolean controlledAttackInvocation;
    private static RelayMiningMode configuredMiningMode = RelayMiningMode.LINE_MINING;
    private static RelayMiningMode sessionMiningMode;
    private static BlockPos singleBlockTarget;
    private static boolean waitingForWorkBlock;

    private PickRelayController() {
    }

    public static RelayState state() {
        return state;
    }

    public static RelayQueue queue() {
        return CONFIGURED_QUEUE;
    }

    public static boolean isActive() {
        return state == RelayState.ACTIVE;
    }

    public static boolean canEditQueue() {
        return !isActive() && state != RelayState.STARTING && state != RelayState.STOPPING;
    }

    public static StopReason lastStopReason() {
        return lastStopReason;
    }

    public static int activeIndex() {
        return activeIndex;
    }

    public static RelayMiningMode miningMode() {
        return isActive() && sessionMiningMode != null ? sessionMiningMode : configuredMiningMode;
    }

    public static void setMiningMode(RelayMiningMode mode) {
        if (mode != null && canEditQueue()) {
            configuredMiningMode = mode;
        }
    }

    public static BlockPos singleBlockTarget() {
        return singleBlockTarget;
    }

    public static boolean isWaitingForWorkBlock() {
        return waitingForWorkBlock;
    }

    public static RelayEntry activeEntry() {
        if (activeIndex < 0 || activeIndex >= CONFIGURED_QUEUE.size()) {
            return null;
        }
        return CONFIGURED_QUEUE.get(activeIndex);
    }

    /**
     * True only while Pick Relay itself is synchronously executing vanilla's
     * start/continue attack method. The block-destroy mixin uses this as a
     * provenance guard so unrelated client mods cannot accidentally advance
     * a Blocks Broken budget while a relay session is active.
     */
    public static boolean isControlledAttackInvocation() {
        return controlledAttackInvocation;
    }

    public static void enterConfiguration() {
        if (state == RelayState.IDLE) {
            state = RelayState.CONFIGURING;
        }
    }

    public static void leaveConfiguration() {
        if (state == RelayState.CONFIGURING) {
            state = RelayState.IDLE;
        }
    }

    public static RelayValidationResult validation() {
        Minecraft minecraft = Minecraft.getInstance();
        RelayValidationResult queueValidation = RelayValidator.validate(CONFIGURED_QUEUE, minecraft.player);
        if (!queueValidation.valid()) {
            return queueValidation;
        }
        if (currentWorkBlock(minecraft) == null) {
            return new RelayValidationResult(
                    false,
                    Set.of(),
                    Component.translatable("screen.pickrelay.validation.no_work_target")
            );
        }
        return queueValidation;
    }

    public static boolean canStart() {
        if (isActive() || state == RelayState.STARTING || state == RelayState.STOPPING) {
            return false;
        }
        return validation().valid();
    }

    public static boolean start() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        RelayValidationResult validation = validation();
        if (!validation.valid() || player == null) {
            return false;
        }

        BlockHitResult initialWorkBlock = raycastCurrentBlock(minecraft);
        if (initialWorkBlock == null) {
            return false;
        }

        state = RelayState.STARTING;
        lastStopReason = null;
        activeIndex = -1;
        transitionCooldownTicks = 0;
        controlledAttackInvocation = false;
        waitingForWorkBlock = false;
        sessionMiningMode = configuredMiningMode;
        singleBlockTarget = sessionMiningMode == RelayMiningMode.SINGLE_BLOCK
                ? initialWorkBlock.getBlockPos().immutable()
                : null;
        CONFIGURED_QUEUE.resetRuntime();
        SAFETY.arm(player);

        RelayDebug.log("Session starting with {} entries in {} mode{}",
                CONFIGURED_QUEUE.size(),
                sessionMiningMode,
                singleBlockTarget == null ? "" : " at " + singleBlockTarget);

        if (!prepareNextEntry(null)) {
            return false;
        }

        state = RelayState.ACTIVE;
        player.displayClientMessage(Component.translatable("message.pickrelay.started"), true);
        return true;
    }

    public static void stop(StopReason reason) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean wasRunning = state == RelayState.ACTIVE || state == RelayState.STARTING || state == RelayState.STOPPING;

        if (wasRunning) {
            state = RelayState.STOPPING;
        }
        lastStopReason = reason;
        releaseControlledAttack();
        controlledAttackInvocation = false;
        SAFETY.clear();
        activeIndex = -1;
        transitionCooldownTicks = 0;
        sessionMiningMode = null;
        singleBlockTarget = null;
        waitingForWorkBlock = false;

        if (wasRunning || reason == StopReason.DISCONNECT || reason == StopReason.DIMENSION_CHANGE) {
            CONFIGURED_QUEUE.clear();
        }

        state = RelayState.IDLE;
        RelayDebug.log("Session stopped: {}", reason);

        if (wasRunning && minecraft.player != null) {
            minecraft.player.displayClientMessage(stopMessage(reason), true);
        }
    }

    public static void tick() {
        if (!isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null) {
            stop(StopReason.DISCONNECT);
            return;
        }

        StopReason unsafeReason = SAFETY.check(minecraft);
        if (unsafeReason != null) {
            stop(unsafeReason);
            return;
        }

        RelayEntry entry = activeEntry();
        if (entry == null) {
            stop(StopReason.INTERNAL_SAFETY);
            return;
        }

        if (transitionCooldownTicks > 0) {
            releaseControlledAttack();
            transitionCooldownTicks--;
            return;
        }

        ItemStack liveStack = ToolTracker.liveStack(entry, player);
        if (liveStack.isEmpty()) {
            if (entry.status() == RelayEntryStatus.BROKEN) {
                RelayDebug.log("Entry {} was confirmed broken by Pick Relay", activeIndex + 1);
                advance(entry);
            } else {
                RelayDebug.log("Active entry {} disappeared without a relay-confirmed break", activeIndex + 1);
                stop(StopReason.TOOL_INVALID);
            }
            return;
        }

        StopReason integrityReason = runtimeQueueIntegrity(player);
        if (integrityReason != null) {
            stop(integrityReason);
            return;
        }

        if (!ToolTracker.matchesExpectedSlot(entry, player)) {
            stop(StopReason.TOOL_INVALID);
            return;
        }
        if (entry.currentInventorySlot() < 0 || entry.currentInventorySlot() >= 9) {
            stop(StopReason.INVENTORY_DESYNC);
            return;
        }

        InventoryRelayManager.enforceSelected(entry, player);
        PROGRESS.observe(entry, liveStack);

        MiningProgressTracker.Completion completion = PROGRESS.completion(entry, liveStack);
        if (completion == MiningProgressTracker.Completion.PRESERVED) {
            entry.rememberLiveStack(liveStack);
            entry.setStatus(RelayEntryStatus.PRESERVED);
            RelayDebug.log("Entry {} preserved at {} remaining durability", activeIndex + 1, MiningProgressTracker.remainingDurability(liveStack));
            advance(entry);
            return;
        }
        if (completion == MiningProgressTracker.Completion.TARGET_REACHED) {
            entry.rememberLiveStack(liveStack);
            entry.setStatus(RelayEntryStatus.COMPLETED);
            RelayDebug.log("Entry {} reached its {} target ({}/{})", activeIndex + 1, entry.workMode(), entry.progress(), entry.workTarget());
            advance(entry);
            return;
        }

        holdControlledAttack();
    }

    public static int captureActiveDamageBeforeDestroy() {
        Minecraft minecraft = Minecraft.getInstance();
        RelayEntry entry = activeEntry();
        if (!isActive() || !controlledAttackInvocation || entry == null || minecraft.player == null) {
            return -1;
        }
        ItemStack stack = ToolTracker.liveStack(entry, minecraft.player);
        return stack.isDamageableItem() ? stack.getDamageValue() : -1;
    }

    public static void onBlockDestroyed(BlockPos pos, int damageBefore) {
        if (!isActive() || !controlledAttackInvocation) {
            return;
        }
        RelayEntry entry = activeEntry();
        Minecraft minecraft = Minecraft.getInstance();
        if (entry == null || entry.status() != RelayEntryStatus.ACTIVE || minecraft.player == null) {
            return;
        }

        PROGRESS.onBlockDestroyed(entry);

        if (damageBefore >= 0) {
            ItemStack after = ToolTracker.liveStack(entry, minecraft.player);
            if (after.isEmpty()) {
                int consumed = Math.max(0, entry.snapshot().getMaxDamage() - damageBefore);
                entry.recordDurabilityConsumption(consumed, entry.snapshot().getMaxDamage());
                entry.setStatus(RelayEntryStatus.BROKEN);
            } else if (after.isDamageableItem()) {
                int damageAfter = after.getDamageValue();
                entry.recordDurabilityConsumption(Math.max(0, damageAfter - damageBefore), damageAfter);
                entry.rememberLiveStack(after);
            }
        }

        RelayDebug.log("Entry {} destroyed block {} (blocks={}, durability={})",
                activeIndex + 1, pos, entry.blocksBroken(), entry.durabilityConsumed());
    }

    private static void advance(RelayEntry previousEntry) {
        releaseControlledAttack();
        if (!prepareNextEntry(previousEntry)) {
            return;
        }
        transitionCooldownTicks = 1;
    }

    private static boolean prepareNextEntry(RelayEntry previousEntry) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            stop(StopReason.DISCONNECT);
            return false;
        }

        while (++activeIndex < CONFIGURED_QUEUE.size()) {
            RelayEntry candidate = CONFIGURED_QUEUE.get(activeIndex);
            if (!ToolTracker.matchesExpectedSlot(candidate, player)) {
                stop(StopReason.TOOL_INVALID);
                return false;
            }

            ItemStack currentStack = ToolTracker.liveStack(candidate, player);
            if (candidate.preserveAtOne()
                    && currentStack.isDamageableItem()
                    && MiningProgressTracker.remainingDurability(currentStack) <= 1) {
                candidate.rememberLiveStack(currentStack);
                candidate.setStatus(RelayEntryStatus.PRESERVED);
                RelayDebug.log("Skipping entry {} because it is already at the preserve threshold", activeIndex + 1);
                continue;
            }

            if (!InventoryRelayManager.equip(CONFIGURED_QUEUE, candidate, previousEntry, player)) {
                stop(StopReason.INVENTORY_DESYNC);
                return false;
            }

            ItemStack equipped = ToolTracker.liveStack(candidate, player);
            if (equipped.isEmpty() || !ToolTracker.matchesExpectedSlot(candidate, player)) {
                stop(StopReason.INVENTORY_DESYNC);
                return false;
            }

            candidate.beginRuntime(equipped);
            transitionCooldownTicks = 1;
            RelayDebug.log("Entry {} is active from inventory slot {}", activeIndex + 1, candidate.currentInventorySlot());
            return true;
        }

        stop(StopReason.QUEUE_COMPLETE);
        return false;
    }

    private static StopReason runtimeQueueIntegrity(LocalPlayer player) {
        if (player.containerMenu != player.inventoryMenu) {
            return StopReason.INVENTORY_DESYNC;
        }

        Set<Integer> occupiedTrackedSlots = new HashSet<>();
        int activeEntries = 0;
        for (RelayEntry tracked : CONFIGURED_QUEUE.entries()) {
            if (tracked.status() != RelayEntryStatus.PENDING && tracked.status() != RelayEntryStatus.ACTIVE) {
                continue;
            }

            int slot = tracked.currentInventorySlot();
            if (slot < 0 || slot >= 36 || !occupiedTrackedSlots.add(slot)) {
                return StopReason.INVENTORY_DESYNC;
            }
            if (!ToolTracker.matchesExpectedSlot(tracked, player)) {
                return StopReason.TOOL_INVALID;
            }
            if (tracked.status() == RelayEntryStatus.ACTIVE) {
                activeEntries++;
            }
        }

        return activeEntries == 1 ? null : StopReason.INTERNAL_SAFETY;
    }

    private static void holdControlledAttack() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.gameMode == null || player == null || minecraft.level == null) {
            return;
        }

        if (player.isUsingItem()) {
            waitingForWorkBlock = false;
            minecraft.gameMode.stopDestroyBlock();
            return;
        }

        BlockHitResult blockHit = currentWorkBlock(minecraft);
        if (blockHit == null) {
            waitingForWorkBlock = true;
            // Temporary air / entities / out-of-reach targets are not a stop condition.
            // Keep the relay session armed and resume automatically when the camera
            // points at a valid block again, matching THE Pick's validated behavior.
            minecraft.gameMode.stopDestroyBlock();
            return;
        }

        waitingForWorkBlock = false;
        BlockPos pos = blockHit.getBlockPos();
        var click = ClientHooks.onClickInput(0, minecraft.options.keyAttack, InteractionHand.MAIN_HAND);
        if (click.isCanceled()) {
            if (click.shouldSwingHand()) {
                minecraft.particleEngine.addBlockHitEffects(pos, blockHit);
                player.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }

        controlledAttackInvocation = true;
        try {
            if (minecraft.gameMode.continueDestroyBlock(pos, blockHit.getDirection()) && click.shouldSwingHand()) {
                minecraft.particleEngine.addBlockHitEffects(pos, blockHit);
                player.swing(InteractionHand.MAIN_HAND);
            }
        } finally {
            controlledAttackInvocation = false;
        }
    }

    private static void releaseControlledAttack() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.keyAttack.setDown(false);
        if (minecraft.gameMode != null) {
            minecraft.gameMode.stopDestroyBlock();
        }
    }

    /**
     * Resolves the current work block according to the selected session mode.
     *
     * LINE_MINING follows the current crosshair every cycle, matching THE Pick.
     * SINGLE_BLOCK pins the coordinate captured at Start and only mines while the
     * crosshair is currently resolving that same coordinate. Looking elsewhere
     * never cancels the session; it simply pauses mining until the pinned block is
     * aimed at again.
     */
    private static BlockHitResult currentWorkBlock(Minecraft minecraft) {
        BlockHitResult blockHit = raycastCurrentBlock(minecraft);
        if (blockHit == null) {
            return null;
        }

        if (isActive()
                && sessionMiningMode == RelayMiningMode.SINGLE_BLOCK
                && singleBlockTarget != null
                && !singleBlockTarget.equals(blockHit.getBlockPos())) {
            return null;
        }

        return blockHit;
    }

    private static BlockHitResult raycastCurrentBlock(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return null;
        }

        HitResult hit = minecraft.player.pick(minecraft.player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return null;
        }

        return minecraft.level.getBlockState(blockHit.getBlockPos()).isAir() ? null : blockHit;
    }

    private static Component stopMessage(StopReason reason) {
        return switch (reason) {
            case QUEUE_COMPLETE -> Component.translatable("message.pickrelay.queue_complete");
            case PLAYER_MOVED -> Component.translatable("message.pickrelay.stopped.player_moved");
            case PHYSICAL_LEFT_CLICK, PHYSICAL_RIGHT_CLICK -> Component.translatable("message.pickrelay.stopped.manual_input");
            case PLAYER_DEATH -> Component.translatable("message.pickrelay.stopped.death");
            case DISCONNECT -> Component.translatable("message.pickrelay.stopped.disconnect");
            case DIMENSION_CHANGE -> Component.translatable("message.pickrelay.stopped.dimension");
            case TOOL_INVALID -> Component.translatable("message.pickrelay.stopped.tool_invalid");
            case INVENTORY_DESYNC -> Component.translatable("message.pickrelay.stopped.inventory");
            case NO_VALID_NEXT_TOOL -> Component.translatable("message.pickrelay.stopped.no_tool");
            case INTERNAL_SAFETY -> Component.translatable("message.pickrelay.stopped.safety");
            case MANUAL -> Component.translatable("message.pickrelay.stopped.manual");
        };
    }
}
