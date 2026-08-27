package dev.maicra.pickrelay.client;

import dev.maicra.pickrelay.PickRelay;
import dev.maicra.pickrelay.client.gui.PickRelayScreen;
import dev.maicra.pickrelay.client.hud.PickRelayHud;
import dev.maicra.pickrelay.session.StopReason;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = PickRelay.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        while (RelayKeyMappings.OPEN_RELAY.consumeClick()) {
            if (minecraft.player != null) {
                minecraft.setScreen(new PickRelayScreen());
            }
        }

        PickRelayController.tick();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PickRelayController.stop(StopReason.DISCONNECT);
    }

    @SubscribeEvent
    public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        if (PickRelayController.isActive()) {
            PickRelayController.stop(StopReason.PLAYER_DEATH);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        PickRelayHud.render(event.getGuiGraphics());
    }
}
