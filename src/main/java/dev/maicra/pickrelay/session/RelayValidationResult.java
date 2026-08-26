package dev.maicra.pickrelay.session;

import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record RelayValidationResult(boolean valid, Set<UUID> invalidEntries, Component message) {
    public RelayValidationResult {
        invalidEntries = Collections.unmodifiableSet(invalidEntries);
    }

    public static RelayValidationResult ok() {
        return new RelayValidationResult(true, Set.of(), Component.empty());
    }
}
