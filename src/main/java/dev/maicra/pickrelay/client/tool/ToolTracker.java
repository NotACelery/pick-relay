package dev.maicra.pickrelay.client.tool;

import dev.maicra.pickrelay.session.RelayEntry;
import dev.maicra.pickrelay.session.RelayEntryStatus;
import dev.maicra.pickrelay.session.RelayQueue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return matchesEntry(entry, liveStack(entry, player));
    }

    public static boolean matchesTrackedSlot(RelayEntry entry, LocalPlayer player) {
        ItemStack live = liveStack(entry, player);
        if (live.isEmpty()) {
            return entry.status() == RelayEntryStatus.BROKEN
                    || entry.status() == RelayEntryStatus.SKIPPED;
        }

        return switch (entry.status()) {
            case PENDING -> ToolFingerprint.matchesConfigured(entry.snapshot(), live);
            case ACTIVE, COMPLETED, PRESERVED, SKIPPED -> ToolFingerprint.matchesWhileActive(entry.snapshot(), live);
            case BROKEN -> false;
        };
    }

    public static void reconcileQueue(RelayQueue queue, LocalPlayer player) {
        Set<Integer> claimed = new HashSet<>();
        List<RelayEntry> unresolved = new ArrayList<>();

        for (RelayEntry entry : queue.entries()) {
            if (!isRuntimeTracked(entry)) {
                continue;
            }

            int slot = entry.currentInventorySlot();
            if (isInventorySlot(slot)
                    && !claimed.contains(slot)
                    && matchesEntry(entry, player.getInventory().getItem(slot))) {
                claimed.add(slot);
            } else {
                unresolved.add(entry);
            }
        }

        for (RelayEntry entry : unresolved) {
            int resolved = findMatchingUnclaimedSlot(entry, player, claimed);
            if (resolved >= 0) {
                entry.setCurrentInventorySlot(resolved);
                claimed.add(resolved);
            } else {
                entry.setCurrentInventorySlot(-1);
            }
        }
    }

    public static int resolveSlot(RelayEntry entry, LocalPlayer player, Set<Integer> excludedSlots) {
        int current = entry.currentInventorySlot();
        if (isInventorySlot(current)
                && !excludedSlots.contains(current)
                && matchesEntry(entry, player.getInventory().getItem(current))) {
            return current;
        }

        int found = findMatchingUnclaimedSlot(entry, player, excludedSlots);
        entry.setCurrentInventorySlot(found);
        return found;
    }

    public static boolean matchesEntry(RelayEntry entry, ItemStack live) {
        if (live.isEmpty()) {
            return false;
        }
        return entry.status() == RelayEntryStatus.ACTIVE
                ? ToolFingerprint.matchesWhileActive(entry.snapshot(), live)
                : ToolFingerprint.matchesConfigured(entry.snapshot(), live);
    }

    private static int findMatchingUnclaimedSlot(RelayEntry entry, LocalPlayer player, Set<Integer> claimed) {
        if (entry.status() == RelayEntryStatus.ACTIVE) {

            for (int slot = 0; slot < 36; slot++) {
                if (claimed.contains(slot)) {
                    continue;
                }
                ItemStack live = player.getInventory().getItem(slot);
                if (!live.isEmpty() && ToolFingerprint.matchesConfigured(entry.lastKnownSnapshot(), live)) {
                    return slot;
                }
            }

            int uniqueCandidate = -1;
            for (int slot = 0; slot < 36; slot++) {
                if (claimed.contains(slot)) {
                    continue;
                }
                ItemStack live = player.getInventory().getItem(slot);
                if (ToolFingerprint.matchesWhileActive(entry.snapshot(), live)) {
                    if (uniqueCandidate >= 0) {
                        return -1;
                    }
                    uniqueCandidate = slot;
                }
            }
            return uniqueCandidate;
        }

        for (int slot = 0; slot < 36; slot++) {
            if (claimed.contains(slot)) {
                continue;
            }
            if (ToolFingerprint.matchesConfigured(entry.snapshot(), player.getInventory().getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isRuntimeTracked(RelayEntry entry) {
        return entry.status() == RelayEntryStatus.PENDING || entry.status() == RelayEntryStatus.ACTIVE;
    }

    private static boolean isInventorySlot(int slot) {
        return slot >= 0 && slot < 36;
    }
}
