package org.brahypno.esotericismtinker.library.recipe;


import java.util.Locale;

public enum FluidOutputMode {
    INSTANT,
    OVER_TIME;

    public static FluidOutputMode byName(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "over_time" -> OVER_TIME;
            default -> INSTANT;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
