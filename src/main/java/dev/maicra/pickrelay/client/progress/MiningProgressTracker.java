package dev.maicra.pickrelay.client.progress;

import dev.maicra.pickrelay.session.RelayEntry;
import dev.maicra.pickrelay.session.RelayWorkMode;
import net.minecraft.world.item.ItemStack;

public final class MiningProgressTracker {
    public void observe(RelayEntry entry, ItemStack liveStack) {
        entry.observeDamage(liveStack);
    }

    public void onBlockDestroyed(RelayEntry entry) {
        entry.incrementBlocksBroken();
    }

    public Completion completion(RelayEntry entry, ItemStack liveStack) {
        if (entry.preserveAtOne() && liveStack.isDamageableItem() && remainingDurability(liveStack) <= 1) {
            return Completion.PRESERVED;
        }

        if (entry.workMode() == RelayWorkMode.DURABILITY && entry.durabilityConsumed() >= entry.workTarget()) {
            return Completion.TARGET_REACHED;
        }
        if (entry.workMode() == RelayWorkMode.BLOCKS && entry.blocksBroken() >= entry.workTarget()) {
            return Completion.TARGET_REACHED;
        }
        return Completion.NONE;
    }

    public static int remainingDurability(ItemStack stack) {
        return stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
    }

    public enum Completion {
        NONE,
        TARGET_REACHED,
        PRESERVED
    }
}
