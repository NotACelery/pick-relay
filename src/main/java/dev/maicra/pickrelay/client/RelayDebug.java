package dev.maicra.pickrelay.client;

import dev.maicra.pickrelay.PickRelay;

public final class RelayDebug {
    private RelayDebug() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("pickrelay.debug");
    }

    public static void log(String message, Object... args) {
        if (enabled()) {
            PickRelay.LOGGER.info("[Pick Relay] " + message, args);
        }
    }
}
