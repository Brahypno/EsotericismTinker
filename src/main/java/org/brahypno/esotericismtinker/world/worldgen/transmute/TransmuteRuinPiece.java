package org.brahypno.esotericismtinker.world.worldgen.transmute;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.world.worldgen.EsotericismTinkerWorldgenRegistry;
import org.jetbrains.annotations.NotNull;

public class TransmuteRuinPiece extends StructurePiece {
    private static final ResourceLocation TEMPLATE = EsotericismTinker.getLocation("transmute_ruin");

    private static final String TAG_ORIGIN_X = "OriginX";
    private static final String TAG_ORIGIN_Y = "OriginY";
    private static final String TAG_ORIGIN_Z = "OriginZ";
    private static final String TAG_KIND = "Kind";
    private static final String TAG_ROTATION = "Rotation";
    private static final String TAG_RUIN_SEED = "RuinSeed";

    private final BlockPos origin;
    private final TransmuteRuinKind kind;
    private final Rotation rotation;
    private final long ruinSeed;

    public TransmuteRuinPiece(
            BlockPos origin,
            TransmuteRuinKind kind,
            Rotation rotation,
            long ruinSeed
    ) {
        super(
                EsotericismTinkerWorldgenRegistry.TRANSMUTE_RUIN_PIECE.get(),
                0,
                makeBoundingBox(origin)
        );
        this.origin = origin;
        this.kind = kind;
        this.rotation = rotation;
        this.ruinSeed = ruinSeed;
    }

    public TransmuteRuinPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(EsotericismTinkerWorldgenRegistry.TRANSMUTE_RUIN_PIECE.get(), tag);
        this.origin = new BlockPos(
                tag.getInt(TAG_ORIGIN_X),
                tag.getInt(TAG_ORIGIN_Y),
                tag.getInt(TAG_ORIGIN_Z)
        );
        this.kind = TransmuteRuinKind.byName(tag.getString(TAG_KIND));
        this.rotation = readRotation(tag);
        this.ruinSeed = tag.getLong(TAG_RUIN_SEED);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt(TAG_ORIGIN_X, origin.getX());
        tag.putInt(TAG_ORIGIN_Y, origin.getY());
        tag.putInt(TAG_ORIGIN_Z, origin.getZ());
        tag.putString(TAG_KIND, kind.getSerializedName());
        tag.putString(TAG_ROTATION, rotation.name());
        tag.putLong(TAG_RUIN_SEED, ruinSeed);
    }

    @Override
    public void postProcess(
            WorldGenLevel level, @NotNull StructureManager structureManager,
            @NotNull ChunkGenerator chunkGenerator,
            @NotNull RandomSource random,
            @NotNull BoundingBox box,
            @NotNull ChunkPos chunkPos,
            @NotNull BlockPos pivot
    ) {
        StructureTemplateManager templateManager = level.getLevel().getStructureManager();
        StructureTemplate template = templateManager.getOrCreate(TEMPLATE);
        Vec3i size = template.getSize();

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2))
                .setIgnoreEntities(true)
                .setFinalizeEntities(true)
                .setBoundingBox(box)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .addProcessor(new TransmuteStructureStateProcessor(rotation, Direction.EAST));

        BlockPos placePos = origin.offset(
                -size.getX() / 2,
                -1,
                -size.getZ() / 2
        );

        BoundingBox placedBox = template.getBoundingBox(settings, placePos);

        template.placeInWorld(level, placePos, placePos, settings, random, Block.UPDATE_CLIENTS);

        TransmuteRuinPostProcessor.process(level, placedBox, box, random, kind, ruinSeed, template, settings, placePos);
    }

    private static Rotation readRotation(CompoundTag tag) {
        if (!tag.contains(TAG_ROTATION)){
            return Rotation.NONE;
        }

        try {
            return Rotation.valueOf(tag.getString(TAG_ROTATION));
        }
        catch (IllegalArgumentException ignored) {
            return Rotation.NONE;
        }
    }

    private static BoundingBox makeBoundingBox(BlockPos origin) {
        int radius = 96;
        return new BoundingBox(
                origin.getX() - radius,
                origin.getY() - 32,
                origin.getZ() - radius,
                origin.getX() + radius,
                origin.getY() + 96,
                origin.getZ() + radius
        );
    }
}