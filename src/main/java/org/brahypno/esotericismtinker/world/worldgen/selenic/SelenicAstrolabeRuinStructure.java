package org.brahypno.esotericismtinker.world.worldgen.selenic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.brahypno.esotericismtinker.world.worldgen.EsotericismTinkerWorldgenRegistry;

import java.util.Optional;

public class SelenicAstrolabeRuinStructure extends Structure {
    public static final Codec<SelenicAstrolabeRuinStructure> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    settingsCodec(instance),
                    SelenicAstrolabeRuinKind.CODEC.fieldOf("kind")
                                                  .forGetter(SelenicAstrolabeRuinStructure::kind)
            ).apply(instance, SelenicAstrolabeRuinStructure::new));

    private final SelenicAstrolabeRuinKind kind;

    public SelenicAstrolabeRuinStructure(
            StructureSettings settings,
            SelenicAstrolabeRuinKind kind
    ) {
        super(settings);
        this.kind = kind;
    }

    public SelenicAstrolabeRuinKind kind() {
        return kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();

        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();

        int y = context.chunkGenerator().getBaseHeight(
                x,
                z,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );

        BlockPos origin = new BlockPos(x, y, z);
        SelenicAstrolabeRuinConfiguration config = kind.config();

        if (!SelenicAstrolabeRuinPlacer.isInHeightRange(origin, config)){
            return Optional.empty();
        }

        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new SelenicAstrolabeRuinPiece(origin, kind))));
    }

    @Override
    public StructureType<?> type() {
        return EsotericismTinkerWorldgenRegistry.SELENIC_ASTROLABE_RUIN_STRUCTURE.get();
    }
}