package dev.maicra.pickrelay.client.tool;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public final class ToolEligibility {
    private ToolEligibility() {
    }

    public static boolean isSupported(ItemStack stack) {
        return !stack.isEmpty() && (
                stack.is(ItemTags.PICKAXES)
                        || stack.is(ItemTags.AXES)
                        || stack.is(ItemTags.SHOVELS)
                        || stack.is(ItemTags.HOES)
        );
    }
}
