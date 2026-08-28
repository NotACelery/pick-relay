package dev.maicra.pickrelay.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.maicra.pickrelay.client.ClientEvents;
import dev.maicra.pickrelay.client.PickRelayController;
import dev.maicra.pickrelay.client.RelayKeyMappings;
import dev.maicra.pickrelay.client.progress.MiningRateEstimator;
import dev.maicra.pickrelay.client.tool.ToolEligibility;
import dev.maicra.pickrelay.client.tool.ToolTracker;
import dev.maicra.pickrelay.session.RelayEntry;
import dev.maicra.pickrelay.session.RelayEntryStatus;
import dev.maicra.pickrelay.session.RelayQueue;
import dev.maicra.pickrelay.session.RelayMiningMode;
import dev.maicra.pickrelay.session.RelayState;
import dev.maicra.pickrelay.session.RelayValidationResult;
import dev.maicra.pickrelay.session.RelayValidator;
import dev.maicra.pickrelay.session.RelayWorkMode;
import dev.maicra.pickrelay.session.StopReason;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PickRelayScreen extends Screen {
    private static final int COLUMNS = 9;
    private static final int QUEUE_ROWS = 4;
    private static final int INVENTORY_ROWS = 4;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_WIDTH = COLUMNS * SLOT_SIZE;
    private static final int QUEUE_HEIGHT = QUEUE_ROWS * SLOT_SIZE;
    private static final int SIDE_BY_SIDE_GRID_GAP = 24;
    private static final int SESSION_PANEL_WIDTH = 108;
    private static final int SESSION_PANEL_HEIGHT = QUEUE_HEIGHT + 10;
    private static final int SESSION_PANEL_GAP = 6;
    private static final int SESSION_PANEL_PADDING = 5;
    private static final int SESSION_ROW_HEIGHT = 10;
    private static final int LAYOUT_BOTTOM_MARGIN = 30;
    private static final int SIDE_BY_SIDE_MIN_WIDTH = GRID_WIDTH * 2 + SIDE_BY_SIDE_GRID_GAP + 12;
    private static final int RESPONSIVE_SESSION_MIN_WIDTH =
            GRID_WIDTH * 2 + SESSION_PANEL_WIDTH + SESSION_PANEL_GAP * 2 + 4;
    private static final int NORMAL_SESSION_MIN_WIDTH = GRID_WIDTH + SESSION_PANEL_WIDTH + SESSION_PANEL_GAP + 8;
    private static final int COLOR_ACTIVE = 0xFFFFFFFF;
    private static final int COLOR_SELECTED = 0xFFFFD75F;
    private static final int COLOR_INVALID = 0xFFFF5555;
    private static final int COLOR_DRAG_TARGET = 0xFF55FFFF;
    private static final int COLOR_HOVER = 0xFFB0B0B0;
    private static final double QUEUE_DRAG_THRESHOLD_SQ = 16.0;

    private final RelayQueue queue = PickRelayController.queue();
    private final Set<Integer> visitedInventorySlotsThisDrag = new HashSet<>();

    private Button actionButton;
    private Button clearQueueButton;
    private Button closeButton;
    private Button singleBlockButton;
    private Button lineMiningButton;
    private Button modeButton;
    private Button preserveButton;
    private EditBox targetEdit;
    private DurabilityBudgetSlider durabilitySlider;
    private UUID selectedEntryId;
    private boolean syncingEditor;

    private InventoryDragMode inventoryDragMode = InventoryDragMode.NONE;
    private int queuePressIndex = -1;
    private double queuePressX;
    private double queuePressY;
    private boolean queueDragging;

    public PickRelayScreen() {
        super(Component.translatable("screen.pickrelay.title"));
    }

    @Override
    protected void init() {
        suppressBackgroundGameplay();
        PickRelayController.enterConfiguration();

        int controlsY = detailsControlsY();
        modeButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleMode())
                .bounds(detailsControlsStartX(), controlsY, 128, 20)
                .build());

        targetEdit = addRenderableWidget(new EditBox(
                font,
                detailsEditorX(),
                controlsY,
                76,
                20,
                Component.translatable("screen.pickrelay.target")
        ));
        targetEdit.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        targetEdit.setMaxLength(8);
        targetEdit.setResponder(this::onTargetChanged);

        durabilitySlider = addRenderableWidget(new DurabilityBudgetSlider(
                detailsEditorX(),
                controlsY,
                76,
                20
        ));

        preserveButton = addRenderableWidget(Button.builder(Component.empty(), button -> togglePreserve())
                .bounds(detailsPreserveX(), controlsY, 132, 20)
                .build());

        singleBlockButton = addRenderableWidget(
                Button.builder(Component.empty(), button -> setMiningMode(RelayMiningMode.SINGLE_BLOCK))
                        .bounds(singleBlockButtonX(), miningModeButtonsY(), miningModeButtonWidth(), 20)
                        .build());

        lineMiningButton = addRenderableWidget(
                Button.builder(Component.empty(), button -> setMiningMode(RelayMiningMode.LINE_MINING))
                        .bounds(lineMiningButtonX(), miningModeButtonsY(), miningModeButtonWidth(), 20)
                        .build());

        actionButton = addRenderableWidget(Button.builder(actionLabel(), button -> onAction())
                .bounds(actionButtonX(), actionButtonY(), actionButtonWidth(), 20)
                .build());

        clearQueueButton = addRenderableWidget(
                Button.builder(Component.translatable("screen.pickrelay.clear_queue"), button -> clearQueue())
                        .bounds(clearQueueButtonX(), clearQueueButtonY(), clearQueueButtonWidth(), 20)
                        .build());

        closeButton = addRenderableWidget(
                Button.builder(Component.translatable("screen.pickrelay.close"), button -> onClose())
                        .bounds(closeButtonX(), closeButtonY(), closeButtonWidth(), 20)
                        .build());

        if (selectedEntryId == null && !queue.isEmpty()) {
            selectedEntryId = queue.get(0).id();
        }
        syncEditorFromSelection();
        updateWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        suppressBackgroundGameplay();
        if (selectedEntryId != null && queue.findById(selectedEntryId).isEmpty()) {
            selectedEntryId = null;
            syncEditorFromSelection();
        }
        updateWidgets();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);

        gui.drawCenteredString(font, title, width / 2, 9, 0xFFFFFF);
        renderQueueHeader(gui);

        renderQueue(gui, mouseX, mouseY);
        renderSessionPanel(gui);
        renderSelectedDetails(gui);
        renderPlayerInventory(gui, mouseX, mouseY);
        renderSessionModeHeader(gui);
        renderValidation(gui);
        renderQueueDragFeedback(gui, mouseX, mouseY);
        renderHoveredTooltip(gui, mouseX, mouseY);
    }

    private void suppressBackgroundGameplay() {
        Minecraft client = minecraft != null ? minecraft : Minecraft.getInstance();

        client.options.keyUse.setDown(false);
        client.options.keyAttack.setDown(false);
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keyShift.setDown(false);
        client.options.keySprint.setDown(false);

        if (client.player != null && client.player.isUsingItem()) {
            if (client.gameMode != null) {
                client.gameMode.releaseUsingItem(client.player);
            } else {
                client.player.releaseUsingItem();
            }
        }

        if (!PickRelayController.isActive() && client.gameMode != null) {
            client.gameMode.stopDestroyBlock();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (RelayKeyMappings.OPEN_RELAY.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            closeFromRelayBinding();
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (RelayKeyMappings.OPEN_RELAY.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(button))) {
            closeFromRelayBinding();
            return true;
        }

        if (PickRelayController.canEditQueue()) {
            int inventorySlot = inventorySlotAt(mouseX, mouseY);
            if (button == 0 && inventorySlot >= 0 && beginInventoryDrag(inventorySlot)) {
                return true;
            }

            int queueCell = queueCellAt(mouseX, mouseY);
            if (queueCell >= 0 && queueCell < queue.size()) {
                if (button == 1) {
                    RelayEntry removed = queue.removeAt(queueCell);
                    if (removed != null && removed.id().equals(selectedEntryId)) {
                        selectedEntryId = queue.isEmpty()
                                ? null
                                : queue.get(Math.min(queueCell, queue.size() - 1)).id();
                    }
                    resetQueueDrag();
                    syncEditorFromSelection();
                    return true;
                }

                if (button == 0) {
                    selectEntry(queue.get(queueCell));
                    queuePressIndex = queueCell;
                    queuePressX = mouseX;
                    queuePressY = mouseY;
                    queueDragging = false;
                    return true;
                }
            }
        } else {
            int queueCell = queueCellAt(mouseX, mouseY);
            if (button == 0 && queueCell >= 0 && queueCell < queue.size()) {
                selectEntry(queue.get(queueCell));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && inventoryDragMode != InventoryDragMode.NONE) {
            int inventorySlot = inventorySlotAt(mouseX, mouseY);
            if (inventorySlot >= 0) {
                applyInventoryDragToSlot(inventorySlot);
            }
            return true;
        }

        if (button == 0 && queuePressIndex >= 0 && PickRelayController.canEditQueue()) {
            double dx = mouseX - queuePressX;
            double dy = mouseY - queuePressY;
            if (!queueDragging && dx * dx + dy * dy >= QUEUE_DRAG_THRESHOLD_SQ) {
                queueDragging = true;
            }
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && inventoryDragMode != InventoryDragMode.NONE) {
            resetInventoryDrag();
            return true;
        }

        if (button == 0 && queuePressIndex >= 0) {
            if (queueDragging && PickRelayController.canEditQueue()) {
                finishQueueDrag(mouseX, mouseY);
            }
            resetQueueDrag();
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void selectEntry(RelayEntry entry) {
        selectedEntryId = entry.id();
        syncEditorFromSelection();
    }

    private boolean beginInventoryDrag(int inventorySlot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        ItemStack stack = minecraft.player.getInventory().getItem(inventorySlot);
        if (!ToolEligibility.isSupported(stack)) {
            return false;
        }

        inventoryDragMode = queue.containsInventorySlot(inventorySlot)
                ? InventoryDragMode.REMOVE
                : InventoryDragMode.ADD;
        visitedInventorySlotsThisDrag.clear();
        applyInventoryDragToSlot(inventorySlot);
        return true;
    }

    private void applyInventoryDragToSlot(int inventorySlot) {
        if (!visitedInventorySlotsThisDrag.add(inventorySlot)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ItemStack stack = minecraft.player.getInventory().getItem(inventorySlot);
        if (!ToolEligibility.isSupported(stack)) {
            return;
        }

        if (inventoryDragMode == InventoryDragMode.ADD) {
            if (!queue.containsInventorySlot(inventorySlot) && queue.add(inventorySlot, stack)) {
                selectedEntryId = queue.get(queue.size() - 1).id();
                syncEditorFromSelection();
            }
        } else if (inventoryDragMode == InventoryDragMode.REMOVE) {
            queue.findById(selectedEntryId).ifPresent(selected -> {
                if (selected.currentInventorySlot() == inventorySlot) {
                    selectedEntryId = null;
                }
            });
            queue.removeByInventorySlot(inventorySlot);
            if (selectedEntryId == null && !queue.isEmpty()) {
                selectedEntryId = queue.get(0).id();
            }
            syncEditorFromSelection();
        }
    }

    private void finishQueueDrag(double mouseX, double mouseY) {
        if (queuePressIndex < 0 || queuePressIndex >= queue.size()) {
            return;
        }

        int targetCell = queueCellAt(mouseX, mouseY);
        if (targetCell < 0) {
            RelayEntry removed = queue.removeAt(queuePressIndex);
            if (removed != null && removed.id().equals(selectedEntryId)) {
                selectedEntryId = null;
            }
            syncEditorFromSelection();
            return;
        }

        if (targetCell >= queue.size()) {
            queue.moveToInsertionPoint(queuePressIndex, queue.size());
            return;
        }

        QueueDropZone zone = queueDropZone(mouseX, targetCell);
        switch (zone) {
            case SWAP -> queue.swap(queuePressIndex, targetCell);
            case INSERT_BEFORE -> queue.moveToInsertionPoint(queuePressIndex, targetCell);
            case INSERT_AFTER -> queue.moveToInsertionPoint(queuePressIndex, targetCell + 1);
        }
    }

    private void cycleMode() {
        selectedEntry().ifPresent(entry -> {
            if (!PickRelayController.canEditQueue()) {
                return;
            }
            entry.setWorkMode(entry.workMode().next());
            if (entry.workMode() == RelayWorkMode.BLOCKS && entry.workTarget() <= 0) {
                entry.setWorkTarget(100);
            } else if (entry.workMode() == RelayWorkMode.DURABILITY && entry.workTarget() <= 0) {
                entry.setWorkTarget(Math.max(1, Math.min(100, maxUsableDurability(entry))));
            }
            syncEditorFromSelection();
        });
    }

    private void togglePreserve() {
        selectedEntry().ifPresent(entry -> {
            if (!PickRelayController.canEditQueue() || !entry.snapshot().isDamageableItem()) {
                return;
            }
            entry.setPreserveAtOne(!entry.preserveAtOne());
            if (entry.workMode() == RelayWorkMode.DURABILITY && entry.workTarget() > maxUsableDurability(entry)) {
                entry.setWorkTarget(Math.max(0, maxUsableDurability(entry)));
            }
            syncEditorFromSelection();
        });
    }

    private void onTargetChanged(String value) {
        if (syncingEditor || !PickRelayController.canEditQueue()) {
            return;
        }
        selectedEntry().ifPresent(entry -> {
            try {
                entry.setWorkTarget(value.isEmpty() ? 0 : Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                entry.setWorkTarget(Integer.MAX_VALUE);
            }
        });
    }

    private int maxUsableDurability(RelayEntry entry) {
        ItemStack stack = entry.snapshot();
        if (!stack.isDamageableItem()) {
            return 0;
        }
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        return Math.max(0, remaining - (entry.preserveAtOne() ? 1 : 0));
    }

    private void syncEditorFromSelection() {
        if (modeButton == null || preserveButton == null || targetEdit == null || durabilitySlider == null) {
            return;
        }

        syncingEditor = true;
        RelayEntry entry = selectedEntry().orElse(null);
        if (entry == null) {
            modeButton.setMessage(Component.translatable("screen.pickrelay.mode.none"));
            preserveButton.setMessage(Component.translatable("screen.pickrelay.preserve.off"));
            targetEdit.setValue("");
            durabilitySlider.syncFromEntry(null);
        } else {
            modeButton.setMessage(Component.translatable(workModeTranslation(entry)));
            preserveButton.setMessage(Component.translatable(
                    entry.preserveAtOne() ? "screen.pickrelay.preserve.on" : "screen.pickrelay.preserve.off"
            ));
            targetEdit.setValue(entry.workMode() == RelayWorkMode.BLOCKS ? Integer.toString(entry.workTarget()) : "");
            durabilitySlider.syncFromEntry(entry);
        }
        syncingEditor = false;
        updateWidgets();
    }

    private void updateWidgets() {
        if (actionButton == null
                || clearQueueButton == null
                || closeButton == null
                || singleBlockButton == null
                || lineMiningButton == null
                || modeButton == null
                || preserveButton == null
                || targetEdit == null
                || durabilitySlider == null) {
            return;
        }

        RelayEntry entry = selectedEntry().orElse(null);
        boolean editable = PickRelayController.canEditQueue();
        boolean hasEntry = entry != null;

        actionButton.setMessage(actionLabel());
        actionButton.active = PickRelayController.isActive() || PickRelayController.canStart();
        clearQueueButton.active = editable && !queue.isEmpty();

        singleBlockButton.setMessage(miningModeLabel(RelayMiningMode.SINGLE_BLOCK));
        lineMiningButton.setMessage(miningModeLabel(RelayMiningMode.LINE_MINING));
        singleBlockButton.active = editable;
        lineMiningButton.active = editable;

        modeButton.active = editable && hasEntry;
        preserveButton.active = editable && hasEntry && entry.snapshot().isDamageableItem();
        boolean blocksVisible = hasEntry && entry.workMode() == RelayWorkMode.BLOCKS;
        boolean durabilityVisible = hasEntry && entry.workMode() == RelayWorkMode.DURABILITY;

        targetEdit.visible = blocksVisible;
        targetEdit.active = editable && blocksVisible;
        targetEdit.setEditable(editable && blocksVisible);

        durabilitySlider.visible = durabilityVisible;
        durabilitySlider.active = editable && durabilityVisible;
    }

    private void setMiningMode(RelayMiningMode mode) {
        if (PickRelayController.canEditQueue()) {
            PickRelayController.setMiningMode(mode);
            updateWidgets();
        }
    }

    private Component miningModeLabel(RelayMiningMode mode) {
        boolean selected = PickRelayController.miningMode() == mode;
        String key;
        if (compactVerticalLayout()) {
            key = mode == RelayMiningMode.SINGLE_BLOCK
                    ? "screen.pickrelay.mining_mode.single_short"
                    : "screen.pickrelay.mining_mode.line_short";
        } else {
            key = mode == RelayMiningMode.SINGLE_BLOCK
                    ? "screen.pickrelay.mining_mode.single"
                    : "screen.pickrelay.mining_mode.line";
        }
        Component label = Component.translatable(key);
        return selected ? Component.literal("> ").append(label).append(" <") : label;
    }

    private void closeFromRelayBinding() {
        ClientEvents.suppressRelayToggleAfterScreenClose();
        onClose();
    }

    private void clearQueue() {
        if (!PickRelayController.canEditQueue()) {
            return;
        }
        queue.clear();
        selectedEntryId = null;
        resetInventoryDrag();
        resetQueueDrag();
        syncEditorFromSelection();
    }

    private void onAction() {
        if (PickRelayController.isActive()) {
            PickRelayController.stop(StopReason.MANUAL);
        } else {
            PickRelayController.start();
        }
        syncEditorFromSelection();
        updateWidgets();
    }

    private void renderQueue(GuiGraphics gui, int mouseX, int mouseY) {
        int startX = queueGridStartX();
        int startY = queueStartY();
        Set<UUID> invalid = PickRelayController.canEditQueue()
                ? PickRelayController.validation().invalidEntries()
                : Set.of();

        for (int cell = 0; cell < RelayQueue.MAX_ENTRIES; cell++) {
            int x = startX + (cell % COLUMNS) * SLOT_SIZE;
            int y = startY + (cell / COLUMNS) * SLOT_SIZE;
            drawSlotBackground(gui, x, y);

            if (cell >= queue.size()) {
                gui.drawString(font, String.format("%02d", cell + 1), x + 3, y + 5, 0x505050, false);
                continue;
            }

            RelayEntry entry = queue.get(cell);
            boolean selected = entry.id().equals(selectedEntryId);
            boolean hovered = contains(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);

            if (entry.status() == RelayEntryStatus.COMPLETED
                    || entry.status() == RelayEntryStatus.BROKEN
                    || entry.status() == RelayEntryStatus.PRESERVED) {
                gui.fill(x + 1, y + 1, x + 17, y + 17, 0x66000000);
            }

            ItemStack stack = displayStack(entry);
            gui.renderItem(stack, x + 1, y + 1);
            gui.renderItemDecorations(font, stack, x + 1, y + 1);

            if (invalid.contains(entry.id())) {
                drawOutline(gui, x, y, COLOR_INVALID);
            } else if (entry.status() == RelayEntryStatus.ACTIVE) {
                drawOutline(gui, x, y, COLOR_ACTIVE);
            } else if (hovered && !queueDragging) {
                drawOutline(gui, x, y, COLOR_HOVER);
            }

            if (selected) {
                drawInnerOutline(gui, x, y, COLOR_SELECTED);
            }
            if (entry.status() == RelayEntryStatus.ACTIVE) {
                gui.drawString(font, ">", x + 11, y + 1, COLOR_ACTIVE, true);
            }

            gui.drawString(font, String.format("%02d", cell + 1), x + 1, y + 1, 0xFFE0E0E0, true);
        }
    }

    private void renderSelectedDetails(GuiGraphics gui) {
        RelayEntry entry = selectedEntry().orElse(null);
        gui.drawCenteredString(
                font,
                Component.translatable("screen.pickrelay.selected_tool"),
                width / 2,
                detailsHeaderY(),
                0xC0C0C0);
        if (entry == null) {
            gui.drawCenteredString(
                    font,
                    Component.translatable("screen.pickrelay.details.none"),
                    width / 2,
                    detailsSummaryY(),
                    0xD0D0D0);
            return;
        }

        ItemStack display = displayStack(entry);
        int position = queue.indexOf(entry.id()) + 1;
        String durability;
        if (display.isDamageableItem()) {
            int remaining = display.getMaxDamage() - display.getDamageValue();
            double percent = display.getMaxDamage() <= 0 ? 0.0D : remaining * 100.0D / display.getMaxDamage();
            durability = remaining + "/" + display.getMaxDamage() + " (" + String.format("%.1f", percent) + "%)";
        } else {
            durability = "—";
        }

        gui.renderItem(display, detailsIconX(), detailsIconY());
        gui.renderItemDecorations(font, display, detailsIconX(), detailsIconY());

        gui.drawCenteredString(
                font,
                Component.translatable(
                        "screen.pickrelay.details.summary",
                        position,
                        queue.size(),
                        display.getHoverName(),
                        durability),
                width / 2,
                detailsSummaryY(),
                0xD0D0D0
        );

        Component progress = progressText(entry);
        gui.drawCenteredString(font, progress, width / 2, detailsProgressY(), 0xA8A8A8);

        Component rate = selectedMiningRateText(display);
        gui.drawCenteredString(font, rate, width / 2, detailsRateY(), 0xFF80E0FF);
    }

    private Component selectedMiningRateText(ItemStack tool) {
        MiningRateEstimator.Estimate estimate = MiningRateEstimator.preview(tool);
        if (estimate.target() == null) {
            return Component.translatable("screen.pickrelay.estimate.aim");
        }

        Component blockName = estimate.target().state().getBlock().getName();
        if (estimate.unbreakable()) {
            return Component.translatable("screen.pickrelay.estimate.unbreakable", blockName);
        }
        if (!estimate.available()) {
            return Component.translatable("screen.pickrelay.estimate.unavailable", blockName);
        }

        return Component.translatable(
                "screen.pickrelay.estimate.rate",
                blockName,
                formatRate(estimate.blocksPerSecond()),
                formatSecondsPerBlock(estimate.secondsPerBlock())
        );
    }

    private Component progressText(RelayEntry entry) {
        if (entry.status() == RelayEntryStatus.ACTIVE) {
            Component base = switch (entry.workMode()) {
                case UNTIL_BROKEN -> Component.translatable("screen.pickrelay.progress.until_broken");
                case DURABILITY -> Component.translatable(
                        "screen.pickrelay.progress.durability",
                        entry.durabilityConsumed(),
                        entry.workTarget());
                case BLOCKS -> Component.translatable(
                        "screen.pickrelay.progress.blocks",
                        entry.blocksBroken(),
                        entry.workTarget());
            };
            if (PickRelayController.isWaitingForWorkBlock()) {
                String key = PickRelayController.miningMode() == RelayMiningMode.SINGLE_BLOCK
                        ? "screen.pickrelay.progress.waiting_single"
                        : "screen.pickrelay.progress.waiting_line";
                return Component.translatable(key, base);
            }
            return base;
        }

        return switch (entry.workMode()) {
            case UNTIL_BROKEN -> Component.translatable("screen.pickrelay.mode.until_broken");
            case DURABILITY -> Component.translatable("screen.pickrelay.config.durability", entry.workTarget());
            case BLOCKS -> Component.translatable("screen.pickrelay.config.blocks", entry.workTarget());
        };
    }

    private ItemStack displayStack(RelayEntry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && entry.status() == RelayEntryStatus.ACTIVE) {
            ItemStack live = ToolTracker.liveStack(entry, minecraft.player);
            if (!live.isEmpty()) {
                return live;
            }
        }
        return entry.lastKnownSnapshot();
    }

    private void renderPlayerInventory(GuiGraphics gui, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        int startX = inventoryGridStartX();
        int startY = inventoryStartY();

        gui.drawCenteredString(
                font,
                Component.translatable("screen.pickrelay.inventory"),
                inventoryHeaderCenterX(),
                inventoryLabelY(),
                0xC0C0C0);

        if (minecraft.player == null) {
            return;
        }

        Inventory inventory = minecraft.player.getInventory();
        for (int displayIndex = 0; displayIndex < 36; displayIndex++) {
            int inventorySlot = inventorySlotForDisplayIndex(displayIndex);
            int x = startX + (displayIndex % COLUMNS) * SLOT_SIZE;
            int y = startY + (displayIndex / COLUMNS) * SLOT_SIZE;
            drawSlotBackground(gui, x, y);

            ItemStack stack = inventory.getItem(inventorySlot);
            if (stack.isEmpty()) {
                continue;
            }

            boolean supported = ToolEligibility.isSupported(stack);
            boolean queued = isInventorySlotQueued(inventorySlot);
            boolean hovered = contains(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);

            gui.renderItem(stack, x + 1, y + 1);
            gui.renderItemDecorations(font, stack, x + 1, y + 1);

            if (queued) {
                drawOutline(gui, x, y, 0xFFFFFFFF);
            } else if (hovered && supported && PickRelayController.canEditQueue()) {
                drawOutline(gui, x, y, 0xFFB0B0B0);
            } else if (!supported) {
                gui.fill(x + 1, y + 1, x + 17, y + 17, 0x55000000);
            }
        }
    }

    private void renderSessionPanel(GuiGraphics gui) {
        if (!sessionPanelVisible()) {
            return;
        }

        int x = sessionPanelX();
        int y = queueStartY();
        drawPanelBackground(gui, x, y, SESSION_PANEL_WIDTH, SESSION_PANEL_HEIGHT);
        gui.drawCenteredString(
                font,
                Component.translatable("screen.pickrelay.session_panel"),
                x + SESSION_PANEL_WIDTH / 2,
                21,
                0xC0C0C0
        );

        List<SessionLine> lines = sessionPanelLines();
        int maxTextWidth = SESSION_PANEL_WIDTH - SESSION_PANEL_PADDING * 2;
        int lineY = y + 3;
        for (SessionLine line : lines) {
            String text = fitPanelText(line.text().getString(), maxTextWidth);
            gui.drawString(font, text, x + SESSION_PANEL_PADDING, lineY, line.color(), false);
            lineY += SESSION_ROW_HEIGHT;
        }
    }

    private List<SessionLine> sessionPanelLines() {
        List<SessionLine> lines = new ArrayList<>();
        String toolPosition = PickRelayController.isActive() && PickRelayController.activeIndex() >= 0
                ? (PickRelayController.activeIndex() + 1) + "/" + queue.size()
                : "—";

        lines.add(new SessionLine(
                Component.translatable(
                        "screen.pickrelay.session.time",
                        formatSessionTime(PickRelayController.sessionElapsedTicks())),
                0xFFD0D0D0
        ));
        lines.add(new SessionLine(
                Component.translatable("screen.pickrelay.session.blocks", PickRelayController.sessionBlocksBroken()),
                0xFFD0D0D0
        ));

        if (PickRelayController.isActive() && !PickRelayController.isWaitingForWorkBlock()) {
            MiningRateEstimator.Estimate estimate = MiningRateEstimator.live();
            if (estimate.available() && estimate.target() != null) {
                lines.add(new SessionLine(
                        Component.translatable(
                                "screen.pickrelay.session.bps",
                                estimate.target().state().getBlock().getName(),
                                formatRate(estimate.blocksPerSecond())
                        ),
                        0xFF80E0FF
                ));
            } else if (estimate.unbreakable() && estimate.target() != null) {
                lines.add(new SessionLine(
                        Component.translatable(
                                "screen.pickrelay.session.bps_unbreakable",
                                estimate.target().state().getBlock().getName()),
                        0xFFFF8888
                ));
            } else {
                lines.add(new SessionLine(Component.translatable("screen.pickrelay.session.bps_none"), 0xFF777777));
            }
        } else {
            lines.add(new SessionLine(Component.translatable("screen.pickrelay.session.bps_none"), 0xFF777777));
        }

        lines.add(new SessionLine(
                Component.translatable("screen.pickrelay.session.tool", toolPosition),
                PickRelayController.isActive() ? 0xFFFFFFFF : 0xFF909090
        ));

        Minecraft minecraft = Minecraft.getInstance();
        List<SessionLine> dynamic = new ArrayList<>();
        if (minecraft.player != null) {
            List<MobEffectInstance> effects = new ArrayList<>(minecraft.player.getActiveEffects());
            effects.sort(Comparator
                    .comparingInt(this::effectPriority)
                    .thenComparing(effect -> effect.getEffect().value().getDisplayName().getString()));

            for (MobEffectInstance effect : effects) {
                if (isMiningRelatedEffect(effect)) {
                    dynamic.add(effectLine(effect));
                }
            }
            dynamic.addAll(miningAttributeLines());
            for (MobEffectInstance effect : effects) {
                if (!isMiningRelatedEffect(effect)) {
                    dynamic.add(effectLine(effect));
                }
            }
        }

        if (dynamic.isEmpty()) {
            lines.add(new SessionLine(Component.translatable("screen.pickrelay.session.no_effects"), 0xFF777777));
            return lines;
        }

        int remainingRows = 4;
        if (dynamic.size() <= remainingRows) {
            lines.addAll(dynamic);
        } else {
            lines.addAll(dynamic.subList(0, remainingRows - 1));
            lines.add(new SessionLine(
                    Component.translatable("screen.pickrelay.session.more", dynamic.size() - (remainingRows - 1)),
                    0xFF888888
            ));
        }
        return lines;
    }

    private SessionLine effectLine(MobEffectInstance effect) {
        Component name = effect.getEffect().value().getDisplayName();
        String level = romanNumeral(effect.getAmplifier() + 1);
        String duration = effect.isInfiniteDuration() ? "∞" : formatEffectTime(effect.getDuration());
        Component text = name.copy()
                .append(" ")
                .append(level)
                .append("  ")
                .append(duration);

        int color;
        if (effect.is(MobEffects.DIG_SLOWDOWN)) {
            color = 0xFFFF8888;
        } else if (effect.is(MobEffects.DIG_SPEED) || effect.is(MobEffects.CONDUIT_POWER)) {
            color = 0xFF99FF99;
        } else {
            color = 0xFFB8B8B8;
        }
        return new SessionLine(text, color);
    }

    private boolean isMiningRelatedEffect(MobEffectInstance effect) {
        return effect.is(MobEffects.DIG_SPEED)
                || effect.is(MobEffects.DIG_SLOWDOWN)
                || effect.is(MobEffects.CONDUIT_POWER);
    }

    private int effectPriority(MobEffectInstance effect) {
        if (effect.is(MobEffects.DIG_SPEED) || effect.is(MobEffects.CONDUIT_POWER)) {
            return 0;
        }
        if (effect.is(MobEffects.DIG_SLOWDOWN)) {
            return 1;
        }
        return 2;
    }

    private List<SessionLine> miningAttributeLines() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return List.of();
        }

        List<SessionLine> result = new ArrayList<>();
        Set<String> seenModifiers = new HashSet<>();
        appendMiningModifiers(result, minecraft.player.getAttribute(Attributes.BLOCK_BREAK_SPEED), seenModifiers);
        appendMiningModifiers(result, minecraft.player.getAttribute(Attributes.MINING_EFFICIENCY), seenModifiers);
        appendMiningModifiers(result, minecraft.player.getAttribute(Attributes.SUBMERGED_MINING_SPEED), seenModifiers);
        return result;
    }

    private void appendMiningModifiers(
            List<SessionLine> lines,
            AttributeInstance attribute,
            Set<String> seenModifiers) {
        if (attribute == null) {
            return;
        }

        List<AttributeModifier> modifiers = new ArrayList<>(attribute.getModifiers());
        modifiers.sort(Comparator.comparing(modifier -> modifier.id().toString()));
        for (AttributeModifier modifier : modifiers) {
            if (modifier.amount() <= 0.0D || !seenModifiers.add(modifier.id().toString())) {
                continue;
            }
            String source = humanizeModifierSource(modifier.id().getPath());
            String amount = formatModifierAmount(modifier);
            lines.add(new SessionLine(Component.literal("↑ " + source + " " + amount), 0xFF80E0FF));
        }
    }

    private String humanizeModifierSource(String path) {
        int slash = path.indexOf('/');
        String source = slash >= 0 ? path.substring(0, slash) : path;
        StringBuilder result = new StringBuilder();
        for (String word : source.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.isEmpty() ? source : result.toString();
    }

    private String formatModifierAmount(AttributeModifier modifier) {
        double amount = modifier.amount();
        return switch (modifier.operation()) {
            case ADD_VALUE -> "+" + trimNumber(amount);
            case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> "+" + trimNumber(amount * 100.0D) + "%";
        };
    }

    private String formatRate(double value) {
        if (value >= 10.0D) {
            return String.format(java.util.Locale.ROOT, "%.1f", value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private String formatSecondsPerBlock(double value) {
        if (value < 1.0D) {
            return String.format(java.util.Locale.ROOT, "%.2f", value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private String trimNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private String formatSessionTime(long ticks) {
        long seconds = Math.max(0L, ticks) / 20L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainingSeconds = seconds % 60L;
        return hours > 0
                ? String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainingSeconds)
                : String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, remainingSeconds);
    }

    private String formatEffectTime(int ticks) {
        long seconds = Math.max(0, ticks) / 20L;
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        return minutes >= 60L
                ? String.format(java.util.Locale.ROOT, "%d:%02d:%02d", minutes / 60L, minutes % 60L, remainingSeconds)
                : String.format(java.util.Locale.ROOT, "%d:%02d", minutes, remainingSeconds);
    }

    private String romanNumeral(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(value);
        };
    }

    private String fitPanelText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int available = Math.max(0, maxWidth - font.width(ellipsis));
        return font.plainSubstrByWidth(text, available) + ellipsis;
    }

    private boolean isInventorySlotQueued(int inventorySlot) {
        if (PickRelayController.isActive()) {
            return queue.entries().stream().anyMatch(entry ->
                    (entry.status() == RelayEntryStatus.PENDING || entry.status() == RelayEntryStatus.ACTIVE)
                            && entry.currentInventorySlot() == inventorySlot
            );
        }
        return queue.containsInventorySlot(inventorySlot);
    }

    private void renderSessionModeHeader(GuiGraphics gui) {
        gui.drawCenteredString(
                font,
                Component.translatable("screen.pickrelay.session_mode"),
                width / 2,
                sessionModeHeaderY(),
                0xC0C0C0);
    }

    private void renderQueueHeader(GuiGraphics gui) {
        int centerX = queueHeaderCenterX();
        if (compactVerticalLayout() && PickRelayController.canEditQueue() && !queue.isEmpty()) {
            RelayValidationResult validation = PickRelayController.validation();
            if (!validation.valid() && !validation.message().getString().isEmpty()) {
                gui.drawCenteredString(font, validation.message(), centerX, 21, 0xFFFF7777);
                return;
            }
        }
        gui.drawCenteredString(font, queueHeader(), centerX, 21, 0xC0C0C0);
    }

    private void renderValidation(GuiGraphics gui) {
        if (compactVerticalLayout() || !PickRelayController.canEditQueue() || queue.isEmpty()) {
            return;
        }
        RelayValidationResult validation = PickRelayController.validation();
        if (!validation.valid() && !validation.message().getString().isEmpty()) {
            gui.drawCenteredString(font, validation.message(), width / 2, actionButtonY() + 24, 0xFFFF7777);
        }
    }

    private void renderQueueDragFeedback(GuiGraphics gui, int mouseX, int mouseY) {
        if (!queueDragging || queuePressIndex < 0 || queuePressIndex >= queue.size()) {
            return;
        }

        ItemStack dragged = queue.get(queuePressIndex).snapshot();
        gui.renderItem(dragged, mouseX - 8, mouseY - 8);

        int targetCell = queueCellAt(mouseX, mouseY);
        if (targetCell < 0) {
            return;
        }

        if (targetCell >= queue.size()) {
            if (!queue.isEmpty()) {
                drawInsertionMarker(gui, queue.size() - 1, true);
            }
            return;
        }

        QueueDropZone zone = queueDropZone(mouseX, targetCell);
        if (zone == QueueDropZone.SWAP) {
            int x = queueGridStartX() + (targetCell % COLUMNS) * SLOT_SIZE;
            int y = queueStartY() + (targetCell / COLUMNS) * SLOT_SIZE;
            drawOutline(gui, x, y, COLOR_DRAG_TARGET);
            drawInnerOutline(gui, x, y, COLOR_DRAG_TARGET);
        } else {
            drawInsertionMarker(gui, targetCell, zone == QueueDropZone.INSERT_AFTER);
        }
    }

    private void drawInsertionMarker(GuiGraphics gui, int targetCell, boolean after) {
        int x = queueGridStartX() + (targetCell % COLUMNS) * SLOT_SIZE;
        int y = queueStartY() + (targetCell / COLUMNS) * SLOT_SIZE;
        int markerX = after ? x + SLOT_SIZE - 1 : x;
        gui.fill(markerX, y, markerX + 2, y + SLOT_SIZE, COLOR_DRAG_TARGET);
    }

    private void renderHoveredTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (queueDragging || inventoryDragMode != InventoryDragMode.NONE) {
            return;
        }

        RelayEntry selected = selectedEntry().orElse(null);
        if (selected != null && contains(mouseX, mouseY, detailsIconX(), detailsIconY(), 16, 16)) {
            gui.renderTooltip(font, displayStack(selected), mouseX, mouseY);
            return;
        }

        int queueCell = queueCellAt(mouseX, mouseY);
        if (queueCell >= 0 && queueCell < queue.size()) {
            RelayEntry entry = queue.get(queueCell);
            ItemStack stack = displayStack(entry);
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
            lines.add(Component.empty());
            lines.add(Component.translatable("tooltip.pickrelay.header").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.pickrelay.position", queueCell + 1, queue.size())
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable(
                            "tooltip.pickrelay.inventory_slot",
                            inventoryLocation(entry.currentInventorySlot()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (stack.isDamageableItem()) {
                int remaining = stack.getMaxDamage() - stack.getDamageValue();
                double percent = stack.getMaxDamage() <= 0 ? 0.0D : remaining * 100.0D / stack.getMaxDamage();
                lines.add(Component.translatable(
                        "tooltip.pickrelay.durability",
                        remaining,
                        stack.getMaxDamage(),
                        String.format("%.1f", percent)
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
            lines.add(Component.translatable(
                            "tooltip.pickrelay.mode",
                            Component.translatable(workModeTranslation(entry)))
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (entry.workMode() != RelayWorkMode.UNTIL_BROKEN) {
                lines.add(Component.translatable("tooltip.pickrelay.target", entry.workTarget())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            if (entry.preserveAtOne()) {
                lines.add(Component.translatable("tooltip.pickrelay.preserve").withStyle(ChatFormatting.DARK_GRAY));
            }
            lines.add(Component.translatable(
                            "tooltip.pickrelay.status",
                            Component.translatable(statusTranslation(entry.status())))
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (entry.status() == RelayEntryStatus.ACTIVE) {
                lines.add(progressText(entry).copy().withStyle(ChatFormatting.WHITE));
            } else if (PickRelayController.canEditQueue() && minecraft.player != null) {
                int trackedSlot = entry.currentInventorySlot();
                if (trackedSlot < 0 || trackedSlot >= 36) {
                    lines.add(Component.translatable("tooltip.pickrelay.missing_will_skip")
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    ItemStack live = minecraft.player.getInventory().getItem(trackedSlot);
                    Component problem = RelayValidator.validateEntry(entry, live);
                    if (problem != null) {
                        lines.add(Component.translatable("tooltip.pickrelay.problem", problem)
                                .withStyle(ChatFormatting.RED));
                    }
                }
            }
            gui.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }

        int inventorySlot = inventorySlotAt(mouseX, mouseY);
        if (inventorySlot >= 0 && minecraft.player != null) {
            ItemStack stack = minecraft.player.getInventory().getItem(inventorySlot);
            if (!stack.isEmpty()) {
                gui.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
    }

    private Component inventoryLocation(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return Component.translatable("tooltip.pickrelay.location.hotbar", inventorySlot + 1);
        }
        if (inventorySlot >= 9 && inventorySlot < 36) {
            return Component.translatable("tooltip.pickrelay.location.inventory", inventorySlot - 8);
        }
        return Component.translatable("tooltip.pickrelay.location.unknown");
    }

    private Component queueHeader() {
        return Component.translatable("screen.pickrelay.queue_count", queue.size(), RelayQueue.MAX_ENTRIES);
    }

    private Component actionLabel() {
        if (compactVerticalLayout()) {
            return PickRelayController.isActive()
                    ? Component.translatable("screen.pickrelay.stop_short")
                    : Component.translatable("screen.pickrelay.start_short");
        }
        return PickRelayController.isActive()
                ? Component.translatable("screen.pickrelay.stop")
                : Component.translatable("screen.pickrelay.start");
    }

    private String workModeTranslation(RelayEntry entry) {
        return switch (entry.workMode()) {
            case UNTIL_BROKEN -> "screen.pickrelay.mode.until_broken";
            case DURABILITY -> "screen.pickrelay.mode.durability";
            case BLOCKS -> "screen.pickrelay.mode.blocks";
        };
    }

    private String statusTranslation(RelayEntryStatus status) {
        return switch (status) {
            case PENDING -> "tooltip.pickrelay.status.pending";
            case ACTIVE -> "tooltip.pickrelay.status.active";
            case COMPLETED -> "tooltip.pickrelay.status.completed";
            case BROKEN -> "tooltip.pickrelay.status.broken";
            case PRESERVED -> "tooltip.pickrelay.status.preserved";
            case SKIPPED -> "tooltip.pickrelay.status.skipped";
        };
    }

    private Optional<RelayEntry> selectedEntry() {
        return queue.findById(selectedEntryId);
    }

    private int queueCellAt(double mouseX, double mouseY) {
        return gridCellAt(mouseX, mouseY, queueGridStartX(), queueStartY(), QUEUE_ROWS);
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        int displayIndex = gridCellAt(mouseX, mouseY, inventoryGridStartX(), inventoryStartY(), INVENTORY_ROWS);
        return displayIndex < 0 ? -1 : inventorySlotForDisplayIndex(displayIndex);
    }

    private int gridCellAt(double mouseX, double mouseY, int startX, int startY, int rows) {
        if (mouseX < startX
                || mouseX >= startX + GRID_WIDTH
                || mouseY < startY
                || mouseY >= startY + rows * SLOT_SIZE) {
            return -1;
        }

        int column = (int) ((mouseX - startX) / SLOT_SIZE);
        int row = (int) ((mouseY - startY) / SLOT_SIZE);
        return row * COLUMNS + column;
    }

    private QueueDropZone queueDropZone(double mouseX, int targetCell) {
        int cellX = queueGridStartX() + (targetCell % COLUMNS) * SLOT_SIZE;
        double localX = mouseX - cellX;
        if (localX < SLOT_SIZE * 0.25) {
            return QueueDropZone.INSERT_BEFORE;
        }
        if (localX >= SLOT_SIZE * 0.75) {
            return QueueDropZone.INSERT_AFTER;
        }
        return QueueDropZone.SWAP;
    }

    private int inventorySlotForDisplayIndex(int displayIndex) {
        return displayIndex < 27 ? displayIndex + 9 : displayIndex - 27;
    }

    private int queueGridStartX() {
        if (responsiveSessionPanelLayout()) {
            int contentWidth = GRID_WIDTH * 2 + SESSION_PANEL_WIDTH + SESSION_PANEL_GAP * 2;
            return (width - contentWidth) / 2;
        }
        if (sideBySideInventoryLayout()) {
            int contentWidth = GRID_WIDTH * 2 + SIDE_BY_SIDE_GRID_GAP;
            return (width - contentWidth) / 2;
        }
        if (normalSessionPanelLayout()) {
            int contentWidth = GRID_WIDTH + SESSION_PANEL_GAP + SESSION_PANEL_WIDTH;
            return (width - contentWidth) / 2;
        }
        return (width - GRID_WIDTH) / 2;
    }

    private int inventoryGridStartX() {
        if (responsiveSessionPanelLayout()) {
            return sessionPanelX() + SESSION_PANEL_WIDTH + SESSION_PANEL_GAP;
        }
        if (sideBySideInventoryLayout()) {
            return queueGridStartX() + GRID_WIDTH + SIDE_BY_SIDE_GRID_GAP;
        }
        return (width - GRID_WIDTH) / 2;
    }

    private int sessionPanelX() {
        return queueGridStartX() + GRID_WIDTH + SESSION_PANEL_GAP;
    }

    private boolean sessionPanelVisible() {
        return responsiveSessionPanelLayout() || normalSessionPanelLayout();
    }

    private boolean responsiveSessionPanelLayout() {
        return sideBySideInventoryLayout() && width >= RESPONSIVE_SESSION_MIN_WIDTH;
    }

    private boolean normalSessionPanelLayout() {
        return !sideBySideInventoryLayout() && width >= NORMAL_SESSION_MIN_WIDTH;
    }

    private int queueHeaderCenterX() {
        return queueGridStartX() + GRID_WIDTH / 2;
    }

    private int inventoryHeaderCenterX() {
        return inventoryGridStartX() + GRID_WIDTH / 2;
    }

    private int queueStartY() {
        return 34;
    }

    private int detailsHeaderY() {
        return queueStartY() + QUEUE_HEIGHT + 12;
    }

    private int detailsIconX() {
        return width / 2 - 8;
    }

    private int detailsIconY() {
        return detailsHeaderY() + 12;
    }

    private int detailsSummaryY() {
        return detailsIconY() + 20;
    }

    private int detailsProgressY() {
        return detailsSummaryY() + 10;
    }

    private int detailsRateY() {
        return detailsProgressY() + 10;
    }

    private int detailsControlsY() {
        return detailsRateY() + 16;
    }

    private int detailsControlsStartX() {
        return width / 2 - 172;
    }

    private int detailsEditorX() {
        return width / 2 - 36;
    }

    private int detailsPreserveX() {
        return width / 2 + 48;
    }

    private int normalInventoryLabelY() {
        return detailsControlsY() + 30;
    }

    private int normalInventoryStartY() {
        return normalInventoryLabelY() + 12;
    }

    private int inventoryLabelY() {
        return sideBySideInventoryLayout() ? 21 : normalInventoryLabelY();
    }

    private int inventoryStartY() {
        return sideBySideInventoryLayout() ? queueStartY() : normalInventoryStartY();
    }

    private int normalSessionModeHeaderY() {
        return normalInventoryStartY() + INVENTORY_ROWS * SLOT_SIZE + 12;
    }

    private int sessionModeHeaderY() {
        return sideBySideInventoryLayout() ? detailsControlsY() + 30 : normalSessionModeHeaderY();
    }

    private int normalActionButtonY() {
        return normalSessionModeHeaderY() + 10 + 28;
    }

    private boolean sideBySideInventoryLayout() {
        return width >= SIDE_BY_SIDE_MIN_WIDTH && height < normalActionButtonY() + LAYOUT_BOTTOM_MARGIN;
    }

    private boolean compactVerticalLayout() {
        return height < actionButtonY() + LAYOUT_BOTTOM_MARGIN;
    }

    private int actionButtonWidth() {
        return 120;
    }

    private int miningModeButtonWidth() {
        return 116;
    }

    private int singleBlockButtonX() {
        return width / 2 - miningModeButtonWidth() - 4;
    }

    private int lineMiningButtonX() {
        return width / 2 + 4;
    }

    private int miningModeButtonsY() {
        return sessionModeHeaderY() + 10;
    }

    private int actionRowTotalWidth() {
        return actionButtonWidth() + 4 + clearQueueButtonWidth() + 4 + closeButtonWidth();
    }

    private int actionButtonX() {
        return width / 2 - actionRowTotalWidth() / 2;
    }

    private int actionButtonY() {
        return miningModeButtonsY() + 28;
    }

    private int clearQueueButtonWidth() {
        return 68;
    }

    private int clearQueueButtonX() {
        return actionButtonX() + actionButtonWidth() + 4;
    }

    private int clearQueueButtonY() {
        return actionButtonY();
    }

    private int closeButtonWidth() {
        return 68;
    }

    private int closeButtonX() {
        return clearQueueButtonX() + clearQueueButtonWidth() + 4;
    }

    private int closeButtonY() {
        return actionButtonY();
    }

    private void drawSlotBackground(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, 0xFF373737);
        gui.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        gui.fill(x + 2, y + 2, x + 17, y + 17, 0xFF242424);
    }

    private void drawPanelBackground(GuiGraphics gui, int x, int y, int panelWidth, int panelHeight) {
        gui.fill(x, y, x + panelWidth, y + panelHeight, 0xFF373737);
        gui.fill(x + 1, y + 1, x + panelWidth - 1, y + panelHeight - 1, 0xFF1E1E1E);
    }

    private void drawOutline(GuiGraphics gui, int x, int y, int color) {
        gui.fill(x, y, x + SLOT_SIZE, y + 1, color);
        gui.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
        gui.fill(x, y, x + 1, y + SLOT_SIZE, color);
        gui.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
    }

    private void drawInnerOutline(GuiGraphics gui, int x, int y, int color) {
        int inset = 2;
        gui.fill(x + inset, y + inset, x + SLOT_SIZE - inset, y + inset + 1, color);
        gui.fill(x + inset, y + SLOT_SIZE - inset - 1, x + SLOT_SIZE - inset, y + SLOT_SIZE - inset, color);
        gui.fill(x + inset, y + inset, x + inset + 1, y + SLOT_SIZE - inset, color);
        gui.fill(x + SLOT_SIZE - inset - 1, y + inset, x + SLOT_SIZE - inset, y + SLOT_SIZE - inset, color);
    }

    private boolean contains(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void resetInventoryDrag() {
        inventoryDragMode = InventoryDragMode.NONE;
        visitedInventorySlotsThisDrag.clear();
    }

    private void resetQueueDrag() {
        queuePressIndex = -1;
        queueDragging = false;
    }

    @Override
    public void onClose() {
        resetInventoryDrag();
        resetQueueDrag();

        if (!PickRelayController.isActive()) {
            queue.clear();
            selectedEntryId = null;
        }

        if (PickRelayController.state() == RelayState.CONFIGURING) {
            PickRelayController.leaveConfiguration();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class DurabilityBudgetSlider extends ExtendedSlider {
        private DurabilityBudgetSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Component.translatable("screen.pickrelay.slider.use"),
                    Component.empty(),
                    1.0D,
                    1.0D,
                    1.0D,
                    true
            );
        }

        private void syncFromEntry(RelayEntry entry) {
            int max = entry == null ? 1 : Math.max(1, maxUsableDurability(entry));
            int current = entry == null ? 1 : Math.max(1, Math.min(entry.workTarget(), max));
            this.minValue = 1.0D;
            this.maxValue = max;
            setValue(current);
        }

        @Override
        protected void applyValue() {
            super.applyValue();
            if (syncingEditor || !PickRelayController.canEditQueue()) {
                return;
            }
            selectedEntry().ifPresent(entry -> {
                if (entry.workMode() == RelayWorkMode.DURABILITY) {
                    entry.setWorkTarget(getValueInt());
                }
            });
        }
    }

    private record SessionLine(Component text, int color) {
    }

    private enum InventoryDragMode {
        NONE,
        ADD,
        REMOVE
    }

    private enum QueueDropZone {
        INSERT_BEFORE,
        SWAP,
        INSERT_AFTER
    }
}
