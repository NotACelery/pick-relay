package dev.maicra.pickrelay;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(PickRelay.MOD_ID)
public final class PickRelay {
    public static final String MOD_ID = "pickrelay";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PickRelay(IEventBus modEventBus, ModContainer modContainer) {
    }
}
