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
    private static int activeIndex = -1;
    private static int transitionCooldownTicks;
    private static boolean controlledAttackInvocation;
    private static RelayMiningMode configuredMiningMode = RelayMiningMode.LINE_MINING;
    private static RelayMiningMode sessionMiningMode;
    private static BlockPos singleBlockTarget;
    private static boolean waitingForWorkBlock;
    private static boolean preserveTransitionRequested;
    private static int preserveNearBreakCooldownTicks;

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

    public static boolean isWaitingForWorkBlock() {
        return waitingForWorkBlock;
    }

    public static RelayEntry activeEntry() {
        if (activeIndex < 0 || activeIndex >= CONFIGURED_QUEUE.size()) {
            return null;
        }
        return CONFIGURED_QUEUE.get(activeIndex);
    }

    /** True while a block-destroy call belongs to the active Pick Relay mining cycle. */
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
        activeIndex = -1;
        transitionCooldownTicks = 0;
        controlledAttackInvocation = false;
        waitingForWorkBlock = false;
        preserveTransitionRequested = false;
        preserveNearBreakCooldownTicks = 0;
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
        releaseControlledAttack();
        controlledAttackInvocation = false;
        SAFETY.clear();
        activeIndex = -1;
        transitionCooldownTicks = 0;
        sessionMiningMode = null;
        singleBlockTarget = null;
        waitingForWorkBlock = false;
        preserveTransitionRequested = false;
        preserveNearBreakCooldownTicks = 0;

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

        if (preserveNearBreakCooldownTicks > 0) {
            releaseControlledAttack();
            preserveNearBreakCooldownTicks--;
            return;
        }

        ItemStack liveStack = ToolTracker.liveStack(entry, player);
        if (liveStack.isEmpty()) {
            if (entry.status() == RelayEntryStatus.BROKEN || shouldTreatMissingActiveToolAsBroken(entry)) {
                if (entry.status() != RelayEntryStatus.BROKEN) {
                    entry.setStatus(RelayEntryStatus.BROKEN);
                    RelayDebug.log("Active entry {} missing from its slot after reaching critical durability; treating it as broken", activeIndex + 1);
                } else {
                    RelayDebug.log("Entry {} was confirmed broken by Pick Relay", activeIndex + 1);
                }
                advance(entry);
                return;
            }

            int relocated = ToolTracker.resolveSlot(entry, player, Set.of());
            if (relocated < 0) {
                entry.setStatus(RelayEntryStatus.SKIPPED);
                RelayDebug.log("Active entry {} is no longer in the player inventory; skipping it", activeIndex + 1);
                advance(entry);
                return;
            }
            RelayDebug.log("Relocated active entry {} to inventory slot {}", activeIndex + 1, relocated);
            liveStack = ToolTracker.liveStack(entry, player);
        }

        ToolTracker.reconcileQueue(CONFIGURED_QUEUE, player);
        liveStack = ToolTracker.liveStack(entry, player);

        if (entry.currentInventorySlot() < 0 || liveStack.isEmpty() || !ToolTracker.matchesExpectedSlot(entry, player)) {
            entry.setStatus(RelayEntryStatus.SKIPPED);
            RelayDebug.log("Active entry {} no longer has a resolvable tool identity; skipping it", activeIndex + 1);
            advance(entry);
            return;
        }

        StopReason integrityReason = runtimeQueueIntegrity(player);
        if (integrityReason != null) {
            stop(integrityReason);
            return;
        }
        if (entry.currentInventorySlot() >= 9) {
            if (!InventoryRelayManager.equip(CONFIGURED_QUEUE, entry, null, player)) {
                entry.setStatus(RelayEntryStatus.SKIPPED);
                RelayDebug.log("Could not re-equip relocated active entry {}; skipping it", activeIndex + 1);
                advance(entry);
                return;
            }
            liveStack = ToolTracker.liveStack(entry, player);
        }

        InventoryRelayManager.enforceSelected(entry, player);
        PROGRESS.observe(entry, liveStack);

        if (finishEntryIfComplete(entry, liveStack)) {
            return;
        }

        preserveTransitionRequested = false;
        holdControlledAttack();

        // Mining can consume the last safe durability, so re-check in the same tick.
        if (!isActive() || activeEntry() != entry || entry.status() != RelayEntryStatus.ACTIVE) {
            return;
        }

        ItemStack afterAttack = ToolTracker.liveStack(entry, player);
        if (!afterAttack.isEmpty() && ToolTracker.matchesExpectedSlot(entry, player)) {
            PROGRESS.observe(entry, afterAttack);
            if (preserveTransitionRequested || finishEntryIfComplete(entry, afterAttack)) {
                if (preserveTransitionRequested && entry.status() == RelayEntryStatus.ACTIVE) {
                    preserveEntry(entry, afterAttack);
                }
                return;
            }

            if (entry.preserveAtOne()
                    && afterAttack.isDamageableItem()
                    && MiningProgressTracker.remainingDurability(afterAttack) <= 3) {
                // Allow the authoritative damage update to settle near the break threshold.
                preserveNearBreakCooldownTicks = Math.max(preserveNearBreakCooldownTicks, 2);
            }
        }
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

                if (entry.preserveAtOne()) {
                    int remaining = MiningProgressTracker.remainingDurability(after);
                    if (remaining <= 1) {
                        preserveTransitionRequested = true;
                    } else if (remaining <= 3) {
                        preserveNearBreakCooldownTicks = Math.max(preserveNearBreakCooldownTicks, 2);
                    }
                }
            }
        }

        RelayDebug.log("Entry {} destroyed block {} (blocks={}, durability={})",
                activeIndex + 1, pos, entry.blocksBroken(), entry.durabilityConsumed());
    }


    private static boolean finishEntryIfComplete(RelayEntry entry, ItemStack liveStack) {
        MiningProgressTracker.Completion completion = PROGRESS.completion(entry, liveStack);
        if (completion == MiningProgressTracker.Completion.PRESERVED) {
            preserveEntry(entry, liveStack);
            return true;
        }
        if (completion == MiningProgressTracker.Completion.TARGET_REACHED) {
            entry.rememberLiveStack(liveStack);
            entry.setStatus(RelayEntryStatus.COMPLETED);
            RelayDebug.log("Entry {} reached its {} target ({}/{})", activeIndex + 1, entry.workMode(), entry.progress(), entry.workTarget());
            advance(entry);
            return true;
        }
        return false;
    }

    private static void preserveEntry(RelayEntry entry, ItemStack liveStack) {
        releaseControlledAttack();
        entry.rememberLiveStack(liveStack);
        entry.setStatus(RelayEntryStatus.PRESERVED);
        preserveTransitionRequested = false;
        preserveNearBreakCooldownTicks = 0;
        RelayDebug.log("Entry {} preserved at {} remaining durability", activeIndex + 1, MiningProgressTracker.remainingDurability(liveStack));
        advance(entry);
    }

    private static boolean shouldTreatMissingActiveToolAsBroken(RelayEntry entry) {
        ItemStack lastKnown = entry.lastKnownSnapshot();
        if (!entry.snapshot().isDamageableItem() || lastKnown.isEmpty() || !lastKnown.isDamageableItem()) {
            return false;
        }

        return MiningProgressTracker.remainingDurability(lastKnown) <= 1
                || entry.lastObservedDamage() >= lastKnown.getMaxDamage() - 1;
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

        ToolTracker.reconcileQueue(CONFIGURED_QUEUE, player);

        while (++activeIndex < CONFIGURED_QUEUE.size()) {
            RelayEntry candidate = CONFIGURED_QUEUE.get(activeIndex);
            if (candidate.currentInventorySlot() < 0 || !ToolTracker.matchesExpectedSlot(candidate, player)) {
                candidate.setStatus(RelayEntryStatus.SKIPPED);
                RelayDebug.log("Skipping queued entry {} because its selected tool is no longer available", activeIndex + 1);
                continue;
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
            preserveTransitionRequested = false;
            preserveNearBreakCooldownTicks = 0;
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
            if (slot < 0) {
                continue;
            }
            if (slot >= 36 || !occupiedTrackedSlots.add(slot)) {
                return StopReason.INVENTORY_DESYNC;
            }
            if (!ToolTracker.matchesExpectedSlot(tracked, player)) {
                if (tracked.status() == RelayEntryStatus.PENDING) {
                    tracked.setCurrentInventorySlot(-1);
                    continue;
                }
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

        RelayEntry active = activeEntry();
        if (active != null && active.preserveAtOne()) {
            ItemStack activeStack = ToolTracker.liveStack(active, player);
            if (!activeStack.isEmpty()
                    && activeStack.isDamageableItem()
                    && MiningProgressTracker.remainingDurability(activeStack) <= 1) {
                preserveTransitionRequested = true;
                minecraft.gameMode.stopDestroyBlock();
                return;
            }
        }

        BlockHitResult blockHit = currentWorkBlock(minecraft);
        if (blockHit == null) {
            waitingForWorkBlock = true;
            // No valid work block pauses mining without ending the session.
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

    /** Resolves the current work block according to the active session mode. */
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
            case PLAYER_DEATH -> Component.translatable("message.pickrelay.stopped.death");
            case DISCONNECT -> Component.translatable("message.pickrelay.stopped.disconnect");
            case DIMENSION_CHANGE -> Component.translatable("message.pickrelay.stopped.dimension");
            case TOOL_INVALID -> Component.translatable("message.pickrelay.stopped.tool_invalid");
            case INVENTORY_DESYNC -> Component.translatable("message.pickrelay.stopped.inventory");
            case INTERNAL_SAFETY -> Component.translatable("message.pickrelay.stopped.safety");
            case MANUAL -> Component.translatable("message.pickrelay.stopped.manual");
        };
    }
}
