package org.brahypno.esotericismtinker.world.worldgen;

import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinkerModule;
import org.brahypno.esotericismtinker.world.worldgen.selenic.SelenicAstrolabeRuinPiece;
import org.brahypno.esotericismtinker.world.worldgen.selenic.SelenicAstrolabeRuinStructure;
import org.brahypno.esotericismtinker.world.worldgen.transmute.TransmuteRuinPiece;
import org.brahypno.esotericismtinker.world.worldgen.transmute.TransmuteRuinStructure;

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
    public static final RegistryObject<StructureType<TransmuteRuinStructure>> TRANSMUTE_RUIN_STRUCTURE =
            STRUCTURE_TYPES.register("transmute_ruin", () -> () -> TransmuteRuinStructure.CODEC);

    public static final RegistryObject<StructurePieceType> TRANSMUTE_RUIN_PIECE =
            STRUCTURE_PIECE_TYPES.register("transmute_ruin", () -> TransmuteRuinPiece::new);
}