package org.brahypno.esotericismtinker.transcendence.appearance.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-side Stigmata recipe balance.
 */
public final class StigmataConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MATERIAL_UNITS_PER_SLOT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("stigmata");
        MATERIAL_UNITS_PER_SLOT = builder.comment("Minimum TConstruct material units required independently in each of the three material slots.")
                                         .translation("config.esotericism_tinker.stigmata.material_units_per_slot")
                                         .defineInRange("materialUnitsPerSlot", 1, 1, 64);
        builder.pop();
        SPEC = builder.build();
    }

    private StigmataConfig() {}

    public static int materialUnitsPerSlot() {
        return MATERIAL_UNITS_PER_SLOT.get();
    }
}
