package dev.maicra.pickrelay.client.tool;

import net.minecraft.world.item.ItemStack;

public final class ToolFingerprint {
    private ToolFingerprint() {
    }

    public static boolean matchesConfigured(ItemStack expected, ItemStack actual) {
        if (expected.isEmpty() || actual.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(expected, actual);
    }

    public static boolean matchesWhileActive(ItemStack expected, ItemStack actual) {
        if (expected.isEmpty() || actual.isEmpty()) {
            return false;
        }

        ItemStack normalizedExpected = expected.copyWithCount(1);
        ItemStack normalizedActual = actual.copyWithCount(1);
        if (normalizedExpected.isDamageableItem()) {
            normalizedExpected.setDamageValue(0);
        }
        if (normalizedActual.isDamageableItem()) {
            normalizedActual.setDamageValue(0);
        }
        return ItemStack.isSameItemSameComponents(normalizedExpected, normalizedActual);
    }
}
