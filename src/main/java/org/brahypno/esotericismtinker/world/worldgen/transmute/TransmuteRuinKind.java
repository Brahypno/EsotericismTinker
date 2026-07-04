package org.brahypno.esotericismtinker.world.worldgen.transmute;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public enum TransmuteRuinKind implements StringRepresentable {
    COMPLETE("complete", 0.00F, 0.12F, 0.00F, 0.00F, 0.00F),
    BROKEN("broken", 0.34F, 0.55F, 0.24F, 0.45F, 0.16F),
    RUINED("ruined", 0.72F, 0.85F, 0.55F, 0.85F, 0.28F);

    public static final Codec<TransmuteRuinKind> CODEC =
            StringRepresentable.fromEnum(TransmuteRuinKind::values);

    private final String name;
    private final float eraseChance;
    private final float functionalReplaceChance;
    private final float controllerAirChance;
    private final float controllerLoseItemsChance;
    private final float unsupportedExtraChance;

    TransmuteRuinKind(
            String name,
            float eraseChance,
            float functionalReplaceChance,
            float controllerAirChance,
            float controllerLoseItemsChance,
            float unsupportedExtraChance
    ) {
        this.name = name;
        this.eraseChance = eraseChance;
        this.functionalReplaceChance = functionalReplaceChance;
        this.controllerAirChance = controllerAirChance;
        this.controllerLoseItemsChance = controllerLoseItemsChance;
        this.unsupportedExtraChance = unsupportedExtraChance;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public float eraseChance() {
        return eraseChance;
    }

    public float functionalReplaceChance() {
        return functionalReplaceChance;
    }

    public float controllerAirChance() {
        return controllerAirChance;
    }

    public float controllerLoseItemsChance() {
        return controllerLoseItemsChance;
    }

    public float unsupportedExtraChance() {
        return unsupportedExtraChance;
    }

    public static TransmuteRuinKind byName(@Nullable String name) {
        for (TransmuteRuinKind kind : values()) {
            if (kind.name.equals(name)){
                return kind;
            }
        }
        return BROKEN;
    }
}