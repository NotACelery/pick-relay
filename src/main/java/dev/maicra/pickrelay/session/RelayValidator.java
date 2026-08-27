package dev.maicra.pickrelay.session;

import dev.maicra.pickrelay.client.tool.ToolEligibility;
import dev.maicra.pickrelay.client.tool.ToolFingerprint;
import dev.maicra.pickrelay.client.tool.ToolTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class RelayValidator {
    private RelayValidator() {
    }

    public static RelayValidationResult validate(RelayQueue queue, LocalPlayer player) {
        if (player == null) {
            return new RelayValidationResult(false, Set.of(), Component.translatable("screen.pickrelay.validation.no_player"));
        }
        if (queue.isEmpty()) {
            return new RelayValidationResult(false, Set.of(), Component.translatable("screen.pickrelay.validation.empty"));
        }
        if (player.containerMenu != player.inventoryMenu) {
            return new RelayValidationResult(false, Set.of(), Component.translatable("screen.pickrelay.validation.container_open"));
        }

        ToolTracker.reconcileQueue(queue, player);

        Set<UUID> invalid = new LinkedHashSet<>();
        Component firstProblem = Component.empty();
        int availableTools = 0;

        for (RelayEntry entry : queue.entries()) {
            int slot = entry.currentInventorySlot();
            if (slot < 0 || slot >= 36) {
                continue;
            }

            ItemStack live = player.getInventory().getItem(slot);
            if (!ToolFingerprint.matchesConfigured(entry.snapshot(), live)) {
                continue;
            }
            availableTools++;

            Component problem = validateEntry(entry, live);
            if (problem != null) {
                invalid.add(entry.id());
                if (firstProblem.getString().isEmpty()) {
                    firstProblem = problem;
                }
            }
        }

        if (availableTools == 0) {
            return new RelayValidationResult(false, Set.of(), Component.translatable("screen.pickrelay.validation.no_available_tools"));
        }
        if (!invalid.isEmpty()) {
            return new RelayValidationResult(false, invalid, firstProblem);
        }
        return RelayValidationResult.ok();
    }

    public static Component validateEntry(RelayEntry entry, ItemStack live) {
        if (!ToolEligibility.isSupported(live) || !ToolFingerprint.matchesConfigured(entry.snapshot(), live)) {
            return Component.translatable("screen.pickrelay.validation.tool_changed");
        }

        boolean damageable = live.isDamageableItem();
        int remaining = damageable ? live.getMaxDamage() - live.getDamageValue() : Integer.MAX_VALUE;

        if (entry.preserveAtOne() && !damageable) {
            return Component.translatable("screen.pickrelay.validation.preserve_requires_durability");
        }

        return switch (entry.workMode()) {
            case UNTIL_BROKEN -> damageable
                    ? null
                    : Component.translatable("screen.pickrelay.validation.until_requires_durability");
            case DURABILITY -> {
                if (!damageable) {
                    yield Component.translatable("screen.pickrelay.validation.durability_requires_durability");
                }
                int usable = Math.max(0, remaining - (entry.preserveAtOne() ? 1 : 0));
                if (entry.workTarget() <= 0 || entry.workTarget() > usable) {
                    yield Component.translatable("screen.pickrelay.validation.durability_target", usable);
                }
                yield null;
            }
            case BLOCKS -> entry.workTarget() > 0
                    ? null
                    : Component.translatable("screen.pickrelay.validation.blocks_target");
        };
    }
}
