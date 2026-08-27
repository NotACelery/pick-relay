package dev.maicra.pickrelay.session;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class RelayQueue {
    public static final int MAX_ENTRIES = 36;

    private final List<RelayEntry> entries = new ArrayList<>(MAX_ENTRIES);

    public List<RelayEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean isFull() {
        return entries.size() >= MAX_ENTRIES;
    }

    public RelayEntry get(int index) {
        return entries.get(index);
    }

    public Optional<RelayEntry> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst();
    }

    public Optional<RelayEntry> findByCurrentInventorySlot(int inventorySlot) {
        return entries.stream()
                .filter(entry -> entry.currentInventorySlot() == inventorySlot)
                .filter(entry -> entry.status() == RelayEntryStatus.PENDING || entry.status() == RelayEntryStatus.ACTIVE)
                .findFirst();
    }


    public int indexOf(UUID id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public boolean containsInventorySlot(int inventorySlot) {
        return entries.stream().anyMatch(entry -> entry.currentInventorySlot() == inventorySlot);
    }

    public boolean add(int inventorySlot, ItemStack stack) {
        if (stack.isEmpty() || isFull() || containsInventorySlot(inventorySlot)) {
            return false;
        }
        entries.add(new RelayEntry(inventorySlot, stack));
        return true;
    }

    public boolean removeByInventorySlot(int inventorySlot) {
        return entries.removeIf(entry -> entry.currentInventorySlot() == inventorySlot);
    }

    public RelayEntry removeAt(int index) {
        if (index < 0 || index >= entries.size()) {
            return null;
        }
        return entries.remove(index);
    }

    public void swap(int firstIndex, int secondIndex) {
        if (!isValidIndex(firstIndex) || !isValidIndex(secondIndex) || firstIndex == secondIndex) {
            return;
        }
        Collections.swap(entries, firstIndex, secondIndex);
    }

    public void moveToInsertionPoint(int fromIndex, int insertionPoint) {
        if (!isValidIndex(fromIndex)) {
            return;
        }

        int clampedPoint = Math.max(0, Math.min(insertionPoint, entries.size()));
        if (clampedPoint == fromIndex || clampedPoint == fromIndex + 1) {
            return;
        }

        RelayEntry entry = entries.remove(fromIndex);
        if (fromIndex < clampedPoint) {
            clampedPoint--;
        }
        entries.add(Math.max(0, Math.min(clampedPoint, entries.size())), entry);
    }

    public void resetRuntime() {
        entries.forEach(RelayEntry::resetRuntime);
    }

    public void clear() {
        entries.clear();
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < entries.size();
    }
}
