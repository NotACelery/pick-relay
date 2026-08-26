package dev.maicra.pickrelay.client.tool;

import dev.maicra.pickrelay.session.RelayEntry;
import dev.maicra.pickrelay.session.RelayEntryStatus;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public final class ToolTracker {
    private ToolTracker() {
    }

    public static ItemStack liveStack(RelayEntry entry, LocalPlayer player) {
        int slot = entry.currentInventorySlot();
        if (slot < 0 || slot >= 36) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(slot);
    }

    public static boolean matchesExpectedSlot(RelayEntry entry, LocalPlayer player) {
        ItemStack live = liveStack(entry, player);
        if (live.isEmpty()) {
            return false;
        }

        if (entry.status() == RelayEntryStatus.PENDING) {
            return ToolFingerprint.matchesConfigured(entry.snapshot(), live);
        }
        return ToolFingerprint.matchesWhileActive(entry.snapshot(), live);
    }

    public static boolean matchesTrackedSlot(RelayEntry entry, LocalPlayer player) {
        ItemStack live = liveStack(entry, player);
        if (live.isEmpty()) {
            return entry.status() == RelayEntryStatus.BROKEN;
        }

        return switch (entry.status()) {
            case PENDING -> ToolFingerprint.matchesConfigured(entry.snapshot(), live);
            case ACTIVE, COMPLETED, PRESERVED, SKIPPED, INVALID -> ToolFingerprint.matchesWhileActive(entry.snapshot(), live);
            case BROKEN -> false;
        };
    }
}
