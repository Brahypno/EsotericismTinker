package org.brahypno.esotericismtinker.library.recipe.selenic;

import net.minecraft.network.chat.Component;

public enum SelenicFailure {
    NONE("none"),
    NO_CROWN("no_crown"),
    AXIS_BLOCKED("axis_blocked"),
    NO_SPINE("no_spine"),
    NO_INPUT_CROWN("no_input_crown"),

    WRONG_ELEVATION("wrong_elevation"),
    DAYTIME("daytime"),
    WRONG_MOON_PHASE("wrong_moon_phase"),
    MISSING_CROWN_INPUT("missing_crown_input"),
    MISSING_TESTIMONY("missing_testimony"),
    MISSING_MEDIUM("missing_medium"),

    NO_RECIPE("no_recipe"),
    OUTPUT_ITEM_BLOCKED("output_item_blocked"),
    OUTPUT_FLUID_BLOCKED("output_fluid_blocked"),
    ALREADY_ACTIVE("already_active"),
    INTERRUPTED("interrupted");

    private final String key;

    SelenicFailure(String key) {
        this.key = key;
    }

    public Component component() {
        return Component.translatable("message.esotericism_tinker.selenic." + key);
    }

    public static SelenicFailure fromRequirement(SelenicRequirementCheck check) {
        return switch (check) {
            case OK -> NONE;
            case WRONG_ELEVATION -> WRONG_ELEVATION;
            case DAYTIME -> DAYTIME;
            case WRONG_MOON_PHASE -> WRONG_MOON_PHASE;
            case CROWN_INPUT -> MISSING_CROWN_INPUT;
            case TESTIMONY -> MISSING_TESTIMONY;
            case MEDIUM -> MISSING_MEDIUM;
        };
    }
}