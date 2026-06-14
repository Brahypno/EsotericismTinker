package org.brahypno.esotericismtinker.library.recipe.selenic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.library.recipe.*;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.common.FluidStackLoadable;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class SelenicAstrolabeRecipe implements Recipe<Container> {
    protected final ResourceLocation id;
    protected final int priority;
    protected final int duration;
    protected final IntRange elevation;
    protected final EnumSet<MoonPhase> lunarPhases;
    protected final Ingredient input;
    protected final List<Ingredient> testimonies;
    protected final FluidIngredient medium;
    protected final FluidStack mediumOutput;
    protected final FluidOutputMode mediumOutputMode;
    protected final RitualItemOutput output;
    protected final boolean consumeMedium;

    public SelenicAstrolabeRecipe(
            ResourceLocation id,
            int priority,
            int duration,
            IntRange elevation,
            EnumSet<MoonPhase> lunarPhases,
            Ingredient input,
            List<Ingredient> testimonies,
            FluidIngredient medium,
            FluidStack mediumOutput,
            FluidOutputMode mediumOutputMode,
            RitualItemOutput output,
            boolean consumeMedium) {
        this.id = id;
        this.priority = priority;
        this.duration = duration;
        this.elevation = elevation == null ? IntRange.ANY : elevation;
        this.lunarPhases = lunarPhases == null || lunarPhases.isEmpty()
                           ? EnumSet.allOf(MoonPhase.class)
                           : EnumSet.copyOf(lunarPhases);
        this.input = input == null ? Ingredient.EMPTY : input;
        this.testimonies = testimonies == null ? List.of() : List.copyOf(testimonies);
        this.medium = medium == null ? FluidIngredient.EMPTY : medium;
        this.mediumOutput = mediumOutput == null ? FluidStack.EMPTY : mediumOutput.copy();
        this.mediumOutputMode = mediumOutputMode == null ? FluidOutputMode.INSTANT : mediumOutputMode;
        this.output = output == null ? RitualItemOutput.EMPTY : output;
        this.consumeMedium = consumeMedium;

        validateMediumTransition(id, this.medium, this.mediumOutput, this.consumeMedium);
    }

    public boolean matches(SelenicAstrolabeContext context) {
        return checkRequirements(context) == SelenicRequirementCheck.OK;
    }

    protected boolean matchesInput(List<ItemStack> stacks) {
        if (isIngredientEmpty(input)){
            return true;
        }

        return IngredientStackUtil.consumeOne(copyStacks(stacks), input, false);
    }

    protected boolean matchesTestimonies(List<ItemStack> stacks) {
        return IngredientStackUtil.matchesAll(stacks, testimonies);
    }

    protected boolean matchesMedium(FluidStack stack) {
        return medium == FluidIngredient.EMPTY || medium.test(stack);
    }

    public PreparedSelenicRecipe prepareRecipe(SelenicRecipeAccess access, @Nullable RandomSource random) {
        FluidStack fluidOutput = prepareFluidOutput(access);

        if (!mediumOutput.isEmpty() && fluidOutput.isEmpty()){
            return PreparedSelenicRecipe.fail(SelenicFailure.OUTPUT_FLUID_BLOCKED);
        }

        ItemStack itemOutput = prepareItemOutput(access, random);

        if (!getDisplayOutputs().isEmpty() && itemOutput.isEmpty()){
            return PreparedSelenicRecipe.fail(SelenicFailure.OUTPUT_ITEM_BLOCKED);
        }

        return PreparedSelenicRecipe.ok(itemOutput, fluidOutput);
    }

    protected ItemStack prepareItemOutput(SelenicRecipeAccess access, @Nullable RandomSource random) {
        List<ItemStack> candidates = getDisplayOutputs();

        if (candidates.isEmpty()){
            return ItemStack.EMPTY;
        }

        List<ItemStack> fitting = new ArrayList<>();

        for (ItemStack candidate : candidates) {
            if (!candidate.isEmpty() && access.freeItemSpace(candidate) > 0){
                fitting.add(candidate);
            }
        }

        if (fitting.isEmpty()){
            return ItemStack.EMPTY;
        }

        ItemStack chosen = random == null
                           ? fitting.get(0).copy()
                           : fitting.get(random.nextInt(fitting.size())).copy();

        int amount = Math.min(chosen.getCount(), access.freeItemSpace(chosen));

        if (amount <= 0){
            return ItemStack.EMPTY;
        }

        chosen.setCount(amount);
        return chosen;
    }

    protected FluidStack prepareFluidOutput(SelenicRecipeAccess access) {
        if (mediumOutput.isEmpty()){
            return FluidStack.EMPTY;
        }

        int amount = Math.min(mediumOutput.getAmount(), access.freeFluidSpace(mediumOutput));

        if (amount <= 0){
            return FluidStack.EMPTY;
        }

        FluidStack actual = mediumOutput.copy();
        actual.setAmount(amount);
        return actual;
    }

    public ItemStack getItemOutput() {
        return output.createStack();
    }

    public ItemStack createItemOutput(RandomSource random) {
        return getItemOutput();
    }

    public List<ItemStack> getDisplayOutputs() {
        ItemStack stack = getItemOutput();
        return stack.isEmpty() ? List.of() : List.of(stack);
    }

    public Ingredient getInput() {
        return input;
    }

    public List<Ingredient> getTestimonies() {
        return testimonies;
    }

    public IntRange getElevation() {
        return elevation;
    }

    public EnumSet<MoonPhase> getLunarPhases() {
        return EnumSet.copyOf(lunarPhases);
    }

    public FluidIngredient getMedium() {
        return medium;
    }

    public FluidStack getMediumOutput() {
        return mediumOutput.copy();
    }

    public FluidOutputMode getMediumOutputMode() {
        return mediumOutputMode;
    }

    public boolean shouldConsumeMedium() {
        return consumeMedium;
    }

    public int getRequiredMediumAmount(FluidStack stack) {
        if (medium == FluidIngredient.EMPTY || stack.isEmpty() || !medium.test(stack)){
            return 0;
        }

        return medium.getAmount(stack.getFluid());
    }

    public int getPriority() {
        return priority;
    }

    public int getSpecificity() {
        return (isIngredientEmpty(input) ? 0 : 1) + testimonies.size();
    }

    public int getDuration() {
        return duration;
    }

    public boolean hasMoonRestriction() {
        return lunarPhases.size() < MoonPhase.values().length;
    }

    @Override
    public boolean matches(Container container, net.minecraft.world.level.Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return getItemOutput();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return getItemOutput();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_TYPE.get();
    }

    public SelenicRequirementCheck checkRequirements(SelenicAstrolabeContext context) {
        if (!elevation.contains(context.elevation())){
            return SelenicRequirementCheck.WRONG_ELEVATION;
        }

        if (hasMoonRestriction()){
            if (!context.night()){
                return SelenicRequirementCheck.DAYTIME;
            }

            if (!lunarPhases.contains(context.moonPhase())){
                return SelenicRequirementCheck.WRONG_MOON_PHASE;
            }
        }

        if (!matchesInput(context.crownInputs())){
            return SelenicRequirementCheck.CROWN_INPUT;
        }

        if (!matchesTestimonies(context.testimonyInputs())){
            return SelenicRequirementCheck.TESTIMONY;
        }

        if (!matchesMedium(context.medium())){
            return SelenicRequirementCheck.MEDIUM;
        }

        return SelenicRequirementCheck.OK;
    }

    public static CoreData readCore(ResourceLocation id, JsonObject json) {
        int priority = GsonHelper.getAsInt(json, "priority", 0);
        int duration = GsonHelper.getAsInt(json, "duration", 200);
        IntRange elevation = json.has("elevation")
                             ? IntRange.fromJson(GsonHelper.getAsJsonObject(json, "elevation"))
                             : IntRange.ANY;
        EnumSet<MoonPhase> phases = readPhases(json);
        Ingredient input = readInput(json);
        List<Ingredient> testimonies = readTestimonies(json);
        FluidIngredient medium = json.has("medium")
                                 ? FluidIngredient.LOADABLE.getIfPresent(json, "medium")
                                 : FluidIngredient.EMPTY;

        FluidStack mediumOutput = FluidStack.EMPTY;
        FluidOutputMode mediumOutputMode = FluidOutputMode.INSTANT;

        if (json.has("medium_output")){
            JsonObject fluidJson = GsonHelper.getAsJsonObject(json, "medium_output");
            mediumOutput = FluidStackLoadable.REQUIRED_STACK_NBT.convert(fluidJson, "medium_output");
            mediumOutputMode = FluidOutputMode.byName(GsonHelper.getAsString(fluidJson, "mode", "instant"));
        }

        RitualItemOutput output = json.has("output")
                                  ? RitualItemOutput.fromJson(GsonHelper.getAsJsonObject(json, "output"))
                                  : RitualItemOutput.EMPTY;
        boolean consumeMedium = GsonHelper.getAsBoolean(json, "consume_medium", false);

        validateMediumTransition(id, medium, mediumOutput, consumeMedium);

        return new CoreData(
                id,
                priority,
                duration,
                elevation,
                phases,
                input,
                testimonies,
                medium,
                mediumOutput,
                mediumOutputMode,
                output,
                consumeMedium);
    }

    protected static CoreData readCoreNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        int priority = buffer.readVarInt();
        int duration = buffer.readVarInt();
        IntRange elevation = IntRange.fromNetwork(buffer);

        int phaseMask = buffer.readVarInt();
        EnumSet<MoonPhase> phases = EnumSet.noneOf(MoonPhase.class);

        for (MoonPhase phase : MoonPhase.values()) {
            if ((phaseMask & (1 << phase.ordinal())) != 0){
                phases.add(phase);
            }
        }

        Ingredient input = Ingredient.fromNetwork(buffer);

        int testimonySize = buffer.readVarInt();
        List<Ingredient> testimonies = new ArrayList<>();

        for (int i = 0; i < testimonySize; i++) {
            testimonies.add(Ingredient.fromNetwork(buffer));
        }

        FluidIngredient medium = buffer.readBoolean()
                                 ? FluidIngredient.LOADABLE.decode(buffer)
                                 : FluidIngredient.EMPTY;
        FluidStack mediumOutput = FluidStackLoadable.OPTIONAL_STACK_NBT.decode(buffer);
        FluidOutputMode mediumOutputMode = buffer.readEnum(FluidOutputMode.class);
        RitualItemOutput output = RitualItemOutput.fromNetwork(buffer);
        boolean consumeMedium = buffer.readBoolean();

        return new CoreData(
                id,
                priority,
                duration,
                elevation,
                phases,
                input,
                testimonies,
                medium,
                mediumOutput,
                mediumOutputMode,
                output,
                consumeMedium);
    }

    protected static void writeCoreNetwork(FriendlyByteBuf buffer, SelenicAstrolabeRecipe recipe) {
        buffer.writeVarInt(recipe.priority);
        buffer.writeVarInt(recipe.duration);
        recipe.elevation.toNetwork(buffer);

        int phaseMask = 0;

        for (MoonPhase phase : recipe.lunarPhases) {
            phaseMask |= 1 << phase.ordinal();
        }

        buffer.writeVarInt(phaseMask);

        recipe.input.toNetwork(buffer);

        buffer.writeVarInt(recipe.testimonies.size());

        for (Ingredient testimony : recipe.testimonies) {
            testimony.toNetwork(buffer);
        }

        buffer.writeBoolean(recipe.medium != FluidIngredient.EMPTY);

        if (recipe.medium != FluidIngredient.EMPTY){
            FluidIngredient.LOADABLE.encode(buffer, recipe.medium);
        }

        FluidStackLoadable.OPTIONAL_STACK_NBT.encode(buffer, recipe.mediumOutput);
        buffer.writeEnum(recipe.mediumOutputMode);
        recipe.output.toNetwork(buffer);
        buffer.writeBoolean(recipe.consumeMedium);
    }

    private static EnumSet<MoonPhase> readPhases(JsonObject json) {
        if (!json.has("lunar_phase")){
            return EnumSet.allOf(MoonPhase.class);
        }

        EnumSet<MoonPhase> phases = EnumSet.noneOf(MoonPhase.class);
        JsonArray array = GsonHelper.getAsJsonArray(json, "lunar_phase");

        for (JsonElement element : array) {
            phases.add(MoonPhase.byName(element.getAsString()));
        }

        return phases.isEmpty() ? EnumSet.allOf(MoonPhase.class) : phases;
    }

    private static Ingredient readInput(JsonObject json) {
        return json.has("input")
               ? Ingredient.fromJson(GsonHelper.getNonNull(json, "input"))
               : Ingredient.EMPTY;
    }

    private static List<Ingredient> readTestimonies(JsonObject json) {
        List<Ingredient> testimonies = new ArrayList<>();

        if (!json.has("testimonies")){
            return testimonies;
        }

        for (JsonElement element : GsonHelper.getAsJsonArray(json, "testimonies")) {
            testimonies.add(Ingredient.fromJson(element));
        }

        return testimonies;
    }

    protected static void validateMediumTransition(
            ResourceLocation id, FluidIngredient medium,
            FluidStack mediumOutput, boolean consumeMedium) {
        if (consumeMedium || medium == FluidIngredient.EMPTY || mediumOutput.isEmpty()){
            return;
        }

        if (medium.getAmount(mediumOutput.getFluid()) > 0){
            return;
        }

        throw new IllegalArgumentException(
                "Selenic recipe " + id + " has a non-consuming medium input and a different medium_output. " +
                "Either set consume_medium=true or make medium_output match medium.");
    }

    public static boolean isIngredientEmpty(Ingredient ingredient) {
        return ingredient == Ingredient.EMPTY || ingredient.getItems().length == 0;
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        List<ItemStack> copy = new ArrayList<>();

        for (ItemStack stack : stacks) {
            copy.add(stack.copy());
        }

        return copy;
    }

    public record CoreData(
            ResourceLocation id,
            int priority,
            int duration,
            IntRange elevation,
            EnumSet<MoonPhase> lunarPhases,
            Ingredient input,
            List<Ingredient> testimonies,
            FluidIngredient medium,
            FluidStack mediumOutput,
            FluidOutputMode mediumOutputMode,
            RitualItemOutput output,
            boolean consumeMedium
    ) {}

    public static class Serializer implements RecipeSerializer<SelenicAstrolabeRecipe> {
        @Override
        public SelenicAstrolabeRecipe fromJson(ResourceLocation id, JsonObject json) {
            CoreData data = readCore(id, json);

            return new SelenicAstrolabeRecipe(
                    data.id(),
                    data.priority(),
                    data.duration(),
                    data.elevation(),
                    data.lunarPhases(),
                    data.input(),
                    data.testimonies(),
                    data.medium(),
                    data.mediumOutput(),
                    data.mediumOutputMode(),
                    data.output(),
                    data.consumeMedium());
        }

        @Override
        public SelenicAstrolabeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            CoreData data = readCoreNetwork(id, buffer);

            return new SelenicAstrolabeRecipe(
                    data.id(),
                    data.priority(),
                    data.duration(),
                    data.elevation(),
                    data.lunarPhases(),
                    data.input(),
                    data.testimonies(),
                    data.medium(),
                    data.mediumOutput(),
                    data.mediumOutputMode(),
                    data.output(),
                    data.consumeMedium());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SelenicAstrolabeRecipe recipe) {
            writeCoreNetwork(buffer, recipe);
        }
    }
}