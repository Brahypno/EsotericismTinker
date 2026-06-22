package org.brahypno.esotericismtinker.world.worldgen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.world.worldgen.selenic.SelenicAstrolabeRuinStructures;

public final class EsotericismTinkerWorldGen {
    private EsotericismTinkerWorldGen() {}

    public static RegistrySetBuilder build() {
        return new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, EsotericismTinkerWorldGen::bootstrapConfigured)
                .add(Registries.PLACED_FEATURE, EsotericismTinkerWorldGen::bootstrapPlaced)
                .add(ForgeRegistries.Keys.BIOME_MODIFIERS, EsotericismTinkerBiomeModifiers::bootstrap)
                .add(Registries.STRUCTURE, EsotericismTinkerWorldGen::bootstrapStructures)
                .add(Registries.STRUCTURE_SET, EsotericismTinkerWorldGen::bootstrapStructureSets);
    }

    public static void bootstrapConfigured(BootstapContext<ConfiguredFeature<?, ?>> ctx) {

        // Future:
        // BWorldGen.bootstrapConfigured(ctx);
        // CWorldGen.bootstrapConfigured(ctx);
    }

    public static void bootstrapPlaced(BootstapContext<PlacedFeature> ctx) {

        // Future:
        // BWorldGen.bootstrapPlaced(ctx);
        // CWorldGen.bootstrapPlaced(ctx);
    }

    public static void bootstrapStructures(BootstapContext<Structure> ctx) {
        SelenicAstrolabeRuinStructures.bootstrapStructures(ctx);
    }

    public static void bootstrapStructureSets(BootstapContext<StructureSet> ctx) {
        SelenicAstrolabeRuinStructures.bootstrapStructureSets(ctx);
    }
}