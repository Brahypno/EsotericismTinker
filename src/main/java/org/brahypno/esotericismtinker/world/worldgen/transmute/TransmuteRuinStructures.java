package org.brahypno.esotericismtinker.world.worldgen.transmute;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.brahypno.esotericismtinker.EsotericismTinker;

import java.util.List;
import java.util.Map;

public final class TransmuteRuinStructures {
    public static final ResourceKey<Structure> COMPLETE = structureKey("transmute_ruin_complete");
    public static final ResourceKey<Structure> BROKEN = structureKey("transmute_ruin_broken");
    public static final ResourceKey<Structure> RUINED = structureKey("transmute_ruin_ruined");

    public static final ResourceKey<StructureSet> SET = ResourceKey.create(
            Registries.STRUCTURE_SET,
            EsotericismTinker.getLocation("transmute_ruins")
    );

    private TransmuteRuinStructures() {}

    public static void bootstrapStructures(BootstapContext<Structure> ctx) {
        HolderSet<Biome> biomes = ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.HAS_END_CITY);

        Structure.StructureSettings settings = new Structure.StructureSettings(
                biomes,
                Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE
        );

        ctx.register(COMPLETE, new TransmuteRuinStructure(settings, TransmuteRuinKind.COMPLETE));
        ctx.register(BROKEN, new TransmuteRuinStructure(settings, TransmuteRuinKind.BROKEN));
        ctx.register(RUINED, new TransmuteRuinStructure(settings, TransmuteRuinKind.RUINED));
    }

    public static void bootstrapStructureSets(BootstapContext<StructureSet> ctx) {
        HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);

        ctx.register(
                SET,
                new StructureSet(
                        List.of(
                                StructureSet.entry(structures.getOrThrow(COMPLETE), 1),
                                StructureSet.entry(structures.getOrThrow(BROKEN), 4),
                                StructureSet.entry(structures.getOrThrow(RUINED), 3)
                        ),
                        new RandomSpreadStructurePlacement(
                                40,
                                12,
                                RandomSpreadType.LINEAR,
                                709957548
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