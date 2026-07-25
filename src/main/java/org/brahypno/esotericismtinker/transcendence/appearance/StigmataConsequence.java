package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum StigmataConsequence {
    EXALTATION,
    MALEDICTION,
    JUDGEMENT,
    OBSESSION,
    INCUBATION,
    DEFILEMENT,
    DEVOURING,
    SACRAMENT,
    OFFERING,
    IMMOLATION,
    PENANCE,
    DOMINION,
    NOCTURNE,
    ZENITH,
    ABYSS,
    BEATITUDE,
    ANATHEMA;

    public static StigmataConsequence fromSeed(int seed) {
        StigmataConsequence[] values = values();
        return values[Math.floorMod(seed, values.length)];
    }

    public Component stageName(StigmataStage stage) {
        return Component.translatable(
                "stigmata_consequence.esotericism_tinker."
                + name().toLowerCase(Locale.ROOT)
                + "."
                + stage.getSerializedName()
        );
    }

    public Component displayName() {
        return Component.translatable(
                "stigmata_consequence.esotericism_tinker."
                + name().toLowerCase(Locale.ROOT)
                + ".name"
        );
    }

    public Component armorStageName(StigmataStage stage) {
        return Component.translatable(
                "stigmata_consequence.esotericism_tinker."
                + name().toLowerCase(Locale.ROOT)
                + ".armor."
                + stage.getSerializedName()
        );
    }
}
