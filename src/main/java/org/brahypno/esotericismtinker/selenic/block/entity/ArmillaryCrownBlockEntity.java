package org.brahypno.esotericismtinker.selenic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ArmillaryCrownBlockEntity extends BlockEntity {
    private static final int MAX_INPUT_SLOTS = 8;
    private static final int BASE_INPUT_FLUID_CAPACITY = 1000;
    private static final int INPUT_FLUID_CAPACITY_PER_SPINE = 1000;
    private static final int MAX_SPINES = 32;

    private final ItemStackHandler items = new ItemStackHandler(MAX_INPUT_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return slot < getActiveInputSlots() ? 64 : 0;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= getActiveInputSlots()){
                return stack;
            }

            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= getActiveInputSlots()){
                return ItemStack.EMPTY;
            }

            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChangedAndUpdate();
        }
    };

    private final FluidTank tank = new FluidTank(BASE_INPUT_FLUID_CAPACITY) {
        @Override
        public int getCapacity() {
            return getFluidCapacity();
        }

        @Override
        protected void onContentsChanged() {
            setChangedAndUpdate();
        }
    };

    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);

    public ArmillaryCrownBlockEntity(BlockPos pos, BlockState state) {
        super(EsotericismTinkerSelenic.armillaryCrownBE.get(), pos, state);
    }

    public ItemStack insertInputItem(ItemStack stack) {
        trimToCurrentCapacity();
        return ItemHandlerHelper.insertItemStacked(items, stack, false);
    }

    public ItemStack extractOneStack() {
        trimToCurrentCapacity();

        for (int i = getActiveInputSlots() - 1; i >= 0; i--) {
            ItemStack extracted = items.extractItem(i, 64, false);

            if (!extracted.isEmpty()){
                return extracted;
            }
        }

        return ItemStack.EMPTY;
    }

    public List<ItemStack> copyInputStacks() {
        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < getActiveInputSlots(); i++) {
            stacks.add(items.getStackInSlot(i).copy());
        }

        return stacks;
    }

    public ItemStack getInputStack(int slot) {
        return slot < getActiveInputSlots() ? items.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    public void setInputStack(int slot, ItemStack stack) {
        if (slot < getActiveInputSlots()){
            items.setStackInSlot(slot, stack);
        }
    }

    public FluidStack getInputFluid() {
        return tank.getFluid().copy();
    }

    public void drainInputFluid(int amount) {
        if (amount > 0){
            tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public void dropContents() {
        if (level == null){
            return;
        }

        for (int i = 0; i < getActiveInputSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);

            if (!stack.isEmpty()){
                Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, stack);
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public int getInputSlotCount() {
        return getActiveInputSlots();
    }

    public int getActiveInputSlots() {
        return Mth.clamp(1 + getUpperSpinesBelow(), 1, MAX_INPUT_SLOTS);
    }

    public int getFluidCapacity() {
        return BASE_INPUT_FLUID_CAPACITY + getUpperSpinesBelow() * INPUT_FLUID_CAPACITY_PER_SPINE;
    }

    private int getUpperSpinesBelow() {
        if (level == null){
            return 0;
        }

        int spines = 0;
        BlockPos cursor = worldPosition.below();

        while (level.getBlockState(cursor).is(EsotericismTinkerSelenic.astrolabeSpine.get())) {
            spines++;

            if (spines > MAX_SPINES){
                return MAX_SPINES;
            }

            cursor = cursor.below();
        }

        return level.getBlockState(cursor).is(EsotericismTinkerSelenic.lunarFont.get()) ? spines : 0;
    }

    public void trimToCurrentCapacity() {
        dropExcessItems();
        capFluidToCapacity();
    }

    private void dropExcessItems() {
        if (level == null){
            return;
        }

        for (int i = getActiveInputSlots(); i < MAX_INPUT_SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);

            if (!stack.isEmpty()){
                Containers.dropItemStack(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D,
                        stack);

                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    private void capFluidToCapacity() {
        FluidStack fluid = tank.getFluid();

        if (!fluid.isEmpty() && fluid.getAmount() > getFluidCapacity()){
            FluidStack capped = fluid.copy();
            capped.setAmount(getFluidCapacity());
            tank.setFluid(capped);
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
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        tank.readFromNBT(tag.getCompound("Tank"));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER){
            return itemCap.cast();
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER){
            return fluidCap.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        fluidCap.invalidate();
    }

}