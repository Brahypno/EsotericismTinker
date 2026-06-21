package org.brahypno.esotericismtinker.selenic.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;

public final class SelenicBlockStates {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty SIGNALING = BooleanProperty.create("signaling");

    private SelenicBlockStates() {}

    public static boolean hasHalf(BlockState state, EnumProperty<DoubleBlockHalf> halfProperty) {
        return state.hasProperty(halfProperty);
    }

    public static boolean isLower(BlockState state) {
        return isLower(state, HALF);
    }

    public static boolean isLower(BlockState state, EnumProperty<DoubleBlockHalf> halfProperty) {
        return hasHalf(state, halfProperty) && state.getValue(halfProperty) == DoubleBlockHalf.LOWER;
    }

    public static boolean isUpper(BlockState state, EnumProperty<DoubleBlockHalf> halfProperty) {
        return hasHalf(state, halfProperty) && state.getValue(halfProperty) == DoubleBlockHalf.UPPER;
    }

    public static BlockPos lowerPos(BlockPos pos, BlockState state) {
        return lowerPos(pos, state, HALF);
    }

    public static BlockPos lowerPos(BlockPos pos, BlockState state, EnumProperty<DoubleBlockHalf> halfProperty) {
        if (!hasHalf(state, halfProperty)){
            return pos;
        }

        return isUpper(state, halfProperty) ? pos.below() : pos;
    }

    public static BlockPos upperPos(BlockPos pos, BlockState state) {
        return upperPos(pos, state, HALF);
    }

    public static BlockPos upperPos(BlockPos pos, BlockState state, EnumProperty<DoubleBlockHalf> halfProperty) {
        if (!hasHalf(state, halfProperty)){
            return pos.above();
        }

        return isLower(state, halfProperty) ? pos.above() : pos;
    }

    public static BlockPos otherHalfPos(BlockPos pos, BlockState state, EnumProperty<DoubleBlockHalf> halfProperty) {
        return isLower(state, halfProperty) ? pos.above() : pos.below();
    }

    public static boolean isMatchingOtherHalf(BlockState state, BlockState otherState) {
        return isMatchingOtherHalf(state, otherState, HALF);
    }

    public static boolean isMatchingOtherHalf(
            BlockState state,
            BlockState otherState,
            EnumProperty<DoubleBlockHalf> halfProperty) {
        if (!state.hasProperty(halfProperty) || !otherState.hasProperty(halfProperty)){
            return false;
        }

        return otherState.is(state.getBlock())
               && state.getValue(halfProperty) != otherState.getValue(halfProperty);
    }

    @Nullable
    public static <T extends BlockEntity> T getLowerBlockEntity(
            Level level,
            BlockPos pos,
            BlockState state,
            Class<T> type) {
        BlockPos lowerPos = lowerPos(pos, state);
        BlockEntity blockEntity = level.getBlockEntity(lowerPos);

        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }

    public static void setDoubleSignaling(Level level, BlockPos pos, boolean signaling) {
        setDoubleBoolean(level, pos, SIGNALING, signaling);
    }

    public static void setDoubleBoolean(Level level, BlockPos pos, BooleanProperty property, boolean value) {
        setDoubleBoolean(level, pos, HALF, property, value);
    }

    public static void setDoubleActive(
            Level level,
            BlockPos pos,
            EnumProperty<DoubleBlockHalf> halfProperty,
            BooleanProperty activeProperty,
            boolean active) {
        setDoubleBoolean(level, pos, halfProperty, activeProperty, active);
    }

    public static void setDoubleBoolean(
            Level level,
            BlockPos pos,
            EnumProperty<DoubleBlockHalf> halfProperty,
            BooleanProperty property,
            boolean value) {
        BlockState state = level.getBlockState(pos);

        if (!state.hasProperty(property)){
            return;
        }

        if (!state.hasProperty(halfProperty)){
            setBoolean(level, pos, property, value);
            return;
        }

        BlockPos lowerPos = lowerPos(pos, state, halfProperty);
        BlockPos upperPos = lowerPos.above();
        Block block = state.getBlock();

        setBooleanIfSameBlock(level, lowerPos, block, property, value);
        setBooleanIfSameBlock(level, upperPos, block, property, value);
    }

    public static void removeOtherHalf(Level level, BlockPos pos, BlockState state) {
        removeOtherHalf(level, pos, state, HALF);
    }

    public static void removeOtherHalf(
            Level level,
            BlockPos pos,
            BlockState state,
            EnumProperty<DoubleBlockHalf> halfProperty) {
        if (!state.hasProperty(halfProperty)){
            return;
        }

        BlockPos otherPos = otherHalfPos(pos, state, halfProperty);
        BlockState otherState = level.getBlockState(otherPos);

        if (!isMatchingOtherHalf(state, otherState, halfProperty)){
            return;
        }

        level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void setBoolean(Level level, BlockPos pos, BooleanProperty property, boolean value) {
        BlockState state = level.getBlockState(pos);

        if (!state.hasProperty(property)){
            return;
        }

        if (state.getValue(property) == value){
            return;
        }

        level.setBlock(pos, state.setValue(property, value), Block.UPDATE_ALL);
    }

    private static void setBooleanIfSameBlock(
            Level level,
            BlockPos pos,
            Block block,
            BooleanProperty property,
            boolean value) {
        BlockState state = level.getBlockState(pos);

        if (!state.is(block) || !state.hasProperty(property)){
            return;
        }

        if (state.getValue(property) == value){
            return;
        }

        level.setBlock(pos, state.setValue(property, value), Block.UPDATE_ALL);
    }
}