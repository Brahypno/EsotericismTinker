package org.brahypno.esotericismtinker.selenic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestimonyStandBlockEntity extends BlockEntity {
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChangedAndUpdate();
        }
    };

    public void setTestimony(ItemStack stack) {
        items.setStackInSlot(0, stack);
        setChangedAndUpdate();
    }

    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);

    public TestimonyStandBlockEntity(BlockPos pos, BlockState state) {
        super(EsotericismTinkerSelenic.testimonyStandBE.get(), pos, state);
    }

    public ItemStack getTestimony() {
        return items.getStackInSlot(0);
    }

    public ItemStack insertOne(ItemStack stack) {
        if (stack.isEmpty()){
            return stack;
        }

        ItemStack single = stack.copy();
        single.setCount(1);

        ItemStack remainder = items.insertItem(0, single, false);

        if (remainder.isEmpty()){
            stack.shrink(1);
        }

        return stack;
    }

    public ItemStack extractOne() {
        return items.extractItem(0, 64, false);
    }

    public void dropContents() {
        if (level == null){
            return;
        }

        ItemStack stack = items.getStackInSlot(0);

        if (!stack.isEmpty()){
            Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D, stack);
            items.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    public void setChangedAndUpdate() {
        setChanged();

        if (level != null){
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER){
            return itemCap.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            net.minecraft.network.Connection net,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet
    ) {
        CompoundTag tag = packet.getTag();

        if (tag != null){
            load(tag);
        }
    }

}