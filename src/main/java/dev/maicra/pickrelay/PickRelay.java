package dev.maicra.pickrelay;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(PickRelay.MOD_ID)
public final class PickRelay {
    public static final String MOD_ID = "pickrelay";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PickRelay(IEventBus modEventBus, ModContainer modContainer) {
        // Pick Relay is intentionally client-side. Client registration is isolated
        // in dev.maicra.pickrelay.client through Dist.CLIENT event subscribers.
    }
}
