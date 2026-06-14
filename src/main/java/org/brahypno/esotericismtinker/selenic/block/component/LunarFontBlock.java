package org.brahypno.esotericismtinker.selenic.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;
import org.brahypno.esotericismtinker.selenic.block.entity.LunarFontBlockEntity;

import javax.annotation.Nullable;
import java.util.function.ToIntFunction;

public class LunarFontBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty SIGNALING = BooleanProperty.create("signaling");

    public static final ToIntFunction<BlockState> LIGHT =
            state -> state.getValue(ACTIVE) ? 10 : state.getValue(SIGNALING) ? 6 : 0;

    public LunarFontBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                                            .setValue(ACTIVE, false)
                                            .setValue(SIGNALING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, SIGNALING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LunarFontBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide){
            return null;
        }

        return createTickerHelper(
                type,
                EsotericismTinkerSelenic.lunarFontBE.get(),
                LunarFontBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof LunarFontBlockEntity font)){
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
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            net.minecraft.world.level.block.Block block,
            BlockPos fromPos,
            boolean isMoving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LunarFontBlockEntity font){
            font.handleRedstoneInput();
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(SIGNALING)){
            level.setBlock(pos, state.setValue(SIGNALING, false), 3);
            level.updateNeighborsAt(pos, this);
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
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())){
            if (level.getBlockEntity(pos) instanceof LunarFontBlockEntity font){
                font.dropContents();
            }

            level.removeBlockEntity(pos);
        }

        super.onRemove(oldState, level, pos, newState, moving);
    }
}