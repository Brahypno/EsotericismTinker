package org.brahypno.esotericismtinker.smeltery.block.entity.module;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.brahypno.esotericismtinker.Config;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.smeltery.block.entity.controller.TransmuteBlockEntity;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModule;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SuperByproductMeltingModuleInventory extends MeltingModuleInventory {
    // MeltingModule exposes its progress and recipe requirements through ContainerData.
    private static final int CURRENT_TIME = 0;
    private static final int REQUIRED_TIME = 1;
    private static final int REQUIRED_TEMP = 2;
    private static final AtomicInteger RECIPE_CACHE_VERSION = new AtomicInteger();

    static {
        RecipeCacheInvalidator.addReloadListener(client -> clearTransmuteV2RecipeCaches());
    }

    private final TransmuteBlockEntity parent;
    private final Map<IMeltingContainer, CachedTransmuteV2Recipe> transmuteV2Recipes = new IdentityHashMap<>();
    @Nullable private RecipeManager cachedRecipeManager;
    private int cachedRecipeVersion = -1;
    private MeltingModule[] smeltery_modules;//Just using oreRate for smeltery one
    private MeltType meltType = MeltType.Transmute;

    public SuperByproductMeltingModuleInventory(TransmuteBlockEntity parent, IFluidHandler fluidHandler, IMeltingContainer.IOreRate oreRate, int size) {
        super(parent, fluidHandler, oreRate, size);
        this.parent = parent;
        this.smeltery_modules = new MeltingModule[size];
    }

    public SuperByproductMeltingModuleInventory(TransmuteBlockEntity parent, IFluidHandler fluidHandler, IMeltingContainer.IOreRate oreRate) {
        this(parent, fluidHandler, oreRate, 0);
    }

    public void updateMeltType(MeltType meltType) {
        if (this.meltType == meltType) {
            return;
        }
        boolean recipeTypeChanged = isTransmuteV2() || meltType == MeltType.Transmute_v2;
        this.meltType = meltType;
        if (recipeTypeChanged) {
            transmuteV2Recipes.clear();
            for (int slot = 0; slot < getSlots(); slot++) {
                if (!getStackInSlot(slot).isEmpty()) {
                    MeltingModule module = getModule(slot);
                    // Reset progress (including NO_SPACE) and recompute time and temperature for the new type.
                    module.set(CURRENT_TIME, 0);
                    module.setStack(module.getStack());
                    updateTransmuteV2Requirements(module);
                }
            }
        }
        parent.setChangedFast();
    }

    public boolean isTransmuteV2() {
        return meltType == MeltType.Transmute_v2;
    }

    /** Invalidates furnace-local lookups when server recipes reload or client recipes sync. */
    public static void clearTransmuteV2RecipeCaches() {
        RECIPE_CACHE_VERSION.incrementAndGet();
    }

    @Override
    public CompoundTag writeToTag() {
        CompoundTag tag = super.writeToTag();
        tag.putInt("melt_type", meltType.ordinal());
        return tag;
    }

    @Override
    public void readFromTag(CompoundTag tag) {
        // Restore the type before loading slots so saved v2 progress cannot finish a vanilla recipe.
        meltType = MeltType.toMeltType(tag.getInt("melt_type"));
        transmuteV2Recipes.clear();
        super.readFromTag(tag);
        smeltery_modules = Arrays.copyOf(smeltery_modules, getSlots());
    }

    public void resize(int newSize, Consumer<ItemStack> stackConsumer) {
        int oldSize = getSlots();
        super.resize(newSize, stackConsumer);
        smeltery_modules = Arrays.copyOf(smeltery_modules, getSlots());
        if (oldSize != getSlots()) {
            transmuteV2Recipes.clear();
        }
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (0 == amount || !validSlot(slot))
            return ItemStack.EMPTY;

        ItemStack existing = getStackInSlot(slot);
        if (existing.isEmpty())
            return ItemStack.EMPTY;

        int extractSize = Math.min(amount, existing.getCount());
        if (simulate){
            return ItemHandlerHelper.copyStackWithSize(existing, extractSize);
        }else {
            ItemStack extracted = ItemHandlerHelper.copyStackWithSize(existing, extractSize);
            setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - extractSize));
            return extracted;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 2;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        // actually set the stack
        if (validSlot(slot)){
            if (stack.isEmpty()){
                getModule(slot).setStack(ItemStack.EMPTY);
            }else {
                // validate size
                if (stack.getCount() > getSlotLimit(slot)){
                    stack.setCount(getSlotLimit(slot));
                }
                getModule(slot).setStack(stack);
                updateTransmuteV2Requirements(getModule(slot));
            }
        }
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()){
            return ItemStack.EMPTY;
        }
        if (slot < 0 || slot >= getSlots()){
            return stack;
        }

        MeltingModule module = getModule(slot);
        ItemStack existing = module.getStack();

        // Check if slot is empty or contains same item with room
        boolean isSameItem = !existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack);
        boolean canInsert = existing.isEmpty() || (isSameItem && existing.getCount() < getSlotLimit(slot));

        int insertSize = canInsert ? Math.min(stack.getCount(), getSlotLimit(slot) - existing.getCount()) : 0;

        if (!simulate && canInsert && insertSize > 0){
            ItemStack toInsert = ItemHandlerHelper.copyStackWithSize(stack, insertSize);
            if (existing.isEmpty()){
                setStackInSlot(slot, toInsert);
            }else {
                ItemStack combined = existing.copy();
                combined.grow(insertSize);
                setStackInSlot(slot, combined);
            }
        }
        return insertSize > 0 ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - insertSize) : stack;
    }

    @Nullable
    private IMeltingRecipe findTransmuteV2Recipe(IMeltingContainer container) {
        Level level = parent.getLevel();
        if (level == null) {
            return null;
        }
        RecipeManager manager = level.getRecipeManager();
        int version = RECIPE_CACHE_VERSION.get();
        if (cachedRecipeManager != manager || cachedRecipeVersion != version) {
            transmuteV2Recipes.clear();
            cachedRecipeManager = manager;
            cachedRecipeVersion = version;
        }
        CachedTransmuteV2Recipe cached = transmuteV2Recipes.get(container);
        if (cached != null && ItemStack.matches(cached.input(), container.getStack()) && cached.recipe().matches(container, level)) {
            return cached.recipe();
        }
        IMeltingRecipe recipe = manager.getRecipeFor(
                EsotericismTinkerRecipeTypes.TRANSMUTE_V2_TYPE.get(), container, level)
                .orElseGet(() -> manager.getRecipeFor(
                        TinkerRecipeTypes.MELTING.get(), container, level).orElse(null));
        if (recipe == null) {
            transmuteV2Recipes.remove(container);
        } else {
            // Keep the recipe, not its fluid output: amounts and byproducts still use the live container.
            transmuteV2Recipes.put(container, new CachedTransmuteV2Recipe(container.getStack().copy(), recipe));
        }
        return recipe;
    }

    private record CachedTransmuteV2Recipe(ItemStack input, IMeltingRecipe recipe) {}

    private void updateTransmuteV2Requirements(MeltingModule module) {
        if (isTransmuteV2()) {
            IMeltingRecipe recipe = findTransmuteV2Recipe(module);
            module.set(REQUIRED_TIME, recipe == null ? 0 : recipe.getTime(module) * 10);
            module.set(REQUIRED_TEMP, recipe == null ? 0 : recipe.getTemperature(module));
        }
    }

    @Override
    public void heatItems(int temperature, int rate) {
        if (!isTransmuteV2()) {
            super.heatItems(temperature, rate);
            return;
        }
        for (int slot = 0; slot < getSlots(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                MeltingModule module = getModule(slot);
                if (module.getCurrentTime() == MeltingModule.NO_SPACE || module.canHeatItem(temperature)) {
                    if (module.getCurrentTime() == MeltingModule.NO_SPACE || module.getCurrentTime() >= module.getRequiredTime()) {
                        finishTransmuteV2(slot, module);
                    } else {
                        module.set(CURRENT_TIME, module.getCurrentTime() + rate);
                    }
                }
            }
        }
    }

    @Override
    public void coolItems() {
        if (!isTransmuteV2()) {
            super.coolItems();
            return;
        }
        for (int slot = 0; slot < getSlots(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                MeltingModule module = getModule(slot);
                if (module.getCurrentTime() == MeltingModule.NO_SPACE) {
                    finishTransmuteV2(slot, module);
                } else if (module.getCurrentTime() > 0 && module.getRequiredTime() > 0) {
                    module.set(CURRENT_TIME, module.getCurrentTime() - 5);
                }
            }
        }
    }

    private void finishTransmuteV2(int slot, MeltingModule module) {
        IMeltingRecipe recipe = findTransmuteV2Recipe(module);
        if (recipe == null) {
            module.set(CURRENT_TIME, 0);
            module.set(REQUIRED_TIME, 0);
            module.set(REQUIRED_TEMP, 0);
        } else if (tryFillTank(slot, recipe)) {
            setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            module.set(CURRENT_TIME, MeltingModule.NO_SPACE);
        }
    }

    @Override
    protected boolean tryFillTank(int index, IMeltingRecipe recipe) {
        return switch (meltType) {
            case SMELTERY -> tryFillTankSmeltery(index, recipe);
            case Foundry -> tryFillTankFoundry(index, recipe);
            case Transmute_v2 -> tryFillTankTransmuteV2(index, recipe);
            default -> tryFillFluidTransmute(index, recipe);
        };
    }

    private boolean tryFillTankTransmuteV2(int index, IMeltingRecipe recipe) {
        IMeltingContainer inv = getModule(index);
        FluidStack main = recipe.getOutput(inv);
        if (main.isEmpty() || main.getAmount() <= 0) {
            return false;
        }
        int count = inv.getStack().getCount();
        main.setAmount(main.getAmount() * count);
        if (fluidHandler.fill(main.copy(), IFluidHandler.FluidAction.SIMULATE) != main.getAmount()) {
            return false;
        }
        fluidHandler.fill(main, IFluidHandler.FluidAction.EXECUTE);
        for (int item = 0; item < count; item++) {
            recipe.handleByproducts(inv, fluidHandler);
        }
        return true;
    }

    private boolean tryFillTankFoundry(int index, IMeltingRecipe recipe) {
        IMeltingContainer inv = getModule(index);
        // 1) Main output
        FluidStack main = recipe.getOutput(inv);
        if (main.isEmpty() || main.getAmount() <= 0)
            return false;
        int count = 0;
        main.setAmount(main.getAmount() * inv.getStack().getCount());

        if (fluidHandler.fill(main.copy(), IFluidHandler.FluidAction.SIMULATE) != main.getAmount())
            return false;

        fluidHandler.fill(main, IFluidHandler.FluidAction.EXECUTE);
        while (count < inv.getStack().getCount()) {
            // 4) Let the recipe fill its byproducts into the real fluid handler.
            recipe.handleByproducts(inv, fluidHandler);
            ++count;
        }
        return true;
    }

    private MeltingModule getSmelteryModule(int slot) {
        if (!validSlot(slot)){
            throw new IndexOutOfBoundsException();
        }
        if (smeltery_modules[slot] == null){
            smeltery_modules[slot] = new MeltingModule(parent, recipe -> tryFillTank(slot, recipe), Config.TRANSMUTE_SMELTERY_ORE_RATE, slot);
        }
        return smeltery_modules[slot];
    }

    private boolean tryFillTankSmeltery(int index, IMeltingRecipe recipe) {
        FluidStack fluid = recipe.getOutput(getSmelteryModule(index));
        IMeltingContainer inv = getModule(index);
        fluid.setAmount((Mth.ceil(fluid.getAmount() * inv.getStack().getCount())));
        if (fluidHandler.fill(fluid.copy(), IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount()){
            fluidHandler.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            return true;
        }
        return false;
    }

    private boolean tryFillFluidTransmute(int index, IMeltingRecipe recipe) {
        IMeltingContainer inv = getModule(index);

        // 1) Main output
        FluidStack main = recipe.getOutput(inv);
        if (main.isEmpty() || main.getAmount() <= 0)
            return false;
        int count = 0;
        main.setAmount(main.getAmount() * inv.getStack().getCount());

        if (fluidHandler.fill(main.copy(), IFluidHandler.FluidAction.SIMULATE) != main.getAmount())
            return false;

        ByproductCollector tank = new ByproductCollector();
        while (count < inv.getStack().getCount()) {
            // 4) Let the recipe fill its byproducts into the temporary tank.
            recipe.handleByproducts(inv, tank);
            ++count;
        }
        // 5) Collect the non-empty byproduct fluids and their total amount.
        List<FluidStack> bys = collectNonEmptyFluids(tank);

        long M = main.getAmount();
        long B = 0L;
        FluidStack largest = FluidStack.EMPTY;
        long largestAmt = 0L;

        for (FluidStack by : bys) {
            long amt = by.getAmount();
            if (amt <= 0){
                continue;
            }

            B += amt;
            if (amt > largestAmt){
                largestAmt = amt;
                largest = by;
            }
        }

        if (B <= 0L){
            fluidHandler.fill(main.copy(), IFluidHandler.FluidAction.EXECUTE);
            return true;
        }

        FluidStack swappedMain = main.copy();
        swappedMain.setAmount((int) Math.min(Integer.MAX_VALUE, B));
        fluidHandler.fill(swappedMain, IFluidHandler.FluidAction.EXECUTE);

        long remainder = M;
        for (FluidStack by : bys) {
            long amt = by.getAmount();
            if (amt <= 0){
                continue;
            }

            long share = M * amt / B;
            if (share <= 0L){
                continue;
            }

            remainder -= share;

            FluidStack out = by.copy();
            out.setAmount((int) Math.min(Integer.MAX_VALUE, share));
            fluidHandler.fill(out, IFluidHandler.FluidAction.EXECUTE);
        }

        if (remainder > 0L && !largest.isEmpty()){
            FluidStack extra = largest.copy();
            extra.setAmount((int) Math.min(Integer.MAX_VALUE, remainder));
            fluidHandler.fill(extra, IFluidHandler.FluidAction.EXECUTE);
        }

        return true;
    }

    public enum MeltType {
        Transmute,
        SMELTERY,
        Foundry,
        Transmute_v2;

        public static MeltType toMeltType(int i) {
            return switch (i) {
                case 0 -> Transmute;
                case 1 -> SMELTERY;
                case 2 -> Foundry;
                case 3 -> Transmute_v2;
                default -> throw new IllegalArgumentException("Invalid melt type: " + i);
            };
        }

    }

    private static List<FluidStack> collectNonEmptyFluids(IFluidHandler handler) {
        List<FluidStack> list = new ArrayList<>();
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack fs = handler.getFluidInTank(i);
            if (!fs.isEmpty() && fs.getAmount() > 0)
                list.add(fs.copy());
        }
        return list;
    }

    /**
     * Temporary byproduct sink used only to measure a recipe's complete output.
     * It deliberately has no fixed capacity: third-party recipes may define a
     * byproduct ratio far above the main output amount.
     */
    private static final class ByproductCollector implements IFluidHandler {
        private final List<FluidStack> fluids = new ArrayList<>();

        @Override
        public int getTanks() {
            return fluids.size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank >= 0 && tank < fluids.size() ? fluids.get(tank).copy() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return !stack.isEmpty();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getAmount() <= 0){
                return 0;
            }

            if (action.execute()){
                for (FluidStack stored : fluids) {
                    if (stored.isFluidEqual(resource)){
                        long combined = (long) stored.getAmount() + resource.getAmount();
                        stored.setAmount((int) Math.min(Integer.MAX_VALUE, combined));
                        return resource.getAmount();
                    }
                }
                fluids.add(resource.copy());
            }

            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
