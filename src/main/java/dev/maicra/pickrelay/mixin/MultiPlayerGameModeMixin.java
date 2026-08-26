package dev.maicra.pickrelay.mixin;

import dev.maicra.pickrelay.client.PickRelayController;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Unique
    private int pickrelay$damageBeforeDestroy = -1;

    @Unique
    private boolean pickrelay$relayInitiatedDestroy;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void pickrelay$captureDamageBeforeDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        pickrelay$relayInitiatedDestroy = PickRelayController.isControlledAttackInvocation();
        pickrelay$damageBeforeDestroy = pickrelay$relayInitiatedDestroy
                ? PickRelayController.captureActiveDamageBeforeDestroy()
                : -1;
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void pickrelay$recordSuccessfulDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (pickrelay$relayInitiatedDestroy && cir.getReturnValue()) {
            PickRelayController.onBlockDestroyed(pos, pickrelay$damageBeforeDestroy);
        }
        pickrelay$damageBeforeDestroy = -1;
        pickrelay$relayInitiatedDestroy = false;
    }
}
