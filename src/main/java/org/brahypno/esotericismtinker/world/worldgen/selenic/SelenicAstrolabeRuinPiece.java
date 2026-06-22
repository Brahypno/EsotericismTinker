package org.brahypno.esotericismtinker.world.worldgen.selenic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.brahypno.esotericismtinker.world.worldgen.EsotericismTinkerWorldgenRegistry;

public class SelenicAstrolabeRuinPiece extends StructurePiece {
    private static final String TAG_ORIGIN_X = "OriginX";
    private static final String TAG_ORIGIN_Y = "OriginY";
    private static final String TAG_ORIGIN_Z = "OriginZ";
    private static final String TAG_KIND = "Kind";

    private final BlockPos origin;
    private final SelenicAstrolabeRuinKind kind;

    public SelenicAstrolabeRuinPiece(
            BlockPos origin,
            SelenicAstrolabeRuinKind kind
    ) {
        super(
                EsotericismTinkerWorldgenRegistry.SELENIC_ASTROLABE_RUIN_PIECE.get(),
                0,
                makeBoundingBox(origin, kind.config())
        );

        this.origin = origin;
        this.kind = kind;
    }

    public SelenicAstrolabeRuinPiece(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        super(
                EsotericismTinkerWorldgenRegistry.SELENIC_ASTROLABE_RUIN_PIECE.get(),
                tag
        );

        this.origin = new BlockPos(
                tag.getInt(TAG_ORIGIN_X),
                tag.getInt(TAG_ORIGIN_Y),
                tag.getInt(TAG_ORIGIN_Z)
        );

        this.kind = SelenicAstrolabeRuinKind.byName(tag.getString(TAG_KIND));
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        tag.putInt(TAG_ORIGIN_X, origin.getX());
        tag.putInt(TAG_ORIGIN_Y, origin.getY());
        tag.putInt(TAG_ORIGIN_Z, origin.getZ());
        tag.putString(TAG_KIND, kind.getSerializedName());
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        SelenicAstrolabeRuinPlacer.place(level, origin, random, kind.config());
    }

    private static BoundingBox makeBoundingBox(
            BlockPos origin,
            SelenicAstrolabeRuinConfiguration config
    ) {
        int radius = Math.max(
                config.placement().reserveRadius() + 8,
                config.rewards().lootChestRadius() + 2
        );

        int minY = origin.getY() - 4;
        int maxY = origin.getY()
                   + config.structure().maxSpinesBelow()
                   + config.structure().maxSpinesAbove()
                   + config.placement().reserveHeightExtra()
                   + 10;

        return new BoundingBox(
                origin.getX() - radius,
                minY,
                origin.getZ() - radius,
                origin.getX() + radius,
                maxY,
                origin.getZ() + radius
        );
    }
}