package org.brahypno.esotericismtinker.selenic.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import org.brahypno.esotericismtinker.selenic.block.entity.ArmillaryCrownBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToIntFunction;

import static org.brahypno.esotericismtinker.selenic.block.component.SelenicBlockStates.ACTIVE;
import static org.brahypno.esotericismtinker.selenic.block.component.SelenicBlockStates.HALF;

public class ArmillaryCrownBlock extends BaseEntityBlock {

    public static final ToIntFunction<BlockState> LIGHT = state ->
            SelenicBlockStates.isUpper(state, HALF) ? 6 : state.getValue(ACTIVE) ? 12 : 6;

    private static final VoxelShape LOWER_SHAPE = Shapes.or(
            // base: [5,0,5] -> [11,2,11]
            Block.box(5, 0, 5, 11, 2, 11),

            // base_ring: [4,2,4] -> [12,3,12]
            Block.box(4, 2, 4, 12, 3, 12),

            // core: [7,2,7] -> [9,7,9]
            Block.box(7, 2, 7, 9, 7, 9),

            // ring_ns_bottom: [7.4,4.5,3.5] -> [8.6,5.3,12.5]
            Block.box(7.4, 4.5, 3.5, 8.6, 5.3, 12.5),

            // ring_ns_north: [7.4,5.3,3.5] -> [8.6,8,4.3]
            Block.box(7.4, 5.3, 3.5, 8.6, 8, 4.3),

            // ring_ns_south: [7.4,5.3,11.7] -> [8.6,8,12.5]
            Block.box(7.4, 5.3, 11.7, 8.6, 8, 12.5),

            // ring_ns_top: [7.4,8,3.5] -> [8.6,8.8,12.5]
            Block.box(7.4, 8, 3.5, 8.6, 8.8, 12.5),

            // ring_ew_bottom: [3.5,4.7,7.4] -> [12.5,5.7,8.6]
            Block.box(3.5, 4.7, 7.4, 12.5, 5.7, 8.6),

            // ring_ew_west: [3.5,5.7,7.4] -> [4.3,7.7,8.6]
            Block.box(3.5, 5.7, 7.4, 4.3, 7.7, 8.6),

            // ring_ew_east: [11.7,5.7,7.4] -> [12.5,7.7,8.6]
            Block.box(11.7, 5.7, 7.4, 12.5, 7.7, 8.6),

            // ring_ew_top: [3.5,7.7,7.4] -> [12.5,8.7,8.6]
            Block.box(3.5, 7.7, 7.4, 12.5, 8.7, 8.6),

            // cradle_plate: [5,8.8,5] -> [11,9.8,11]
            Block.box(5, 8.8, 5, 11, 9.8, 11),

            // cradle_north: [4,9.8,4] -> [12,10.8,5]
            Block.box(4, 9.8, 4, 12, 10.8, 5),

            // cradle_south: [4,9.8,11] -> [12,10.8,12]
            Block.box(4, 9.8, 11, 12, 10.8, 12),

            // cradle_west: [4,9.8,5] -> [5,10.8,11]
            Block.box(4, 9.8, 5, 5, 10.8, 11),

            // cradle_east: [11,9.8,5] -> [12,10.8,11]
            Block.box(11, 9.8, 5, 12, 10.8, 11)
    );

    private static final VoxelShape UPPER_SHAPE = Shapes.or(
            // upper half of the dynamic sphere selection volume
            // local upper Y=0 means total model Y=16.
            // ring center is around total Y=18.9, i.e. upper local Y=2.9.
            // Make this taller so the top of the dynamic ring is selectable.
            Block.box(0.5, 0, 0.5, 15.5, 12, 15.5),

            // slightly denser center area for item/ring center
            Block.box(3, 1, 3, 13, 7, 13)
    );

