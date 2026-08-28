package dev.maicra.pickrelay.client.inventory;

import dev.maicra.pickrelay.client.RelayDebug;
import dev.maicra.pickrelay.client.tool.ToolTracker;
import dev.maicra.pickrelay.mixin.MultiPlayerGameModeInvoker;
import dev.maicra.pickrelay.session.RelayEntry;
import dev.maicra.pickrelay.session.RelayEntryStatus;
import dev.maicra.pickrelay.session.RelayQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public final class InventoryRelayManager {
    private InventoryRelayManager() {
    }

    public static boolean equip(RelayQueue queue, RelayEntry entry, RelayEntry previousEntry, LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null || player.containerMenu != player.inventoryMenu) {
            return false;
        }
        if (!ToolTracker.matchesExpectedSlot(entry, player)) {
            return false;
        }

        int sourceSlot = entry.currentInventorySlot();
        if (isHotbarSlot(sourceSlot)) {
            player.getInventory().selected = sourceSlot;
            syncSelectedSlot(minecraft);
            RelayDebug.log("Selected queued tool already in hotbar slot {}", sourceSlot);
            return true;
        }

        int targetHotbar = firstEmptyHotbarSlot(player);
        if (targetHotbar < 0) {
            targetHotbar = relayFallbackHotbarSlot(previousEntry);
        }

        RelayEntry displacedEntry;
        if (previousEntry != null
                && previousEntry != entry
                && previousEntry.currentInventorySlot() == targetHotbar
                && (previousEntry.status() == RelayEntryStatus.PENDING
                        || previousEntry.status() == RelayEntryStatus.ACTIVE)) {
            displacedEntry = previousEntry;
        } else {
            displacedEntry = queue.findByCurrentInventorySlot(targetHotbar)
                    .filter(candidate -> candidate != entry)
                    .orElse(null);
        }

        if (displacedEntry != null && !ToolTracker.matchesTrackedSlot(displacedEntry, player)) {
            RelayDebug.log("Refusing relay swap because tracked destination slot {} is inconsistent", targetHotbar);
            return false;
        }

        ItemStack expectedCandidate = player.getInventory().getItem(sourceSlot).copy();
        ItemStack expectedDisplaced = player.getInventory().getItem(targetHotbar).copy();

        RelayDebug.log("Relay swap inventory {} -> hotbar {}", sourceSlot, targetHotbar);
        minecraft.gameMode.handleInventoryMouseClick(
                player.inventoryMenu.containerId,
                inventorySlotToMenuSlot(sourceSlot),
                targetHotbar,
                ClickType.SWAP,
                player
        );

        ItemStack candidateAfter = player.getInventory().getItem(targetHotbar);
        ItemStack displacedAfter = player.getInventory().getItem(sourceSlot);
        if (!sameStackExact(expectedCandidate, candidateAfter)
                || !sameStackExact(expectedDisplaced, displacedAfter)) {
            RelayDebug.log("Local inventory state did not reflect the expected SWAP operation");
            return false;
        }

        entry.setCurrentInventorySlot(targetHotbar);
        if (displacedEntry != null) {
            displacedEntry.setCurrentInventorySlot(sourceSlot);
        }

        if (!ToolTracker.matchesExpectedSlot(entry, player)) {
            return false;
        }
        if (displacedEntry != null && !ToolTracker.matchesTrackedSlot(displacedEntry, player)) {
            return false;
        }

        player.getInventory().selected = targetHotbar;
        syncSelectedSlot(minecraft);
        return true;
    }

    public static void enforceSelected(RelayEntry entry, LocalPlayer player) {
        int slot = entry.currentInventorySlot();
        if (isHotbarSlot(slot) && player.getInventory().selected != slot) {
            player.getInventory().selected = slot;
            syncSelectedSlot(Minecraft.getInstance());
        }
    }

    private static void syncSelectedSlot(Minecraft minecraft) {
        if (minecraft.gameMode != null) {
            ((MultiPlayerGameModeInvoker) (Object) minecraft.gameMode).pickrelay$ensureHasSentCarriedItem();
        }
    }

    private static boolean sameStackExact(ItemStack expected, ItemStack actual) {
        if (expected.isEmpty() || actual.isEmpty()) {
            return expected.isEmpty() && actual.isEmpty();
        }
        return expected.getCount() == actual.getCount()
                && ItemStack.isSameItemSameComponents(expected, actual);
    }

    private static int firstEmptyHotbarSlot(LocalPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static int relayFallbackHotbarSlot(RelayEntry previousEntry) {
        if (previousEntry != null && isHotbarSlot(previousEntry.currentInventorySlot())) {
            return previousEntry.currentInventorySlot();
        }
        return 0;
    }

    private static int inventorySlotToMenuSlot(int inventorySlot) {

        return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
    }

    private static boolean isHotbarSlot(int inventorySlot) {
        return inventorySlot >= 0 && inventorySlot < 9;
    }
}
