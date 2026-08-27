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

/**
 * Tracks queued tools by their RelayEntry identity instead of treating the
 * inventory slot as the identity itself.
 *
 * The UUID lives only in Pick Relay's local queue model; ItemStacks are never
 * mutated with hidden NBT/components. A current slot is therefore just a
 * locator that may be repaired whenever the player moves the tool.
 */
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

    /**
     * Reconciles all pending/active entries against the player's current 36
     * inventory slots. Existing valid locations are kept first, then moved
     * tools are reassigned one-to-one to unclaimed matching slots.
     *
     * Missing pending entries are deliberately left unresolved; the session
     * will mark them SKIPPED when their turn arrives instead of stopping.
     */
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

    /** Resolve one entry after the global reconciliation pass. */
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
            // When an active tool was manually moved, prefer its last exact live
            // snapshot (including its current damage). This prevents a pristine,
            // otherwise identical pending tool from being mistaken for it.
            for (int slot = 0; slot < 36; slot++) {
                if (claimed.contains(slot)) {
                    continue;
                }
                ItemStack live = player.getInventory().getItem(slot);
                if (!live.isEmpty() && ToolFingerprint.matchesConfigured(entry.lastKnownSnapshot(), live)) {
                    return slot;
                }
            }

            // Fallback for unusual external component/damage changes: only accept
            // a broad active fingerprint when exactly one candidate exists.
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
