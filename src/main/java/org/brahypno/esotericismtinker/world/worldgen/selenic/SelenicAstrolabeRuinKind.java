package org.brahypno.esotericismtinker.world.worldgen.selenic;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;

public enum SelenicAstrolabeRuinKind implements StringRepresentable {
    BROKEN("broken"),
    COMPLETE("complete"),
    TOWER("tower");

    public static final Codec<SelenicAstrolabeRuinKind> CODEC =
            StringRepresentable.fromEnum(SelenicAstrolabeRuinKind::values);

    private final String name;

    SelenicAstrolabeRuinKind(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static SelenicAstrolabeRuinKind byName(@Nullable String name) {
        for (SelenicAstrolabeRuinKind kind : values()) {
            if (kind.name.equals(name)){
                return kind;
            }
        }

        return BROKEN;
    }

    public SelenicAstrolabeRuinConfiguration config() {
        return switch (this) {
            case BROKEN -> new SelenicAstrolabeRuinConfiguration(
                    new SelenicAstrolabeRuinConfiguration.Variant(
                            0.05F,
                            0.35F,
                            0.55F,
                            0.45F,
                            0.35F
                    ),
                    new SelenicAstrolabeRuinConfiguration.Structure(
                            0,
                            2,
                            0,
                            4,
                            0,
                            4
                    ),
                    new SelenicAstrolabeRuinConfiguration.Placement(
                            80,
                            176,
                            4,
                            2
                    ),
                    new SelenicAstrolabeRuinConfiguration.Rewards(
                            0.25F,
                            1,
                            4,
                            6,
                            0.4F,
                            120
                    )
            );

            case COMPLETE -> new SelenicAstrolabeRuinConfiguration(
                    new SelenicAstrolabeRuinConfiguration.Variant(
                            0.95F,
                            1.0F,
                            1.0F,
                            0.85F,
                            0.20F
                    ),
                    new SelenicAstrolabeRuinConfiguration.Structure(
                            1,
                            3,
                            1,
                            5,
                            3,
                            8
                    ),
                    new SelenicAstrolabeRuinConfiguration.Placement(
                            112,
                            224,
                            5,
                            3
                    ),
                    new SelenicAstrolabeRuinConfiguration.Rewards(
                            0.45F,
                            3,
                            6,
                            7,
                            0.6F,
                            160
                    )
            );

            case TOWER -> new SelenicAstrolabeRuinConfiguration(
                    new SelenicAstrolabeRuinConfiguration.Variant(
                            0.40F,
                            0.80F,
                            0.85F,
                            0.65F,
                            0.45F
                    ),
                    new SelenicAstrolabeRuinConfiguration.Structure(
                            1,
                            4,
                            4,
                            9,
                            1,
                            6
                    ),
                    new SelenicAstrolabeRuinConfiguration.Placement(
                            144,
                            256,
                            5,
                            4
                    ),
                    new SelenicAstrolabeRuinConfiguration.Rewards(
                            0.35F,
                            0,
                            8,
                            8,
                            0.8F,
                            180
                    )
            );
        };
    }
}