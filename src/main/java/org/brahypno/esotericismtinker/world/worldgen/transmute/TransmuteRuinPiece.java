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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransmuteRuinPiece extends StructurePiece {
    static final ResourceLocation TEMPLATE =
            EsotericismTinker.getLocation("transmute_ruin");

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

    /** Runtime-only, thread-safe index rebuilt lazily after loading the world. */
    @Nullable
    private transient volatile TemplateChunkIndex templateChunkIndex;

    public TransmuteRuinPiece(BlockPos origin, TransmuteRuinKind kind, Rotation rotation, long ruinSeed, StructureTemplate template) {
        super(EsotericismTinkerWorldgenRegistry.TRANSMUTE_RUIN_PIECE.get(), 0, makeBoundingBox(origin, rotation, template));

        this.origin = origin;
        this.kind = kind;
        this.rotation = rotation;
        this.ruinSeed = ruinSeed;
    }

    private boolean hasLegacyOversizedBox() {
        return boundingBox.getXSpan() >= 190
               && boundingBox.getZSpan() >= 190
               && boundingBox.getYSpan() >= 120;
    }

    public TransmuteRuinPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(EsotericismTinkerWorldgenRegistry.TRANSMUTE_RUIN_PIECE.get(), tag);

        this.origin = new BlockPos(tag.getInt(TAG_ORIGIN_X), tag.getInt(TAG_ORIGIN_Y), tag.getInt(TAG_ORIGIN_Z));

        this.kind = TransmuteRuinKind.byName(tag.getString(TAG_KIND));
        this.rotation = readRotation(tag);
        this.ruinSeed = tag.getLong(TAG_RUIN_SEED);

        StructureTemplate template = context.structureTemplateManager()
                                            .getOrCreate(TEMPLATE);

        if (hasLegacyOversizedBox()){
            this.boundingBox = makeBoundingBox(origin, rotation, template);
        }
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
    public void postProcess(WorldGenLevel level, @NotNull StructureManager structureManager, @NotNull ChunkGenerator chunkGenerator, @NotNull RandomSource random, @NotNull BoundingBox box, @NotNull ChunkPos chunkPos, @NotNull BlockPos pivot) {
        StructureTemplateManager templateManager = level.getLevel().getStructureManager();

        StructureTemplate template = templateManager.getOrCreate(TEMPLATE);

        if (!isValidTemplate(template)){
            return;
        }

        Vec3i size = template.getSize();
        BlockPos placePos = calculatePlacePos(origin, size);

        StructurePlaceSettings settings = createSettings(rotation, size)
                .setIgnoreEntities(true)
                .setFinalizeEntities(true)
                .setBoundingBox(box)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .addProcessor(new TransmuteStructureStateProcessor(rotation, Direction.EAST));

        BoundingBox placedBox = template.getBoundingBox(settings, placePos);

        template.placeInWorld(level, placePos, placePos, settings, random, Block.UPDATE_CLIENTS);

        TemplateChunkIndex index = getOrCreateChunkIndex(template, settings, placePos);
        long chunkKey = chunkPos.toLong();
        Set<BlockPos> templateBlocks = index.blocksByChunk().getOrDefault(chunkKey, Set.of());
        List<StructureTemplate.StructureEntityInfo> templateEntities = index.entitiesByChunk().getOrDefault(chunkKey, List.of());

        TransmuteRuinPostProcessor.process(level, placedBox, box, random, kind, ruinSeed, templateBlocks, templateEntities, settings, placePos);
    }

    private TemplateChunkIndex getOrCreateChunkIndex(StructureTemplate template, StructurePlaceSettings settings, BlockPos placePos) {
        TemplateChunkIndex existing = templateChunkIndex;
        if (existing != null){
            return existing;
        }

        synchronized (this) {
            existing = templateChunkIndex;
            if (existing == null){
                existing = buildTemplateChunkIndex(template, settings, placePos);
                templateChunkIndex = existing;
            }
        }

        return existing;
    }

    private static TemplateChunkIndex buildTemplateChunkIndex(StructureTemplate template, StructurePlaceSettings settings, BlockPos placePos) {
        Map<Long, Set<BlockPos>> blocksByChunk = new HashMap<>();
        Map<Long, List<StructureTemplate.StructureEntityInfo>> entitiesByChunk = new HashMap<>();

        if (!template.palettes.isEmpty()){
            StructureTemplate.Palette palette = settings.getRandomPalette(template.palettes, placePos);

            for (StructureTemplate.StructureBlockInfo info : palette.blocks()) {
                if (!isRealTemplateBlock(info.state())){
                    continue;
                }

                BlockPos worldPos = StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(placePos).immutable();
                long chunkKey = ChunkPos.asLong(worldPos.getX() >> 4, worldPos.getZ() >> 4);
                blocksByChunk.computeIfAbsent(chunkKey, ignored -> new HashSet<>()).add(worldPos);
            }
        }

        for (StructureTemplate.StructureEntityInfo info : template.entityInfoList) {
            BlockPos worldTilePos = StructureTemplate.calculateRelativePosition(settings, info.blockPos).offset(placePos);
            long chunkKey = ChunkPos.asLong(worldTilePos.getX() >> 4, worldTilePos.getZ() >> 4);

            entitiesByChunk.computeIfAbsent(chunkKey, ignored -> new ArrayList<>()).add(info);
        }

        return new TemplateChunkIndex(blocksByChunk, entitiesByChunk);
    }

    private static boolean isRealTemplateBlock(BlockState state) {
        return !state.isAir()
               && !state.is(Blocks.STRUCTURE_VOID)
               && !state.is(Blocks.STRUCTURE_BLOCK)
               && !state.is(Blocks.JIGSAW);
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

    private static BoundingBox makeBoundingBox(BlockPos origin, Rotation rotation, StructureTemplate template) {
        if (!isValidTemplate(template)){
            return new BoundingBox(origin.getX(), origin.getY(), origin.getZ(), origin.getX(), origin.getY(), origin.getZ());
        }

        Vec3i size = template.getSize();
        StructurePlaceSettings settings = createSettings(rotation, size);

        return template.getBoundingBox(settings, calculatePlacePos(origin, size));
    }

    static boolean isValidTemplate(StructureTemplate template) {
        if (template == null){
            return false;
        }

        Vec3i size = template.getSize();

        return size.getX() > 0
               && size.getY() > 0
               && size.getZ() > 0
               && !template.palettes.isEmpty();
    }

    private static StructurePlaceSettings createSettings(Rotation rotation, Vec3i size) {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2));
    }

    private static BlockPos calculatePlacePos(BlockPos origin, Vec3i size) {
        return origin.offset(-size.getX() / 2, -1, -size.getZ() / 2);
    }

    private record TemplateChunkIndex(Map<Long, Set<BlockPos>> blocksByChunk, Map<Long, List<StructureTemplate.StructureEntityInfo>> entitiesByChunk) {}
}
