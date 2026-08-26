package dev.maicra.pickrelay.client.gui;

import dev.maicra.pickrelay.client.PickRelayController;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PickRelayScreen extends Screen {
    private static final int COLUMNS = 9;
    private static final int QUEUE_ROWS = 4;
    private static final int INVENTORY_ROWS = 4;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_WIDTH = COLUMNS * SLOT_SIZE;
    private static final int QUEUE_HEIGHT = QUEUE_ROWS * SLOT_SIZE;
    private static final double QUEUE_DRAG_THRESHOLD_SQ = 16.0;

    private final RelayQueue queue = PickRelayController.queue();
    private final Set<Integer> visitedInventorySlotsThisDrag = new HashSet<>();

    private Button actionButton;
    private Button clearQueueButton;
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
        PickRelayController.enterConfiguration();

        int controlsY = detailsControlsY();
        modeButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleMode())
                .bounds(width / 2 - 150, controlsY, 122, 20)
                .build());

        targetEdit = addRenderableWidget(new EditBox(
                font,
                width / 2 - 22,
                controlsY,
                64,
                20,
                Component.translatable("screen.pickrelay.target")
        ));
        targetEdit.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        targetEdit.setMaxLength(8);
        targetEdit.setResponder(this::onTargetChanged);

        durabilitySlider = addRenderableWidget(new DurabilityBudgetSlider(
                width / 2 - 22,
                controlsY,
                64,
                20
        ));

        preserveButton = addRenderableWidget(Button.builder(Component.empty(), button -> togglePreserve())
                .bounds(width / 2 + 48, controlsY, 112, 20)
                .build());

        singleBlockButton = addRenderableWidget(Button.builder(Component.empty(), button -> setMiningMode(RelayMiningMode.SINGLE_BLOCK))
                .bounds(singleBlockButtonX(), miningModeButtonsY(), miningModeButtonWidth(), 20)
                .build());

        lineMiningButton = addRenderableWidget(Button.builder(Component.empty(), button -> setMiningMode(RelayMiningMode.LINE_MINING))
                .bounds(lineMiningButtonX(), lineMiningButtonY(), miningModeButtonWidth(), 20)
                .build());

        actionButton = addRenderableWidget(Button.builder(actionLabel(), button -> onAction())
                .bounds(actionButtonX(), actionButtonY(), actionButtonWidth(), 20)
                .build());

        clearQueueButton = addRenderableWidget(Button.builder(Component.translatable("screen.pickrelay.clear_queue"), button -> clearQueue())
                .bounds(clearQueueButtonX(), clearQueueButtonY(), clearQueueButtonWidth(), 20)
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
        renderSelectedDetails(gui);
        renderPlayerInventory(gui, mouseX, mouseY);
        renderValidation(gui);
        renderQueueDragFeedback(gui, mouseX, mouseY);
        renderHoveredTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                        selectedEntryId = queue.isEmpty() ? null : queue.get(Math.min(queueCell, queue.size() - 1)).id();
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
                if (selected.originalInventorySlot() == inventorySlot) {
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
        if (actionButton == null || clearQueueButton == null || singleBlockButton == null || lineMiningButton == null
                || modeButton == null || preserveButton == null || targetEdit == null || durabilitySlider == null) {
            return;
        }

        RelayEntry entry = selectedEntry().orElse(null);
        boolean editable = PickRelayController.canEditQueue();
        boolean hasEntry = entry != null;

        actionButton.setMessage(actionLabel());
        actionButton.active = PickRelayController.isActive() || PickRelayController.canStart();
        clearQueueButton.active = editable && !queue.isEmpty();
        clearQueueButton.visible = !compactVerticalLayout();

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
        int startX = gridStartX();
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

            if (entry.status() == RelayEntryStatus.COMPLETED || entry.status() == RelayEntryStatus.BROKEN || entry.status() == RelayEntryStatus.PRESERVED) {
                gui.fill(x + 1, y + 1, x + 17, y + 17, 0x66000000);
            }

            ItemStack stack = displayStack(entry);
            gui.renderItem(stack, x + 1, y + 1);
            gui.renderItemDecorations(font, stack, x + 1, y + 1);

            if (entry.status() == RelayEntryStatus.ACTIVE) {
                drawOutline(gui, x, y, 0xFFFFFFFF);
                gui.drawString(font, ">", x + 11, y + 1, 0xFFFFFFFF, true);
            } else if (invalid.contains(entry.id())) {
                drawOutline(gui, x, y, 0xFFFF5555);
            } else if (selected) {
                drawOutline(gui, x, y, 0xFFAAAAFF);
            } else if (hovered && !queueDragging) {
                drawOutline(gui, x, y, 0xFFB0B0B0);
            }

            gui.drawString(font, String.format("%02d", cell + 1), x + 1, y + 1, 0xFFE0E0E0, true);
        }
    }

    private void renderSelectedDetails(GuiGraphics gui) {
        RelayEntry entry = selectedEntry().orElse(null);
        int y = detailsTextY();
        if (entry == null) {
            gui.drawCenteredString(font, Component.translatable("screen.pickrelay.details.none"), width / 2, y, 0xD0D0D0);
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
                Component.translatable("screen.pickrelay.details.summary", position, queue.size(), display.getHoverName(), durability),
                width / 2,
                y,
                0xD0D0D0
        );

        Component progress = progressText(entry);
        gui.drawCenteredString(font, progress, width / 2, y + 10, 0xA8A8A8);
    }

    private Component progressText(RelayEntry entry) {
        if (entry.status() == RelayEntryStatus.ACTIVE) {
            Component base = switch (entry.workMode()) {
                case UNTIL_BROKEN -> Component.translatable("screen.pickrelay.progress.until_broken");
                case DURABILITY -> Component.translatable("screen.pickrelay.progress.durability", entry.durabilityConsumed(), entry.workTarget());
                case BLOCKS -> Component.translatable("screen.pickrelay.progress.blocks", entry.blocksBroken(), entry.workTarget());
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
        int startX = gridStartX();
        int startY = inventoryStartY();

        gui.drawCenteredString(font, Component.translatable("screen.pickrelay.inventory"), width / 2, startY - 10, 0xC0C0C0);

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

    private boolean isInventorySlotQueued(int inventorySlot) {
        if (PickRelayController.isActive()) {
            return queue.entries().stream().anyMatch(entry ->
                    (entry.status() == RelayEntryStatus.PENDING || entry.status() == RelayEntryStatus.ACTIVE)
                            && entry.currentInventorySlot() == inventorySlot
            );
        }
        return queue.containsInventorySlot(inventorySlot);
    }

    private void renderQueueHeader(GuiGraphics gui) {
        if (compactVerticalLayout() && PickRelayController.canEditQueue() && !queue.isEmpty()) {
            RelayValidationResult validation = PickRelayController.validation();
            if (!validation.valid() && !validation.message().getString().isEmpty()) {
                gui.drawCenteredString(font, validation.message(), width / 2, 21, 0xFFFF7777);
                return;
            }
        }
        gui.drawCenteredString(font, queueHeader(), width / 2, 21, 0xC0C0C0);
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
            int x = gridStartX() + (targetCell % COLUMNS) * SLOT_SIZE;
            int y = queueStartY() + (targetCell / COLUMNS) * SLOT_SIZE;
            drawOutline(gui, x, y, 0xFFFFFFFF);
        } else {
            drawInsertionMarker(gui, targetCell, zone == QueueDropZone.INSERT_AFTER);
        }
    }

    private void drawInsertionMarker(GuiGraphics gui, int targetCell, boolean after) {
        int x = gridStartX() + (targetCell % COLUMNS) * SLOT_SIZE;
        int y = queueStartY() + (targetCell / COLUMNS) * SLOT_SIZE;
        int markerX = after ? x + SLOT_SIZE - 1 : x;
        gui.fill(markerX, y, markerX + 2, y + SLOT_SIZE, 0xFFFFFFFF);
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
            lines.add(Component.translatable("tooltip.pickrelay.position", queueCell + 1, queue.size()).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("tooltip.pickrelay.inventory_slot", inventoryLocation(entry.currentInventorySlot())).withStyle(ChatFormatting.DARK_GRAY));
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
            lines.add(Component.translatable("tooltip.pickrelay.mode", Component.translatable(workModeTranslation(entry))).withStyle(ChatFormatting.DARK_GRAY));
            if (entry.workMode() != RelayWorkMode.UNTIL_BROKEN) {
                lines.add(Component.translatable("tooltip.pickrelay.target", entry.workTarget()).withStyle(ChatFormatting.DARK_GRAY));
            }
            if (entry.preserveAtOne()) {
                lines.add(Component.translatable("tooltip.pickrelay.preserve").withStyle(ChatFormatting.DARK_GRAY));
            }
            lines.add(Component.translatable("tooltip.pickrelay.status", Component.translatable(statusTranslation(entry.status())))
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (entry.status() == RelayEntryStatus.ACTIVE) {
                lines.add(progressText(entry).copy().withStyle(ChatFormatting.WHITE));
            } else if (PickRelayController.canEditQueue() && minecraft.player != null) {
                ItemStack live = minecraft.player.getInventory().getItem(entry.originalInventorySlot());
                Component problem = RelayValidator.validateEntry(entry, live);
                if (problem != null) {
                    lines.add(Component.translatable("tooltip.pickrelay.problem", problem).withStyle(ChatFormatting.RED));
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
            case INVALID -> "tooltip.pickrelay.status.invalid";
        };
    }

    private java.util.Optional<RelayEntry> selectedEntry() {
        return queue.findById(selectedEntryId);
    }

    private int queueCellAt(double mouseX, double mouseY) {
        return gridCellAt(mouseX, mouseY, queueStartY(), QUEUE_ROWS);
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        int displayIndex = gridCellAt(mouseX, mouseY, inventoryStartY(), INVENTORY_ROWS);
        return displayIndex < 0 ? -1 : inventorySlotForDisplayIndex(displayIndex);
    }

    private int gridCellAt(double mouseX, double mouseY, int startY, int rows) {
        int startX = gridStartX();
        if (mouseX < startX || mouseX >= startX + GRID_WIDTH || mouseY < startY || mouseY >= startY + rows * SLOT_SIZE) {
            return -1;
        }

        int column = (int) ((mouseX - startX) / SLOT_SIZE);
        int row = (int) ((mouseY - startY) / SLOT_SIZE);
        return row * COLUMNS + column;
    }

    private QueueDropZone queueDropZone(double mouseX, int targetCell) {
        int cellX = gridStartX() + (targetCell % COLUMNS) * SLOT_SIZE;
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

    private int gridStartX() {
        return (width - GRID_WIDTH) / 2;
    }

    private int queueStartY() {
        return 31;
    }

    private int detailsIconX() {
        return gridStartX();
    }

    private int detailsIconY() {
        return detailsTextY() - 3;
    }

    private int detailsTextY() {
        return queueStartY() + QUEUE_HEIGHT + 4;
    }

    private int detailsControlsY() {
        return detailsTextY() + 20;
    }

    private int inventoryStartY() {
        return detailsControlsY() + 31;
    }

    private boolean compactVerticalLayout() {
        // The session mode selector adds one more control row in spacious GUIs.
        // On shorter screens all three session controls live beside the queue.
        return height < inventoryStartY() + 136;
    }

    private int actionButtonWidth() {
        if (!compactVerticalLayout()) {
            return 106;
        }
        int sideSpace = (width - GRID_WIDTH) / 2 - 8;
        return Math.max(64, Math.min(104, sideSpace));
    }

    private int miningModeButtonWidth() {
        return compactVerticalLayout() ? actionButtonWidth() : 104;
    }

    private int singleBlockButtonX() {
        return compactVerticalLayout()
                ? actionButtonX()
                : width / 2 - miningModeButtonWidth() - 2;
    }

    private int lineMiningButtonX() {
        return compactVerticalLayout()
                ? actionButtonX()
                : width / 2 + 2;
    }

    private int miningModeButtonsY() {
        return compactVerticalLayout() ? queueStartY() : inventoryStartY() + 75;
    }

    private int lineMiningButtonY() {
        return compactVerticalLayout() ? miningModeButtonsY() + 24 : miningModeButtonsY();
    }

    private int actionButtonX() {
        if (!compactVerticalLayout()) {
            return (width - 150) / 2;
        }
        return gridStartX() + GRID_WIDTH + 4;
    }

    private int actionButtonY() {
        if (compactVerticalLayout()) {
            return queueStartY() + 48;
        }
        return inventoryStartY() + 99;
    }

    private int clearQueueButtonWidth() {
        return 40;
    }

    private int clearQueueButtonX() {
        return actionButtonX() + actionButtonWidth() + 4;
    }

    private int clearQueueButtonY() {
        return actionButtonY();
    }

    private void drawSlotBackground(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, 0xFF373737);
        gui.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        gui.fill(x + 2, y + 2, x + 17, y + 17, 0xFF242424);
    }

    private void drawOutline(GuiGraphics gui, int x, int y, int color) {
        gui.fill(x, y, x + SLOT_SIZE, y + 1, color);
        gui.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
        gui.fill(x, y, x + 1, y + SLOT_SIZE, color);
        gui.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
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
