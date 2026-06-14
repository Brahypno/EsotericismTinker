package org.brahypno.esotericismtinker.selenic.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.brahypno.esotericismtinker.selenic.block.entity.TestimonyStandBlockEntity;

import javax.annotation.Nullable;

public class TestimonyStandBlock extends BaseEntityBlock {
    public TestimonyStandBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestimonyStandBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null;
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
}