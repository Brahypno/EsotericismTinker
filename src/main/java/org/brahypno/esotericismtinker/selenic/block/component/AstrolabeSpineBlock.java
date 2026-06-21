package org.brahypno.esotericismtinker.selenic.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class AstrolabeSpineBlock extends Block {
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape CORE_MIDDLE = box(6, 2, 6, 10, 14, 10);
    private static final VoxelShape CORE_TOP = box(6, 14, 6, 10, 16, 10);
    private static final VoxelShape CORE_BOTTOM = box(6, 0, 6, 10, 2, 10);
    private static final VoxelShape MID_BAND = box(5, 7, 5, 11, 9, 11);
    private static final VoxelShape FRONT_INLAY = box(7.1, 3, 5.75, 8.9, 13, 6);
    private static final VoxelShape TOP_COLLAR = box(5, 14, 5, 11, 15, 11);
    private static final VoxelShape TOP_PLATE = box(4, 15, 4, 12, 16, 12);
    private static final VoxelShape BOTTOM_COLLAR = box(5, 1, 5, 11, 2, 11);
    private static final VoxelShape BOTTOM_PLATE = box(4, 0, 4, 12, 1, 12);

    private static final VoxelShape BODY_SHAPE = Shapes.or(CORE_MIDDLE, MID_BAND, FRONT_INLAY);
    private static final VoxelShape ISOLATED_SHAPE = Shapes.or(BODY_SHAPE, TOP_COLLAR, TOP_PLATE, BOTTOM_COLLAR, BOTTOM_PLATE);
    private static final VoxelShape UP_ONLY_SHAPE = Shapes.or(BODY_SHAPE, CORE_TOP, BOTTOM_COLLAR, BOTTOM_PLATE);
    private static final VoxelShape DOWN_ONLY_SHAPE = Shapes.or(BODY_SHAPE, TOP_COLLAR, TOP_PLATE, CORE_BOTTOM);
    private static final VoxelShape CONNECTED_SHAPE = Shapes.or(BODY_SHAPE, CORE_TOP, CORE_BOTTOM);

    public AstrolabeSpineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                                            .setValue(UP, false)
                                            .setValue(DOWN, false)
                                            .setValue(ACTIVE, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        return defaultBlockState()
                .setValue(UP, connectsTo(level, pos.above()))
                .setValue(DOWN, connectsTo(level, pos.below()))
                .setValue(ACTIVE, false);
    }

    @Override
    public BlockState updateShape(
            BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP){
            return state.setValue(UP, connectsTo(neighborState));
        }
        if (direction == Direction.DOWN){
            return state.setValue(DOWN, connectsTo(neighborState));
        }
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(UP), state.getValue(DOWN));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(UP), state.getValue(DOWN));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, ACTIVE);
    }

    private static VoxelShape shapeFor(boolean up, boolean down) {
        if (up && down){
            return CONNECTED_SHAPE;
        }
        if (up){
            return UP_ONLY_SHAPE;
        }
        if (down){
            return DOWN_ONLY_SHAPE;
        }
        return ISOLATED_SHAPE;
    }

    private static boolean connectsTo(BlockGetter level, BlockPos pos) {
        return connectsTo(level.getBlockState(pos));
    }

    private static boolean connectsTo(BlockState state) {
        Block block = state.getBlock();
        return block instanceof AstrolabeSpineBlock
               || block instanceof ArmillaryCrownBlock
               || block instanceof LunarFontBlock;
    }
}
