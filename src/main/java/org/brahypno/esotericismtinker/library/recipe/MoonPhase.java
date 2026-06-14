package org.brahypno.esotericismtinker.library.recipe;

import java.util.Locale;

public enum MoonPhase {
    FULL_MOON(0, "full_moon"),
    WANING_GIBBOUS(1, "waning_gibbous"),
    LAST_QUARTER(2, "last_quarter"),
    WANING_CRESCENT(3, "waning_crescent"),
    NEW_MOON(4, "new_moon"),
    WAXING_CRESCENT(5, "waxing_crescent"),
    FIRST_QUARTER(6, "first_quarter"),
    WAXING_GIBBOUS(7, "waxing_gibbous");

    private final int vanillaId;
    private final String name;

    MoonPhase(int vanillaId, String name) {
        this.vanillaId = vanillaId;
        this.name = name;
    }

    public int vanillaId() {
        return vanillaId;
    }

    public String serializedName() {
        return name;
    }

    public static MoonPhase fromVanillaId(int id) {
        int normalized = Math.floorMod(id, 8);

        for (MoonPhase phase : values()) {
            if (phase.vanillaId == normalized){
                return phase;
            }
        }

        return FULL_MOON;
    }

    public static MoonPhase byName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);

        for (MoonPhase phase : values()) {
            if (phase.name.equals(normalized)){
                return phase;
            }
        }

        throw new IllegalArgumentException("Unknown moon phase: " + name);
    }
}
