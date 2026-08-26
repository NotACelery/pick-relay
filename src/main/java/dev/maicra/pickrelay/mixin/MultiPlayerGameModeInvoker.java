package dev.maicra.pickrelay.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeInvoker {
    @Invoker("ensureHasSentCarriedItem")
    void pickrelay$ensureHasSentCarriedItem();
}
