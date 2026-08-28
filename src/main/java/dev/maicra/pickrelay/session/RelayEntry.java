package dev.maicra.pickrelay.session;

import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class RelayEntry {
    private final UUID id;
    private int currentInventorySlot;
    private final ItemStack snapshot;
    private ItemStack lastKnownSnapshot;
    private RelayWorkMode workMode;
    private int workTarget;
    private boolean preserveAtOne;
    private RelayEntryStatus status;

    private int blocksBroken;
    private int durabilityConsumed;
    private int lastObservedDamage;

    public RelayEntry(int initialInventorySlot, ItemStack stack) {
        this.id = UUID.randomUUID();
        this.currentInventorySlot = initialInventorySlot;
        this.snapshot = stack.copy();
        this.lastKnownSnapshot = stack.copy();
        this.workMode = RelayWorkMode.UNTIL_BROKEN;
        this.workTarget = 0;
        this.preserveAtOne = false;
        this.status = RelayEntryStatus.PENDING;
    }

    public UUID id() {
        return id;
    }

    public int currentInventorySlot() {
        return currentInventorySlot;
    }

    public void setCurrentInventorySlot(int currentInventorySlot) {
        this.currentInventorySlot = currentInventorySlot;
    }

    public ItemStack snapshot() {
        return snapshot;
    }

    public ItemStack lastKnownSnapshot() {
        return lastKnownSnapshot;
    }

    public void rememberLiveStack(ItemStack liveStack) {
        if (!liveStack.isEmpty()) {
            lastKnownSnapshot = liveStack.copy();
        }
    }

    public RelayWorkMode workMode() {
        return workMode;
    }

    public void setWorkMode(RelayWorkMode workMode) {
        this.workMode = workMode;
    }

    public int workTarget() {
        return workTarget;
    }

    public void setWorkTarget(int workTarget) {
        this.workTarget = Math.max(0, workTarget);
    }

    public boolean preserveAtOne() {
        return preserveAtOne;
    }

    public void setPreserveAtOne(boolean preserveAtOne) {
        this.preserveAtOne = preserveAtOne;
    }

    public RelayEntryStatus status() {
        return status;
    }

    public void setStatus(RelayEntryStatus status) {
        this.status = status;
    }

    public int blocksBroken() {
        return blocksBroken;
    }

    public void incrementBlocksBroken() {
        blocksBroken++;
    }

    public int durabilityConsumed() {
        return durabilityConsumed;
    }

    public void recordDurabilityConsumption(int amount, int observedDamage) {
        if (amount > 0) {
            durabilityConsumed += amount;
        }
        lastObservedDamage = Math.max(0, observedDamage);
    }

    public int lastObservedDamage() {
        return lastObservedDamage;
    }

    public void beginRuntime(ItemStack liveStack) {
        status = RelayEntryStatus.ACTIVE;
        blocksBroken = 0;
        durabilityConsumed = 0;
        lastObservedDamage = liveStack.isDamageableItem() ? liveStack.getDamageValue() : 0;
        rememberLiveStack(liveStack);
    }

    public void observeDamage(ItemStack liveStack) {
        if (!liveStack.isDamageableItem()) {
            rememberLiveStack(liveStack);
            return;
        }

        int currentDamage = liveStack.getDamageValue();
        if (currentDamage > lastObservedDamage) {
            durabilityConsumed += currentDamage - lastObservedDamage;
        }

        lastObservedDamage = currentDamage;
        rememberLiveStack(liveStack);
    }

    public void resetRuntime() {
        status = RelayEntryStatus.PENDING;
        blocksBroken = 0;
        durabilityConsumed = 0;
        lastObservedDamage = snapshot.isDamageableItem() ? snapshot.getDamageValue() : 0;
        lastKnownSnapshot = snapshot.copy();
    }

    public int progress() {
        return switch (workMode) {
            case UNTIL_BROKEN -> 0;
            case DURABILITY -> durabilityConsumed;
            case BLOCKS -> blocksBroken;
        };
    }
}
