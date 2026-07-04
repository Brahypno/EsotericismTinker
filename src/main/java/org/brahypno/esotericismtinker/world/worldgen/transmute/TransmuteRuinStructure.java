package org.brahypno.esotericismtinker.world.worldgen.transmute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.brahypno.esotericismtinker.Config;
import org.brahypno.esotericismtinker.world.worldgen.EsotericismTinkerWorldgenRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TransmuteRuinStructure extends Structure {
    private static final int MIN_DISTANCE_FROM_END_CENTER = 1024;
    private static final int MIN_END_SURFACE_Y = 48;
    private static final int FOOTPRINT_RADIUS = 20;
    private static final int MAX_SURFACE_DELTA = 5;

    public static final Codec<TransmuteRuinStructure> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    settingsCodec(instance),
                    TransmuteRuinKind.CODEC.fieldOf("kind")
                                           .forGetter(TransmuteRuinStructure::kind)
            ).apply(instance, TransmuteRuinStructure::new));

    private final TransmuteRuinKind kind;

    public TransmuteRuinStructure(StructureSettings settings, TransmuteRuinKind kind) {
        super(settings);
        this.kind = kind;
    }

    public TransmuteRuinKind kind() {
        return kind;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        if (!Config.GENERATE_TRANSMUTE_RUINS.get()){
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();

        if (!isOutsideEndMainIsland(x, z)){
            return Optional.empty();
        }

        Optional<Integer> y = findStableSurfaceY(context, x, z);
        if (y.isEmpty()){
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x, y.get(), z);
        Rotation rotation = Rotation.getRandom(context.random());
        long ruinSeed = context.random().nextLong();

        return Optional.of(new GenerationStub(
                origin,
                builder -> builder.addPiece(new TransmuteRuinPiece(origin, kind, rotation, ruinSeed))
        ));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return EsotericismTinkerWorldgenRegistry.TRANSMUTE_RUIN_STRUCTURE.get();
    }

    private static boolean isOutsideEndMainIsland(int x, int z) {
        long minDistance = MIN_DISTANCE_FROM_END_CENTER;
        return (long) x * (long) x + (long) z * (long) z >= minDistance * minDistance;
    }

    private static Optional<Integer> findStableSurfaceY(
            GenerationContext context,
            int centerX,
            int centerZ
    ) {
        int[][] offsets = {
                {0, 0},
                {FOOTPRINT_RADIUS, 0},
                {-FOOTPRINT_RADIUS, 0},
                {0, FOOTPRINT_RADIUS},
                {0, -FOOTPRINT_RADIUS},
                {FOOTPRINT_RADIUS, FOOTPRINT_RADIUS},
                {FOOTPRINT_RADIUS, -FOOTPRINT_RADIUS},
                {-FOOTPRINT_RADIUS, FOOTPRINT_RADIUS},
                {-FOOTPRINT_RADIUS, -FOOTPRINT_RADIUS},
                {FOOTPRINT_RADIUS / 2, FOOTPRINT_RADIUS / 2},
                {FOOTPRINT_RADIUS / 2, -FOOTPRINT_RADIUS / 2},
                {-FOOTPRINT_RADIUS / 2, FOOTPRINT_RADIUS / 2},
                {-FOOTPRINT_RADIUS / 2, -FOOTPRINT_RADIUS / 2}
        };

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int[] offset : offsets) {
            int y = context.chunkGenerator().getBaseHeight(
                    centerX + offset[0],
                    centerZ + offset[1],
                    Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(),
                    context.randomState()
            );

            if (y < MIN_END_SURFACE_Y){
                return Optional.empty();
            }

            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        if (maxY - minY > MAX_SURFACE_DELTA){
            return Optional.empty();
        }

        return Optional.of(minY);
    }
}