package org.brahypno.esotericismtinker.world.worldgen;

import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinkerModule;
import org.brahypno.esotericismtinker.world.worldgen.selenic.SelenicAstrolabeRuinPiece;
import org.brahypno.esotericismtinker.world.worldgen.selenic.SelenicAstrolabeRuinStructure;

public class EsotericismTinkerWorldgenRegistry extends EsotericismTinkerModule {

    public static final RegistryObject<StructureType<SelenicAstrolabeRuinStructure>> SELENIC_ASTROLABE_RUIN_STRUCTURE =
            STRUCTURE_TYPES.register(
                    "selenic_astrolabe_ruin",
                    () -> () -> SelenicAstrolabeRuinStructure.CODEC
            );

    public static final RegistryObject<StructurePieceType> SELENIC_ASTROLABE_RUIN_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "selenic_astrolabe_ruin",
                    () -> SelenicAstrolabeRuinPiece::new
            );
}