package org.brahypno.esotericismtinker.selenic.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;
import org.brahypno.esotericismtinker.selenic.block.entity.LunarFontBlockEntity;

import javax.annotation.Nullable;
import java.util.function.ToIntFunction;

import static org.brahypno.esotericismtinker.selenic.block.component.SelenicBlockStates.ACTIVE;
import static org.brahypno.esotericismtinker.selenic.block.component.SelenicBlockStates.HALF;

public class LunarFontBlock extends BaseEntityBlock {
    public static final BooleanProperty SIGNALING = BooleanProperty.create("signaling");

    public static final ToIntFunction<BlockState> LIGHT = state ->
            state.getValue(ACTIVE) ? 12 : state.getValue(SIGNALING) ? 10 : 6;

    private static final VoxelShape LOWER_SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(2, 3, 2, 14, 6, 14),
            Block.box(2, 6, 2, 14, 10, 4),
            Block.box(2, 6, 12, 14, 10, 14),
            Block.box(2, 6, 4, 4, 10, 12),
            Block.box(12, 6, 4, 14, 10, 12),
            Block.box(4, 6, 4, 12, 9, 5),
            Block.box(4, 6, 11, 12, 9, 12),
            Block.box(4, 6, 5, 5, 9, 11),
            Block.box(11, 6, 5, 12, 9, 11),
            Block.box(6, 7, 6, 10, 14, 10),
            Block.box(5, 13, 5, 11, 14, 6),
            Block.box(5, 13, 10, 11, 14, 11),
            Block.box(5, 13, 6, 6, 14, 10),
            Block.box(10, 13, 6, 11, 14, 10),
            Block.box(7, 15, 7, 9, 16, 9)
    );

    private static final VoxelShape UPPER_SHAPE = Shapes.or(
            Block.box(5, 0, 5, 10, 1, 6),
            Block.box(5, 0, 10, 10, 1, 11),
            Block.box(5, 0, 6, 6, 1, 10),
            Block.box(10, 0, 5, 11, 1, 11),
            Block.box(7, 0, 7, 9, 5, 9),
            Block.box(3, 2, 3, 4, 14, 4),
            Block.box(12, 2, 3, 13, 14, 4),
            Block.box(3, 2, 12, 4, 14, 13),
            Block.box(12, 2, 12, 13, 14, 13),
            Block.box(4, 13, 3, 12, 14, 4),
            Block.box(4, 13, 12, 12, 14, 13),
            Block.box(3, 13, 4, 4, 14, 12),
            Block.box(12, 13, 4, 13, 14, 12),
            Block.box(4, 14, 4, 12, 15, 12),
            Block.box(6, 15, 6, 10, 16, 10),
            Block.box(6, 5, 6, 10, 6, 10),
            Block.box(3, 14, 3, 4, 14.1, 4),
            Block.box(2.9, 13, 3, 3, 14, 4),
            Block.box(3, 13, 2.9, 4, 14, 3),
            Block.box(3, 1.9, 3, 4, 2, 4),
            Block.box(2.9, 2, 3, 3, 3, 4),
            Block.box(4, 2, 3, 4.1, 3, 4),
            Block.box(3, 2, 2.9, 4, 3, 3),
            Block.box(3, 2, 4, 4, 3, 4.1),
            Block.box(12, 14, 3, 13, 14.1, 4),
            Block.box(13, 13, 3, 13.1, 14, 4),
            Block.box(12, 13, 2.9, 13, 14, 3),
            Block.box(12, 1.9, 3, 13, 2, 4),
            Block.box(11.9, 2, 3, 12, 3, 4),
            Block.box(13, 2, 3, 13.1, 3, 4),
            Block.box(12, 2, 2.9, 13, 3, 3),
            Block.box(12, 2, 4, 13, 3, 4.1),
            Block.box(3, 14, 12, 4, 14.1, 13),
            Block.box(2.9, 13, 12, 3, 14, 13),
            Block.box(3, 13, 13, 4, 14, 13.1),
            Block.box(3, 1.9, 12, 4, 2, 13),
            Block.box(2.9, 2, 12, 3, 3, 13),
            Block.box(4, 2, 12, 4.1, 3, 13),
            Block.box(3, 2, 11.9, 4, 3, 12),
            Block.box(3, 2, 13, 4, 3, 13.1),
            Block.box(12, 14, 12, 13, 14.1, 13),
            Block.box(13, 13, 12, 13.1, 14, 13),
            Block.box(12, 13, 13, 13, 14, 13.1),
            Block.box(12, 1.9, 12, 13, 2, 13),
            Block.box(11.9, 2, 12, 12, 3, 13),
            Block.box(13, 2, 12, 13.1, 3, 13),
            Block.box(12, 2, 11.9, 13, 3, 12),
            Block.box(12, 2, 13, 13, 3, 13.1)
    );


    public LunarFontBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                                            .setValue(HALF, DoubleBlockHalf.LOWER)
                                            .setValue(ACTIVE, false)
                                            .setValue(SIGNALING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, ACTIVE, SIGNALING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)){
            return null;
        }
        return defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state
                .setValue(HALF, DoubleBlockHalf.UPPER)
                .setValue(ACTIVE, false)
                .setValue(SIGNALING, false), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction requiredDirection = SelenicBlockStates.isLower(state) ? Direction.UP : Direction.DOWN;

        if (direction == requiredDirection && !SelenicBlockStates.isMatchingOtherHalf(state, neighborState)){
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER){
            return true;
        }

        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && below.hasProperty(HALF) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return SelenicBlockStates.isLower(state)
               ? new LunarFontBlockEntity(pos, state)
               : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !SelenicBlockStates.isLower(state)){
            return null;
        }

        return createTickerHelper(type, EsotericismTinkerSelenic.lunarFontBE.get(), LunarFontBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        LunarFontBlockEntity font = getFont(level, pos, state);
        if (font == null){
            return InteractionResult.PASS;
        }

        if (level.isClientSide){
            return InteractionResult.SUCCESS;
        }

        if (tryFluidInteraction(font, player, hand, hit.getDirection())){
            return InteractionResult.CONSUME;
        }

        ItemStack output = font.extractOutputItem();
        if (!output.isEmpty()){
            giveToPlayer(player, hand, output);
            return InteractionResult.CONSUME;
        }

        if (player.getItemInHand(hand).isEmpty()){
            font.tryActivate(player);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Nullable
    private LunarFontBlockEntity getFont(Level level, BlockPos pos, BlockState state) {
        return SelenicBlockStates.getLowerBlockEntity(level, pos, state, LunarFontBlockEntity.class);
    }

    private void giveToPlayer(Player player, InteractionHand hand, ItemStack stack) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()){
            player.setItemInHand(hand, stack);
        }else {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    private boolean tryFluidInteraction(
            LunarFontBlockEntity font,
            Player player,
            InteractionHand hand,
            Direction side) {

        return font.getCapability(ForgeCapabilities.FLUID_HANDLER, side)
                   .map(handler -> FluidUtil.interactWithFluidHandler(player, hand, handler))
                   .orElse(false);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {

        if (level.isClientSide){
            return;
        }

        LunarFontBlockEntity font = getFont(level, pos, state);
        if (font != null){
            font.handleRedstoneInput();
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SIGNALING)){
            return;
        }

        BlockPos lowerPos = SelenicBlockStates.lowerPos(pos, state);
        SelenicBlockStates.setDoubleSignaling(level, lowerPos, false);

        level.updateNeighborsAt(lowerPos, this);
        level.updateNeighborsAt(lowerPos.above(), this);
    }

    public static void setBothHalves(Level level, BlockPos lowerPos, BooleanProperty property, boolean value) {
        setHalf(level, lowerPos, property, value);
        setHalf(level, lowerPos.above(), property, value);
    }

    private static void setHalf(Level level, BlockPos pos, BooleanProperty property, boolean value) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LunarFontBlock && state.hasProperty(property) && state.getValue(property) != value){
            level.setBlock(pos, state.setValue(property, value), 3);
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(SIGNALING) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SelenicBlockStates.isLower(state) ? LOWER_SHAPE : UPPER_SHAPE;
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
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (oldState.is(newState.getBlock())){
            super.onRemove(oldState, level, pos, newState, moving);
            return;
        }

        BlockPos lowerPos = SelenicBlockStates.lowerPos(pos, oldState);

        if (!level.isClientSide){
            if (level.getBlockEntity(lowerPos) instanceof LunarFontBlockEntity font){
                font.dropContents();
            }

            SelenicBlockStates.removeOtherHalf(level, pos, oldState);
        }

        if (SelenicBlockStates.isLower(oldState)){
            level.removeBlockEntity(pos);
        }

        super.onRemove(oldState, level, pos, newState, moving);
    }
}