    private static final VoxelShape LOWER_COLLISION = Shapes.or(
            // bottom solid base
            Block.box(4, 0, 4, 12, 3, 12),

            // central solid axis
            Block.box(7, 3, 7, 9, 9.8, 9),

            // cradle rim as interactable crown top
            Block.box(4, 9.8, 4, 12, 10.8, 12)
    );


    public ArmillaryCrownBlock(Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any()
                                            .setValue(ACTIVE, false)
                                            .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return SelenicBlockStates.isLower(state)
               ? new ArmillaryCrownBlockEntity(pos, state)
               : null;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        if (pos.getY() >= level.getMaxBuildHeight() - 1){
            return null;
        }

        if (!level.getBlockState(pos.above()).canBeReplaced(context)){
            return null;
        }

        return defaultBlockState()
                .setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)
                .setValue(ACTIVE, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockPos upperPos = SelenicBlockStates.upperPos(pos, state);
        BlockState upperState = state
                .setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)
                .setValue(ACTIVE, false);

        level.setBlock(upperPos, upperState, Block.UPDATE_ALL);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (SelenicBlockStates.isLower(state, HALF)){
            BlockState above = level.getBlockState(pos.above());
            return above.isAir()
                   || above.is(this) && SelenicBlockStates.isUpper(above, HALF);
        }

        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && SelenicBlockStates.isLower(below, HALF);
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos
    ) {
        if (SelenicBlockStates.isLower(state, HALF) && direction == net.minecraft.core.Direction.UP){
            if (!neighborState.is(this) || !SelenicBlockStates.isUpper(neighborState, HALF)){
                return Fluids.EMPTY.defaultFluidState().createLegacyBlock();
            }
        }

        if (SelenicBlockStates.isUpper(state, HALF) && direction == net.minecraft.core.Direction.DOWN){
            if (!neighborState.is(this) || !SelenicBlockStates.isLower(neighborState, HALF)){
                return Fluids.EMPTY.defaultFluidState().createLegacyBlock();
            }
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SelenicBlockStates.isLower(state) ? LOWER_SHAPE : UPPER_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SelenicBlockStates.isLower(state) ? LOWER_COLLISION : Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ArmillaryCrownBlockEntity crown = getCrown(level, pos, state);

        if (crown == null){
            return InteractionResult.PASS;
        }

        if (level.isClientSide){
            return InteractionResult.SUCCESS;
        }

        if (tryFluidInteraction(crown, player, hand)){
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown() || held.isEmpty()){
            ItemStack extracted = crown.extractOneStack();

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
            ItemStack remainder = crown.insertInputItem(held.copy());

            if (remainder.getCount() != held.getCount()){
                held.setCount(remainder.getCount());
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide){
            SelenicBlockStates.removeOtherHalf(level, pos, state);
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving
    ) {
        if (oldState.is(newState.getBlock())){
            super.onRemove(oldState, level, pos, newState, moving);
            return;
        }

        if (SelenicBlockStates.isLower(oldState, HALF)){
            if (level.getBlockEntity(pos) instanceof ArmillaryCrownBlockEntity crown){
                crown.dropContents();
            }

            level.removeBlockEntity(pos);
        }

        super.onRemove(oldState, level, pos, newState, moving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, HALF);
    }

    @Nullable
    private ArmillaryCrownBlockEntity getCrown(Level level, BlockPos pos, BlockState state) {
        return SelenicBlockStates.getLowerBlockEntity(level, pos, state, ArmillaryCrownBlockEntity.class);
    }

    private boolean tryFluidInteraction(
            ArmillaryCrownBlockEntity crown,
            Player player,
            InteractionHand hand
    ) {
        return crown.getCapability(ForgeCapabilities.FLUID_HANDLER)
                    .map(handler -> FluidUtil.interactWithFluidHandler(player, hand, handler))
                    .orElse(false);
    }

    public static void setActive(Level level, BlockPos pos, boolean active) {
        SelenicBlockStates.setDoubleActive(level, pos, HALF, ACTIVE, active);
    }
}