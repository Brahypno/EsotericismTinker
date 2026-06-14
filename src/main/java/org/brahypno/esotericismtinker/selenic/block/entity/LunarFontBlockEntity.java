package org.brahypno.esotericismtinker.selenic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.brahypno.esotericismtinker.library.recipe.IngredientStackUtil;
import org.brahypno.esotericismtinker.library.recipe.MoonPhase;
import org.brahypno.esotericismtinker.library.recipe.selenic.*;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;
import org.brahypno.esotericismtinker.selenic.block.component.LunarFontBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class LunarFontBlockEntity extends BlockEntity {
    private static final int MAX_ITEM_SLOTS = 8;
    private static final int BASE_FLUID_CAPACITY = 1000;
    private static final int FLUID_CAPACITY_PER_SPINE = 1000;
    private static final int SUCCESS_SIGNAL_TICKS = 6;

    private final OutputItemHandler outputItems = new OutputItemHandler(this);
    private final OutputFluidTank outputTank = new OutputFluidTank(this);
    private final LazyOptional<OutputItemHandler> itemCap = LazyOptional.of(() -> outputItems);
    private final LazyOptional<OutputFluidTank> fluidCap = LazyOptional.of(() -> outputTank);

    private int cachedLowerSpines = 0;
    private int cachedUpperSpines = 0;
    private int cachedTestimonyCount = 0;
    private boolean wasPowered = false;

    @Nullable
    private ResourceLocation activeRecipeId;

    private float progress = 0.0F;
    private SelenicFailure lastFailure = SelenicFailure.NONE;

    public LunarFontBlockEntity(BlockPos pos, BlockState state) {
        super(EsotericismTinkerSelenic.lunarFontBE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LunarFontBlockEntity be) {
        if (level.getGameTime() % 20L == 0L){
            be.scanFigureAndTrim();
        }

        be.tickActiveRecipe();
    }

    public void tryActivate(@Nullable Player player) {
        Level level = this.level;

        if (level == null || level.isClientSide){
            return;
        }

        if (activeRecipeId != null){
            fail(SelenicFailure.ALREADY_ACTIVE, player);
            return;
        }

        SelenicFigure figure = SelenicFigureScanner.scan(level, worldPosition);

        if (!figure.valid()){
            fail(figure.failure(), player);
            return;
        }

        ArmillaryCrownBlockEntity crown = getCrown(level, figure);

        if (crown == null){
            fail(SelenicFailure.NO_INPUT_CROWN, player);
            return;
        }

        updateCachedFigure(level, figure);
        trimOutputToCurrentCapacity();

        Optional<SelenicAstrolabeRecipe> recipe = findStartableRecipe(level, figure, crown);

        if (recipe.isEmpty()){
            fail(lastFailure, player);
            return;
        }

        startRecipe(level, recipe.get());

        if (player != null){
            player.displayClientMessage(
                    Component.translatable("message.esotericism_tinker.selenic.started"),
                    true);
        }
    }

    public void handleRedstoneInput() {
        if (level == null || level.isClientSide){
            return;
        }

        boolean powered = level.hasNeighborSignal(worldPosition);

        if (!isSuccessSignaling() && powered && !wasPowered){
            tryActivate(null);
        }

        wasPowered = powered;
        setChanged();
    }

    private boolean isSuccessSignaling() {
        BlockState state = getBlockState();
        return state.hasProperty(LunarFontBlock.SIGNALING) && state.getValue(LunarFontBlock.SIGNALING);
    }

    private Optional<SelenicAstrolabeRecipe> findStartableRecipe(
            Level level,
            SelenicFigure figure,
            ArmillaryCrownBlockEntity crown) {
        boolean itemBlocked = false;
        boolean fluidBlocked = false;
        SelenicFailure bestFailure = SelenicFailure.NONE;

        for (SelenicAstrolabeRecipe recipe : SelenicRecipeCache.getSortedRecipes(level)) {
            RecipeCheck check = checkRecipe(level, recipe, figure, crown);

            if (check.isOk()){
                lastFailure = SelenicFailure.NONE;
                return Optional.of(recipe);
            }

            if (check.failure() == SelenicFailure.OUTPUT_ITEM_BLOCKED){
                itemBlocked = true;
                continue;
            }

            if (check.failure() == SelenicFailure.OUTPUT_FLUID_BLOCKED){
                fluidBlocked = true;
                continue;
            }

            bestFailure = chooseBetterFailure(bestFailure, check.failure());
        }

        if (itemBlocked){
            lastFailure = SelenicFailure.OUTPUT_ITEM_BLOCKED;
        }else if (fluidBlocked){
            lastFailure = SelenicFailure.OUTPUT_FLUID_BLOCKED;
        }else if (bestFailure != SelenicFailure.NONE){
            lastFailure = bestFailure;
        }else {
            lastFailure = SelenicFailure.NO_RECIPE;
        }

        return Optional.empty();
    }

    private SelenicFailure chooseBetterFailure(SelenicFailure current, SelenicFailure next) {
        return failurePriority(next) > failurePriority(current) ? next : current;
    }

    private int failurePriority(SelenicFailure failure) {
        return switch (failure) {
            case DAYTIME -> 90;
            case WRONG_MOON_PHASE -> 80;
            case WRONG_ELEVATION -> 70;
            case MISSING_MEDIUM -> 60;
            case MISSING_CROWN_INPUT -> 50;
            case MISSING_TESTIMONY -> 40;
            default -> 0;
        };
    }

    private void startRecipe(Level level, SelenicAstrolabeRecipe recipe) {
        activeRecipeId = recipe.getId();
        progress = 0.0F;

        setActive(true);
        setChangedAndUpdate();

        if (recipe.getDuration() <= 0){
            SelenicFigure figure = SelenicFigureScanner.scan(level, worldPosition);
            ArmillaryCrownBlockEntity crown = getCrown(level, figure);

            if (figure.valid() && crown != null){
                completeRecipe(level, recipe, figure, crown);
            }else {
                stopInterrupted();
            }
        }
    }

    private void tickActiveRecipe() {
        Level level = this.level;

        if (level == null || level.isClientSide || activeRecipeId == null){
            return;
        }

        SelenicFigure figure = SelenicFigureScanner.scan(level, worldPosition);

        if (!figure.valid()){
            stopInterrupted();
            return;
        }

        ArmillaryCrownBlockEntity crown = getCrown(level, figure);

        if (crown == null){
            stopInterrupted();
            return;
        }

        Optional<SelenicAstrolabeRecipe> recipe = getActiveRecipe(level);

        if (recipe.isEmpty()){
            stopInterrupted();
            return;
        }

        SelenicAstrolabeRecipe active = recipe.get();

        updateCachedFigure(level, figure);
        trimOutputToCurrentCapacity();

        if (!active.matches(createContext(level, figure, crown)) || !canPrepareRecipe(level, active, figure, crown)){
            stopInterrupted();
            return;
        }

        progress += getProgressStep();

        if (progress >= active.getDuration()){
            completeRecipe(level, active, figure, crown);
        }

        setChanged();
    }

    private Optional<SelenicAstrolabeRecipe> getActiveRecipe(Level level) {
        return SelenicRecipeCache.getSortedRecipes(level)
                                 .stream()
                                 .filter(candidate -> candidate.getId().equals(activeRecipeId))
                                 .findFirst();
    }

    private float getProgressStep() {
        return 1.0F + 0.3F * Mth.sqrt((float) cachedTestimonyCount);
    }

    private void stopInterrupted() {
        activeRecipeId = null;
        progress = 0.0F;
        lastFailure = SelenicFailure.INTERRUPTED;

        setActive(false);
        setChangedAndUpdate();
    }

    private void completeRecipe(
            Level level,
            SelenicAstrolabeRecipe recipe,
            SelenicFigure figure,
            ArmillaryCrownBlockEntity crown) {
        SelenicRecipeAccess access = createRecipeAccess(level, figure, crown);
        PreparedSelenicRecipe prepared = recipe.prepareRecipe(access, level.random);

        if (!prepared.isOk()){
            lastFailure = prepared.failure();
            stopInterrupted();
            return;
        }

        prepared.apply(recipe, access);

        activeRecipeId = null;
        progress = 0.0F;
        lastFailure = SelenicFailure.NONE;

        setActive(false);
        pulseSuccessSignal();
        setChangedAndUpdate();
    }

    private boolean canPrepareRecipe(
            Level level,
            SelenicAstrolabeRecipe recipe,
            SelenicFigure figure,
            ArmillaryCrownBlockEntity crown) {
        return checkRecipe(level, recipe, figure, crown).isOk();
    }

    private RecipeCheck checkRecipe(
            Level level,
            SelenicAstrolabeRecipe recipe,
            SelenicFigure figure,
            ArmillaryCrownBlockEntity crown) {
        SelenicRequirementCheck requirement = recipe.checkRequirements(createContext(level, figure, crown));

        if (requirement != SelenicRequirementCheck.OK){
            return RecipeCheck.fail(failureOf(requirement));
        }

        PreparedSelenicRecipe prepared = recipe.prepareRecipe(
                createRecipeAccess(level, figure, crown),
                null);

        if (!prepared.isOk()){
            return RecipeCheck.fail(prepared.failure());
        }

        return RecipeCheck.ok();
    }

    public void scanFigureAndTrim() {
        Level level = this.level;

        if (level == null){
            return;
        }

        SelenicFigure figure = SelenicFigureScanner.scan(level, worldPosition);
        int oldLowerSpines = cachedLowerSpines;

        if (figure.valid()){
            updateCachedFigure(level, figure);
            trimCrownToCurrentCapacity(level, figure);
        }else {
            cachedLowerSpines = 0;
            cachedUpperSpines = 0;
            cachedTestimonyCount = 0;
        }

        if (cachedLowerSpines < oldLowerSpines){
            trimOutputToCurrentCapacity();
        }

        setChanged();
    }

    private void trimCrownToCurrentCapacity(Level level, SelenicFigure figure) {
        ArmillaryCrownBlockEntity crown = getCrown(level, figure);

        if (crown != null){
            crown.trimToCurrentCapacity();
        }
    }

    private void updateCachedFigure(Level level, SelenicFigure figure) {
        cachedLowerSpines = Math.max(0, figure.lowerSpines());
        cachedUpperSpines = Math.max(0, figure.upperSpines());
        cachedTestimonyCount = collectWitnesses(level, figure).size();
    }

    private void trimOutputToCurrentCapacity() {
        outputItems.dropExcessItems();
        outputTank.capToCapacity();
    }

    private SelenicAstrolabeContext createContext(
            Level level,
            SelenicFigure figure,
            ArmillaryCrownBlockEntity crown) {
        return new SelenicAstrolabeContext(
                figure.totalSpines(),
                MoonPhase.fromVanillaId(level.getMoonPhase()),
                level.isNight(),
                crown.copyInputStacks(),
                copyStacks(collectTestimonySources(level, figure)),
                crown.getInputFluid());
    }

    private SelenicRecipeAccess createRecipeAccess(
            Level level,
            SelenicFigure figure,
            ArmillaryCrownBlockEntity crown) {
        return new FontRecipeAccess(level, figure, crown);
    }

    private List<InputSource> collectTestimonySources(Level level, SelenicFigure figure) {
        List<InputSource> sources = new ArrayList<>();

        for (TestimonyStandBlockEntity stand : collectWitnesses(level, figure)) {
            sources.add(new InputSource(
                    stand.getTestimony(),
                    stack -> stand.setTestimony(normalize(stack))));
        }

        return sources;
    }

    private List<InputSource> collectCrownSources(ArmillaryCrownBlockEntity crown) {
        List<InputSource> sources = new ArrayList<>();

        for (int slot = 0; slot < crown.getInputSlotCount(); slot++) {
            int index = slot;

            sources.add(new InputSource(
                    crown.getInputStack(index),
                    stack -> crown.setInputStack(index, normalize(stack))));
        }

        return sources;
    }

    private List<TestimonyStandBlockEntity> collectWitnesses(Level level, SelenicFigure figure) {
        List<TestimonyStandBlockEntity> witnesses = new ArrayList<>();

        if (!figure.valid() || figure.crownPos() == null || figure.bottomPos() == null){
            return witnesses;
        }

        int topY = figure.crownPos().getY();
        int bottomY = figure.bottomPos().getY();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0){
                    continue;
                }

                TestimonyStandBlockEntity witness = findFirstWitnessAtOffset(level, dx, dz, topY, bottomY);

                if (witness != null){
                    witnesses.add(witness);
                }
            }
        }

        return witnesses;
    }

    @Nullable
    private TestimonyStandBlockEntity findFirstWitnessAtOffset(
            Level level,
            int dx,
            int dz,
            int topY,
            int bottomY) {
        for (int y = topY; y >= bottomY; y--) {
            BlockPos pos = new BlockPos(worldPosition.getX() + dx, y, worldPosition.getZ() + dz);

            if (level.getBlockEntity(pos) instanceof TestimonyStandBlockEntity stand && !stand.getTestimony().isEmpty()){
                return stand;
            }
        }

        return null;
    }

    @Nullable
    private ArmillaryCrownBlockEntity getCrown(Level level, SelenicFigure figure) {
        if (!figure.valid() || figure.crownPos() == null){
            return null;
        }

        BlockEntity be = level.getBlockEntity(figure.crownPos());
        return be instanceof ArmillaryCrownBlockEntity crown ? crown : null;
    }

    private List<ItemStack> copyStacks(List<InputSource> sources) {
        List<ItemStack> stacks = new ArrayList<>();

        for (InputSource source : sources) {
            stacks.add(source.stack().copy());
        }

        return stacks;
    }

    private ItemStack normalize(ItemStack stack) {
        return stack.isEmpty() || stack.getCount() <= 0 ? ItemStack.EMPTY : stack;
    }

    private void fail(SelenicFailure failure, @Nullable Player player) {
        lastFailure = failure;

        if (player != null){
            player.displayClientMessage(failure.component(), true);
        }

        setChanged();
    }

    private void setActive(boolean active) {
        if (level == null){
            return;
        }

        BlockState state = getBlockState();

        if (state.hasProperty(LunarFontBlock.ACTIVE) && state.getValue(LunarFontBlock.ACTIVE) != active){
            level.setBlock(worldPosition, state.setValue(LunarFontBlock.ACTIVE, active), 3);
        }
    }

    private void pulseSuccessSignal() {
        if (level == null){
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(LunarFontBlock.SIGNALING)){
            return;
        }

        level.setBlock(worldPosition, state.setValue(LunarFontBlock.SIGNALING, true), 3);
        level.scheduleTick(worldPosition, state.getBlock(), SUCCESS_SIGNAL_TICKS);
        level.updateNeighborsAt(worldPosition, state.getBlock());
    }

    public ItemStack extractOutputItem() {
        for (int i = outputItems.getSlots() - 1; i >= 0; i--) {
            ItemStack extracted = outputItems.extractItem(i, 64, false);

            if (!extracted.isEmpty()){
                return extracted;
            }
        }

        return ItemStack.EMPTY;
    }

    public void dropContents() {
        outputItems.dropAllItems();
    }

    public int getActiveItemSlots() {
        return Mth.clamp(1 + cachedLowerSpines, 1, MAX_ITEM_SLOTS);
    }

    public int getFluidCapacity() {
        return BASE_FLUID_CAPACITY + cachedLowerSpines * FLUID_CAPACITY_PER_SPINE;
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

        tag.put("OutputItems", outputItems.serializeNBT());
        tag.put("OutputTank", outputTank.writeToNBT(new CompoundTag()));
        tag.putInt("LowerSpines", cachedLowerSpines);
        tag.putInt("UpperSpines", cachedUpperSpines);
        tag.putInt("TestimonyCount", cachedTestimonyCount);
        tag.putBoolean("WasPowered", wasPowered);
        tag.putFloat("Progress", progress);
        tag.putString("LastFailure", lastFailure.name());

        if (activeRecipeId != null){
            tag.putString("ActiveRecipe", activeRecipeId.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        outputItems.deserializeNBT(tag.getCompound("OutputItems"));
        outputTank.readFromNBT(tag.getCompound("OutputTank"));
        cachedLowerSpines = tag.getInt("LowerSpines");
        cachedUpperSpines = tag.getInt("UpperSpines");
        cachedTestimonyCount = tag.getInt("TestimonyCount");
        wasPowered = tag.getBoolean("WasPowered");
        progress = tag.getFloat("Progress");
        activeRecipeId = tag.contains("ActiveRecipe") ? new ResourceLocation(tag.getString("ActiveRecipe")) : null;

        try {
            lastFailure = SelenicFailure.valueOf(tag.getString("LastFailure"));
        }
        catch (Exception ignored) {
            lastFailure = SelenicFailure.NONE;
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
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

    private SelenicFailure failureOf(SelenicRequirementCheck check) {
        return switch (check) {
            case WRONG_ELEVATION -> SelenicFailure.WRONG_ELEVATION;
            case DAYTIME -> SelenicFailure.DAYTIME;
            case WRONG_MOON_PHASE -> SelenicFailure.WRONG_MOON_PHASE;
            case CROWN_INPUT -> SelenicFailure.MISSING_CROWN_INPUT;
            case TESTIMONY -> SelenicFailure.MISSING_TESTIMONY;
            case MEDIUM -> SelenicFailure.MISSING_MEDIUM;
            case OK -> SelenicFailure.NONE;
        };
    }

    private class FontRecipeAccess implements SelenicRecipeAccess {
        private final Level level;
        private final SelenicFigure figure;
        private final ArmillaryCrownBlockEntity crown;

        private FontRecipeAccess(Level level, SelenicFigure figure, ArmillaryCrownBlockEntity crown) {
            this.level = level;
            this.figure = figure;
            this.crown = crown;
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public List<ItemStack> crownInputs() {
            return crown.copyInputStacks();
        }

        @Override
        public List<ItemStack> testimonyInputs() {
            return copyStacks(collectTestimonySources(level, figure));
        }

        @Override
        public FluidStack inputMedium() {
            return crown.getInputFluid();
        }

        @Override
        public int freeItemSpace(ItemStack stack) {
            return outputItems.getFreeSpaceFor(stack);
        }

        @Override
        public int freeFluidSpace(FluidStack stack) {
            return outputTank.getFreeSpaceFor(stack);
        }

        @Override
        public int countCrownInput(Ingredient ingredient) {
            if (SelenicAstrolabeRecipe.isIngredientEmpty(ingredient)){
                return 0;
            }

            int count = 0;

            for (ItemStack stack : crown.copyInputStacks()) {
                if (!stack.isEmpty() && ingredient.test(stack)){
                    count += stack.getCount();
                }
            }

            return count;
        }

        @Override
        public void consumeCrownInput(Ingredient ingredient) {
            if (SelenicAstrolabeRecipe.isIngredientEmpty(ingredient)){
                return;
            }

            List<InputSource> sources = collectCrownSources(crown);
            List<ItemStack> working = copyStacks(sources);

            if (!IngredientStackUtil.consumeOne(working, ingredient, false)){
                return;
            }

            for (int i = 0; i < sources.size(); i++) {
                sources.get(i).set(normalize(working.get(i)));
            }
        }

        @Override
        public void consumeCrownInput(Ingredient ingredient, int amount) {
            if (amount <= 0 || SelenicAstrolabeRecipe.isIngredientEmpty(ingredient)){
                return;
            }

            List<InputSource> sources = collectCrownSources(crown);
            int remaining = amount;

            for (InputSource source : sources) {
                if (remaining <= 0){
                    break;
                }

                ItemStack stack = source.stack();

                if (stack.isEmpty() || !ingredient.test(stack)){
                    continue;
                }

                ItemStack copy = stack.copy();
                int consumed = Math.min(copy.getCount(), remaining);

                copy.shrink(consumed);
                remaining -= consumed;
                source.set(normalize(copy));
            }
        }

        @Override
        public void consumeTestimonies(List<Ingredient> testimonies) {
            if (testimonies.isEmpty()){
                return;
            }

            List<InputSource> sources = collectTestimonySources(level, figure);
            List<ItemStack> working = copyStacks(sources);

            if (!IngredientStackUtil.consumeAll(working, testimonies, false)){
                return;
            }

            for (int i = 0; i < sources.size(); i++) {
                sources.get(i).set(normalize(working.get(i)));
            }
        }

        @Override
        public void drainMedium(int amount) {
            if (amount > 0){
                crown.drainInputFluid(amount);
            }
        }

        @Override
        public void insertItemOutput(ItemStack stack) {
            if (!stack.isEmpty()){
                outputItems.insertOutput(stack);
            }
        }

        @Override
        public void insertFluidOutput(FluidStack stack) {
            if (!stack.isEmpty()){
                outputTank.insertOutput(stack);
            }
        }
    }

    private record InputSource(ItemStack stack, Consumer<ItemStack> setter) {
        private void set(ItemStack stack) {
            setter.accept(stack);
        }
    }

    private record RecipeCheck(SelenicFailure failure) {
        private static RecipeCheck ok() {
            return new RecipeCheck(SelenicFailure.NONE);
        }

        private static RecipeCheck fail(SelenicFailure failure) {
            return new RecipeCheck(failure);
        }

        private boolean isOk() {
            return failure == SelenicFailure.NONE;
        }
    }

    private static class OutputItemHandler extends ItemStackHandler {
        private final LunarFontBlockEntity owner;

        private OutputItemHandler(LunarFontBlockEntity owner) {
            super(MAX_ITEM_SLOTS);
            this.owner = owner;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < owner.getActiveItemSlots() ? 64 : 0;
        }

        /**
         * 太阴泉槽是输出容器：外部不能向这里塞物品。
         * 配方内部输出走 insertOutput(...)。
         */
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= owner.getActiveItemSlots()){
                return ItemStack.EMPTY;
            }

            return super.extractItem(slot, amount, simulate);
        }

        private ItemStack insertOutput(ItemStack stack) {
            ItemStack remaining = stack.copy();

            for (int i = 0; i < owner.getActiveItemSlots(); i++) {
                if (remaining.isEmpty()){
                    break;
                }

                remaining = super.insertItem(i, remaining, false);
            }

            return remaining;
        }

        private int getFreeSpaceFor(ItemStack output) {
            if (output.isEmpty()){
                return 0;
            }

            ItemStack kind = getStoredKind();

            if (!kind.isEmpty() && !ItemStack.isSameItemSameTags(kind, output)){
                return 0;
            }

            int free = 0;

            for (int i = 0; i < owner.getActiveItemSlots(); i++) {
                ItemStack stack = getStackInSlot(i);

                if (stack.isEmpty()){
                    free += output.getMaxStackSize();
                }else if (ItemStack.isSameItemSameTags(stack, output)){
                    free += stack.getMaxStackSize() - stack.getCount();
                }
            }

            return Math.max(0, free);
        }

        private ItemStack getStoredKind() {
            for (int i = 0; i < owner.getActiveItemSlots(); i++) {
                ItemStack stack = getStackInSlot(i);

                if (!stack.isEmpty()){
                    return stack;
                }
            }

            return ItemStack.EMPTY;
        }

        private void dropExcessItems() {
            Level level = owner.level;

            if (level == null){
                return;
            }

            for (int i = owner.getActiveItemSlots(); i < getSlots(); i++) {
                ItemStack stack = getStackInSlot(i);

                if (!stack.isEmpty()){
                    Containers.dropItemStack(
                            level,
                            owner.worldPosition.getX() + 0.5D,
                            owner.worldPosition.getY() + 1.0D,
                            owner.worldPosition.getZ() + 0.5D,
                            stack);

                    setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }

        private void dropAllItems() {
            Level level = owner.level;

            if (level == null){
                return;
            }

            for (int i = 0; i < getSlots(); i++) {
                ItemStack stack = getStackInSlot(i);

                if (!stack.isEmpty()){
                    Containers.dropItemStack(
                            level,
                            owner.worldPosition.getX() + 0.5D,
                            owner.worldPosition.getY() + 1.0D,
                            owner.worldPosition.getZ() + 0.5D,
                            stack);

                    setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }

        @Override
        protected void onContentsChanged(int slot) {
            owner.setChangedAndUpdate();
        }
    }

    private static class OutputFluidTank extends FluidTank {
        private final LunarFontBlockEntity owner;

        private OutputFluidTank(LunarFontBlockEntity owner) {
            super(BASE_FLUID_CAPACITY);
            this.owner = owner;
        }

        @Override
        public int getCapacity() {
            return owner.getFluidCapacity();
        }

        /**
         * 太阴泉槽是输出流体槽：外部不能 fill。
         * 外部抽取仍然允许，走 FluidTank 默认 drain。
         */
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        private int insertOutput(FluidStack stack) {
            return super.fill(stack, FluidAction.EXECUTE);
        }

        private int getFreeSpaceFor(FluidStack output) {
            if (output.isEmpty()){
                return 0;
            }

            FluidStack stored = getFluid();

            if (!stored.isEmpty() && !stored.isFluidEqual(output)){
                return 0;
            }

            return Math.max(0, getCapacity() - stored.getAmount());
        }

        private void capToCapacity() {
            if (!fluid.isEmpty() && fluid.getAmount() > getCapacity()){
                FluidStack capped = fluid.copy();
                capped.setAmount(getCapacity());
                setFluid(capped);
            }
        }

        @Override
        protected void onContentsChanged() {
            owner.setChangedAndUpdate();
        }
    }
}