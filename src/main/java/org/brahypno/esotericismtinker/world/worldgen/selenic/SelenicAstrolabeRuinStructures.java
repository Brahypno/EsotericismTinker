package org.brahypno.esotericismtinker.world.worldgen.selenic;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraftforge.common.Tags;
import org.brahypno.esotericismtinker.EsotericismTinker;

import java.util.List;
import java.util.Map;

public final class SelenicAstrolabeRuinStructures {
    public static final ResourceKey<Structure> BROKEN =
            structureKey("selenic_astrolabe_ruin_broken");

    public static final ResourceKey<Structure> COMPLETE =
            structureKey("selenic_astrolabe_ruin_complete");

    public static final ResourceKey<Structure> TOWER =
            structureKey("selenic_astrolabe_ruin_tower");

    public static final ResourceKey<StructureSet> SET =
            ResourceKey.create(
                    Registries.STRUCTURE_SET,
                    EsotericismTinker.getLocation("selenic_astrolabe_ruins")
            );

    private SelenicAstrolabeRuinStructures() {}

    public static void bootstrapStructures(BootstapContext<Structure> ctx) {
        HolderSet<Biome> biomes = ctx.lookup(Registries.BIOME).getOrThrow(Tags.Biomes.IS_MOUNTAIN);

        Structure.StructureSettings settings = new Structure.StructureSettings(
                biomes,
                Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE
        );

        ctx.register(
                BROKEN,
                new SelenicAstrolabeRuinStructure(settings, SelenicAstrolabeRuinKind.BROKEN)
        );

        ctx.register(
                COMPLETE,
                new SelenicAstrolabeRuinStructure(settings, SelenicAstrolabeRuinKind.COMPLETE)
        );

        ctx.register(
                TOWER,
                new SelenicAstrolabeRuinStructure(settings, SelenicAstrolabeRuinKind.TOWER)
        );
    }

    public static void bootstrapStructureSets(BootstapContext<StructureSet> ctx) {
        HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);

        ctx.register(
                SET,
                new StructureSet(
                        List.of(
                                StructureSet.entry(structures.getOrThrow(BROKEN), 6),
                                StructureSet.entry(structures.getOrThrow(COMPLETE), 2),
                                StructureSet.entry(structures.getOrThrow(TOWER), 1)
                        ),
                        new RandomSpreadStructurePlacement(
                                36,
                                12,
                                RandomSpreadType.LINEAR,
                                927154386
                        )
                )
        );
    }

    private static ResourceKey<Structure> structureKey(String name) {
        return ResourceKey.create(
                Registries.STRUCTURE,
                EsotericismTinker.getLocation(name)
        );
    }
}