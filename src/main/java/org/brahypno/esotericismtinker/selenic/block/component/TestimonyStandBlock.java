package org.brahypno.esotericismtinker.selenic.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.brahypno.esotericismtinker.selenic.block.entity.TestimonyStandBlockEntity;

import javax.annotation.Nullable;

public class TestimonyStandBlock extends BaseEntityBlock {
    public static final BooleanProperty FLOATING = BooleanProperty.create("floating");

    private static final VoxelShape GROUNDED_SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13),
            Block.box(4, 2, 4, 12, 3, 12),
            Block.box(7, 3, 7, 9, 9, 9),
            Block.box(6, 4, 6, 10, 5, 10),
            Block.box(6, 7, 6, 10, 8, 10),
            Block.box(6.5, 9, 6.5, 9.5, 10, 9.5),
            Block.box(7, 11, 5, 9, 12.5, 5.5),
            Block.box(7, 11, 10.5, 9, 12.5, 11),
            Block.box(5, 11, 7, 5.5, 12.5, 9),
            Block.box(10.5, 11, 7, 11, 12.5, 9)
    );

    private static final VoxelShape FLOATING_SHAPE = Shapes.or(
            Block.box(5.5, 0.5, 5.5, 10.5, 1.5, 10.5),
            Block.box(7, 1.5, 7, 9, 7.5, 9),
            Block.box(6, 2.5, 6, 10, 3.5, 10),
            Block.box(6, 5.5, 6, 10, 6.5, 10),
            Block.box(6.5, 7.5, 6.5, 9.5, 8.5, 9.5),
            Block.box(7, 9.5, 5, 9, 11, 5.5),
            Block.box(7, 9.5, 10.5, 9, 11, 11),
            Block.box(5, 9.5, 7, 5.5, 11, 9),
            Block.box(10.5, 9.5, 7, 11, 11, 9)
    );


    public TestimonyStandBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FLOATING, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FLOATING, isFloating(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestimonyStandBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FLOATING) ? FLOATING_SHAPE : GROUNDED_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN){
            return state.setValue(FLOATING, isFloating(level, pos));
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private static boolean isFloating(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();

        return !level.getBlockState(below)
                     .isFaceSturdy(level, below, Direction.UP, SupportType.RIGID);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof TestimonyStandBlockEntity stand)){
            return InteractionResult.PASS;
        }

        if (level.isClientSide){
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown() || held.isEmpty()){
            ItemStack extracted = stand.extractOne();

            if (!extracted.isEmpty()){
                if (held.isEmpty()){
                    player.setItemInHand(hand, extracted);
                }else {
                    player.getInventory().placeItemBackInInventory(extracted);
                }

                return InteractionResult.CONSUME;
            }
        }

        if (!held.isEmpty()){
            ItemStack remainder = stand.insertOne(held.copy());

            if (remainder.getCount() != held.getCount()){
                held.setCount(remainder.getCount());
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())){
            if (level.getBlockEntity(pos) instanceof TestimonyStandBlockEntity stand){
                stand.dropContents();
            }

            level.removeBlockEntity(pos);
        }

        super.onRemove(oldState, level, pos, newState, moving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLOATING);
    }
}
