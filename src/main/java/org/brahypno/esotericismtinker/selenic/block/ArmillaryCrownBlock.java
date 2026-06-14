package org.brahypno.esotericismtinker.selenic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import org.brahypno.esotericismtinker.selenic.block.entity.ArmillaryCrownBlockEntity;

import javax.annotation.Nullable;

public class ArmillaryCrownBlock extends BaseEntityBlock {
    public ArmillaryCrownBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArmillaryCrownBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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

        if (!(be instanceof ArmillaryCrownBlockEntity crown)){
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

    private boolean tryFluidInteraction(ArmillaryCrownBlockEntity crown, Player player, InteractionHand hand) {
        return crown.getCapability(ForgeCapabilities.FLUID_HANDLER)
                    .map(handler -> FluidUtil.interactWithFluidHandler(player, hand, handler))
                    .orElse(false);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())){
            if (level.getBlockEntity(pos) instanceof ArmillaryCrownBlockEntity crown){
                crown.dropContents();
            }

            level.removeBlockEntity(pos);
        }

        super.onRemove(oldState, level, pos, newState, moving);
    }
}