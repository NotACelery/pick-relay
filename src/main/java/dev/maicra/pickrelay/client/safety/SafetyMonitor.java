package dev.maicra.pickrelay.client.safety;

import dev.maicra.pickrelay.session.StopReason;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SafetyMonitor {
    private static final double POSITION_TOLERANCE_SQ = 1.0E-6;

    private Vec3 anchorPosition;
    private ResourceKey<Level> anchorDimension;

    public void arm(LocalPlayer player) {
        anchorPosition = player.position();
        anchorDimension = player.level().dimension();
    }

    public StopReason check(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return StopReason.DISCONNECT;
        }
        if (!player.isAlive()) {
            return StopReason.PLAYER_DEATH;
        }
        if (anchorDimension == null || !minecraft.level.dimension().equals(anchorDimension)) {
            return StopReason.DIMENSION_CHANGE;
        }
        if (anchorPosition == null || player.position().distanceToSqr(anchorPosition) > POSITION_TOLERANCE_SQ) {
            return StopReason.PLAYER_MOVED;
        }
        return null;
    }

    public void clear() {
        anchorPosition = null;
        anchorDimension = null;
    }
}
