package org.brahypno.esotericismtinker.world.worldgen.transmute;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class TransmuteStructureStateProcessor extends StructureProcessor {
    private static final String MOD_ID = "esotericism_tinker";

    private final Direction fixedFacing;

    public TransmuteStructureStateProcessor(Rotation rotation, Direction templateFacing) {
        this.fixedFacing = rotation.rotate(templateFacing);
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureTemplate.StructureBlockInfo rawBlockInfo, StructureTemplate.StructureBlockInfo blockInfo, StructurePlaceSettings settings) {
        BlockState placedState = blockInfo.state();
        DirectionProperty facing = findHorizontalFacingProperty(placedState);

        if (facing == null){
            return blockInfo;
        }

        return new StructureTemplate.StructureBlockInfo(blockInfo.pos(), placedState.setValue(facing, fixedFacing), blockInfo.nbt());
    }

    @Override
    protected @NotNull StructureProcessorType<?> getType() {
        return StructureProcessorType.BLOCK_IGNORE;
    }

    @Nullable
    private static DirectionProperty findHorizontalFacingProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty directionProperty && property.getName().equals("facing") &&
                state.getValue(directionProperty).getAxis().isHorizontal()){
                return directionProperty;
            }
        }
        return null;
    }
}