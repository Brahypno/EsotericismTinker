package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record StigmataMutationResult(boolean success, @Nullable Component error) {
    public static StigmataMutationResult succeeded() {
        return new StigmataMutationResult(true, null);
    }

    public static StigmataMutationResult failure(String translationKey, Object... args) {
        return new StigmataMutationResult(false, Component.translatable(translationKey, args));
    }
}
