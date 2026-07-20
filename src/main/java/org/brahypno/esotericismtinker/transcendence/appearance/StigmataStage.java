package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The three progressive stages of the single Stigmata system.
 * The modifier's externally visible level is deliberately not used as the state source.
 */
public enum StigmataStage implements StringRepresentable {
    MANIFESTATION(1, "manifestation"),
    ALIENATION(2, "alienation"),
    SEALING(3, "sealing");

    private final int index;
    private final String serializedName;

    StigmataStage(int index, String serializedName) {
        this.index = index;
        this.serializedName = serializedName;
    }

    public int index() {
        return index;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static @Nullable StigmataStage byIndex(int index) {
        for (StigmataStage value : values()) {
            if (value.index == index){
                return value;
            }
        }
        return null;
    }

    public static @Nullable StigmataStage byName(String name) {
        if (name == null){
            return null;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        for (StigmataStage value : values()) {
            if (value.serializedName.equals(normalized)){
                return value;
            }
        }
        return null;
    }
}
