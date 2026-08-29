package dev.maicra.pickrelay.client.progress;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MiningRateEstimator {
    private static final double TICKS_PER_SECOND = 20.0D;

    private MiningRateEstimator() {
    }

    public static Estimate preview(ItemStack tool) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Target target = target(minecraft);
        if (player == null || target == null || tool == null || tool.isEmpty()) {
            return Estimate.unavailable(target);
        }

        return calculatePreview(player, target, tool);
    }

    public static Estimate live() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Target target = target(minecraft);
        if (player == null || minecraft.level == null || target == null) {
            return Estimate.unavailable(target);
        }

        if (target.state().getDestroySpeed(minecraft.level, target.pos()) < 0.0F) {
            return Estimate.unbreakable(target);
        }

        float progressPerTick = target.state().getDestroyProgress(player, minecraft.level, target.pos());
        return fromProgress(target, progressPerTick);
    }

    private static Estimate calculatePreview(LocalPlayer player, Target target, ItemStack tool) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Estimate.unavailable(target);
        }

        float hardness = target.state().getDestroySpeed(minecraft.level, target.pos());
        if (hardness < 0.0F) {
            return Estimate.unbreakable(target);
        }

        float destroySpeed = tool.getDestroySpeed(target.state());
        if (destroySpeed > 1.0F) {
            destroySpeed += (float) attributeValueWithMainHand(player, Attributes.MINING_EFFICIENCY, tool);
        }

        MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
        MobEffectInstance conduit = player.getEffect(MobEffects.CONDUIT_POWER);
        if (haste != null || conduit != null) {
            int hasteAmplifier = haste == null ? -1 : haste.getAmplifier();
            int conduitAmplifier = conduit == null ? -1 : conduit.getAmplifier();
            int amplifier = Math.max(hasteAmplifier, conduitAmplifier);
            destroySpeed *= 1.0F + (amplifier + 1) * 0.2F;
        }

        MobEffectInstance fatigue = player.getEffect(MobEffects.DIG_SLOWDOWN);
        if (fatigue != null) {
            destroySpeed *= switch (fatigue.getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
        }

        destroySpeed *= (float) attributeValueWithMainHand(player, Attributes.BLOCK_BREAK_SPEED, tool);

        if (player.isEyeInFluid(FluidTags.WATER)) {
            destroySpeed *= (float) attributeValueWithMainHand(player, Attributes.SUBMERGED_MINING_SPEED, tool);
        }
        if (!player.onGround()) {
            destroySpeed /= 5.0F;
        }

        boolean correctTool = !target.state().requiresCorrectToolForDrops()
                || tool.isCorrectToolForDrops(target.state());
        float divisor = correctTool ? 30.0F : 100.0F;
        float progressPerTick = hardness == 0.0F ? Float.POSITIVE_INFINITY : destroySpeed / hardness / divisor;
        return fromProgress(target, progressPerTick);
    }

    private static double attributeValueWithMainHand(
            LocalPlayer player,
            Holder<Attribute> attribute,
            ItemStack previewTool) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return 0.0D;
        }

        Set<ResourceLocation> currentMainHandModifierIds = new HashSet<>();
        ItemStack currentTool = player.getMainHandItem();
        currentTool.forEachModifier(EquipmentSlot.MAINHAND, (candidateAttribute, modifier) -> {
            if (candidateAttribute.equals(attribute)) {
                currentMainHandModifierIds.add(modifier.id());
            }
        });

        Map<ResourceLocation, AttributeModifier> modifiers = new HashMap<>();
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!currentMainHandModifierIds.contains(modifier.id())) {
                modifiers.put(modifier.id(), modifier);
            }
        }

        previewTool.forEachModifier(EquipmentSlot.MAINHAND, (candidateAttribute, modifier) -> {
            if (candidateAttribute.equals(attribute)) {
                modifiers.put(modifier.id(), modifier);
            }
        });

        return applyAttributeModifiers(instance.getBaseValue(), new ArrayList<>(modifiers.values()));
    }

    private static double applyAttributeModifiers(double baseValue, List<AttributeModifier> modifiers) {
        double value = baseValue;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                value += modifier.amount();
            }
        }

        double multipliedBase = value;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                multipliedBase += baseValue * modifier.amount();
            }
        }

        for (AttributeModifier modifier : modifiers) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                multipliedBase *= 1.0D + modifier.amount();
            }
        }
        return multipliedBase;
    }

    private static Estimate fromProgress(Target target, float progressPerTick) {
        if (!(progressPerTick > 0.0F) || Float.isNaN(progressPerTick)) {
            return Estimate.unavailable(target);
        }

        int ticksPerBlock = Float.isInfinite(progressPerTick)
                ? 1
                : Math.max(1, (int) Math.ceil(1.0D / progressPerTick));
        double secondsPerBlock = ticksPerBlock / TICKS_PER_SECOND;
        double blocksPerSecond = TICKS_PER_SECOND / ticksPerBlock;
        return new Estimate(target, true, false, blocksPerSecond, secondsPerBlock, ticksPerBlock);
    }

    private static Target target(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return null;
        }

        HitResult hit = minecraft.player.pick(minecraft.player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return null;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        return new Target(pos.immutable(), state);
    }

    public record Estimate(
            Target target,
            boolean available,
            boolean unbreakable,
            double blocksPerSecond,
            double secondsPerBlock,
            int ticksPerBlock) {
        private static Estimate unavailable(Target target) {
            return new Estimate(target, false, false, 0.0D, 0.0D, 0);
        }

        private static Estimate unbreakable(Target target) {
            return new Estimate(target, false, true, 0.0D, 0.0D, 0);
        }
    }

    public record Target(BlockPos pos, BlockState state) {
    }
}
