package org.brahypno.esotericismtinker.world.worldgen.selenic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SelenicAstrolabeRuinConfiguration(
        Variant variant,
        Structure structure,
        Placement placement,
        Rewards rewards
) {
    public static final Codec<SelenicAstrolabeRuinConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Variant.CODEC.fieldOf("variant")
                                 .forGetter(SelenicAstrolabeRuinConfiguration::variant),
                    Structure.CODEC.fieldOf("structure")
                                   .forGetter(SelenicAstrolabeRuinConfiguration::structure),
                    Placement.CODEC.fieldOf("placement")
                                   .forGetter(SelenicAstrolabeRuinConfiguration::placement),
                    Rewards.CODEC.fieldOf("rewards")
                                 .forGetter(SelenicAstrolabeRuinConfiguration::rewards)
            ).apply(instance, SelenicAstrolabeRuinConfiguration::new));

    public record Variant(
            float completeChance,
            float crownChance,
            float fontChance,
            float testimonyChance,
            float noiseInfluence
    ) {
        public static final Codec<Variant> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.FLOAT.fieldOf("complete_chance").forGetter(Variant::completeChance),
                        Codec.FLOAT.fieldOf("crown_chance").forGetter(Variant::crownChance),
                        Codec.FLOAT.fieldOf("font_chance").forGetter(Variant::fontChance),
                        Codec.FLOAT.fieldOf("testimony_chance").forGetter(Variant::testimonyChance),
                        Codec.FLOAT.optionalFieldOf("noise_influence", 0.25F).forGetter(Variant::noiseInfluence)
                ).apply(instance, Variant::new));
    }

    public record Structure(
            int minSpinesBelow,
            int maxSpinesBelow,
            int minSpinesAbove,
            int maxSpinesAbove,
            int minStands,
            int maxStands
    ) {
        public static final Codec<Structure> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.INT.fieldOf("min_spines_below").forGetter(Structure::minSpinesBelow),
                        Codec.INT.fieldOf("max_spines_below").forGetter(Structure::maxSpinesBelow),
                        Codec.INT.fieldOf("min_spines_above").forGetter(Structure::minSpinesAbove),
                        Codec.INT.fieldOf("max_spines_above").forGetter(Structure::maxSpinesAbove),
                        Codec.INT.fieldOf("min_stands").forGetter(Structure::minStands),
                        Codec.INT.fieldOf("max_stands").forGetter(Structure::maxStands)
                ).apply(instance, Structure::new));
    }

    public record Placement(
            int minY,
            int maxY,
            int reserveRadius,
            int reserveHeightExtra
    ) {
        public static final Codec<Placement> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.INT.optionalFieldOf("min_y", 80).forGetter(Placement::minY),
                        Codec.INT.optionalFieldOf("max_y", 256).forGetter(Placement::maxY),
                        Codec.INT.optionalFieldOf("reserve_radius", 2).forGetter(Placement::reserveRadius),
                        Codec.INT.optionalFieldOf("reserve_height_extra", 16).forGetter(Placement::reserveHeightExtra)
                ).apply(instance, Placement::new));
    }

    public record Rewards(
            float lootChestChance,
            int minLootChests,
            int maxLootChests,
            int lootChestRadius,
            float netheriteBlockChance,
            int netheriteSearchDepth
    ) {
        public static final Codec<Rewards> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.FLOAT.optionalFieldOf("loot_chest_chance", 0.35F).forGetter(Rewards::lootChestChance),
                        Codec.INT.optionalFieldOf("min_loot_chests", 0).forGetter(Rewards::minLootChests),
                        Codec.INT.optionalFieldOf("max_loot_chests", 2).forGetter(Rewards::maxLootChests),
                        Codec.INT.optionalFieldOf("loot_chest_radius", 6).forGetter(Rewards::lootChestRadius),
                        Codec.FLOAT.optionalFieldOf("netherite_block_chance", 0.04F).forGetter(Rewards::netheriteBlockChance),
                        Codec.INT.optionalFieldOf("netherite_search_depth", 120).forGetter(Rewards::netheriteSearchDepth)
                ).apply(instance, Rewards::new));
    }
}