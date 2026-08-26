package dev.maicra.pickrelay.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.maicra.pickrelay.PickRelay;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = PickRelay.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class RelayKeyMappings {
    public static final KeyMapping OPEN_RELAY = new KeyMapping(
            "key.pickrelay.open",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_5,
            "key.categories.pickrelay"
    );

    private RelayKeyMappings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_RELAY);
    }
}
