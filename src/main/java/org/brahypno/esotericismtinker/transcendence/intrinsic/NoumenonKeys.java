package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.resources.ResourceLocation;

public final class NoumenonKeys {
    public static final String MOD_ID = "esotericism_tinker";

    public static final ResourceLocation LEVEL = id("noumenon_level");
    public static final ResourceLocation SUBSTRATE_POINTS = id("noumenon_substrate_points");
    public static final ResourceLocation ELEVATION_POINTS = id("noumenon_elevation_points");

    /**
     * Debug-only adjustments layered on top of level-derived point supplies.
     */
    public static final ResourceLocation DEBUG_SUBSTRATE_POINTS = id("noumenon_debug_substrate_points");
    public static final ResourceLocation DEBUG_ELEVATION_POINTS = id("noumenon_debug_elevation_points");

    /**
     * Raw TConstruct slot type names, for example upgrades or abilities.
     */
    public static final ResourceLocation RECEPTION_SLOTS = id("noumenon_reception_slots");
    public static final ResourceLocation SUBLIMATIONS = id("noumenon_sublimations");
    public static final ResourceLocation TUNINGS = id("noumenon_tunings");

    /**
     * Source tool definition captured by Investiture.
     */
    public static final ResourceLocation INVESTED_DEFINITION = id("noumenon_invested_definition");
    public static final ResourceLocation INVESTITURE_LOCKED = id("noumenon_investiture_locked");
    public static final ResourceLocation INVESTED_TRAITS = id("noumenon_invested_traits");
    public static final ResourceLocation INVESTITURE_REJECTION = id("noumenon_investiture_rejection");

    public static final ResourceLocation REJECTION = id("noumenon_rejection");

    private NoumenonKeys() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
