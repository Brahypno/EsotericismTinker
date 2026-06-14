package org.brahypno.esotericismtinker.library.recipe.selenic.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.library.recipe.*;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicAstrolabeRecipe;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.data.loadable.common.FluidStackLoadable;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

public class SelenicAstrolabeRecipeBuilder {
    protected final List<Ingredient> testimonies = new ArrayList<>();

    protected Ingredient input = Ingredient.EMPTY;
    protected int priority = 0;
    protected int duration = 200;
    protected IntRange elevation = IntRange.ANY;
    protected EnumSet<MoonPhase> phases = EnumSet.allOf(MoonPhase.class);
    protected FluidIngredient medium = FluidIngredient.EMPTY;
    protected FluidStack mediumOutput = FluidStack.EMPTY;
    protected FluidOutputMode mediumOutputMode = FluidOutputMode.INSTANT;
    protected boolean consumeMedium = false;
    protected RitualItemOutput output = RitualItemOutput.EMPTY;

    public static SelenicAstrolabeRecipeBuilder item(ItemStack output) {
        return new SelenicAstrolabeRecipeBuilder().output(output);
    }

    public SelenicAstrolabeRecipeBuilder input(Ingredient ingredient) {
        this.input = ingredient;
        return this;
    }

    public SelenicAstrolabeRecipeBuilder testimony(Ingredient ingredient) {
        testimonies.add(ingredient);
        return this;
    }

    public SelenicAstrolabeRecipeBuilder testimonies(Ingredient... ingredients) {
        for (Ingredient ingredient : ingredients) {
            testimony(ingredient);
        }

        return this;
    }

    /**
     * 兼容旧调用；现在 inputs 表示征象座输入。
     */
    @Deprecated
    public SelenicAstrolabeRecipeBuilder inputs(Ingredient... ingredients) {
        return testimonies(ingredients);
    }

    public SelenicAstrolabeRecipeBuilder output(ItemStack stack) {
        this.output = new RitualItemOutput(stack);
        return this;
    }

    public SelenicAstrolabeRecipeBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public SelenicAstrolabeRecipeBuilder duration(int duration) {
        this.duration = duration;
        return this;
    }

    public SelenicAstrolabeRecipeBuilder elevation(int min, int max) {
        this.elevation = new IntRange(min, max);
        return this;
    }

    public SelenicAstrolabeRecipeBuilder elevationAtLeast(int min) {
        this.elevation = new IntRange(min, Integer.MAX_VALUE);
        return this;
    }

    public SelenicAstrolabeRecipeBuilder phases(MoonPhase first, MoonPhase... rest) {
        this.phases = EnumSet.of(first, rest);
        return this;
    }

    public SelenicAstrolabeRecipeBuilder medium(FluidIngredient medium) {
        this.medium = medium;
        return this;
    }

    public SelenicAstrolabeRecipeBuilder mediumOutput(FluidStack stack, FluidOutputMode mode) {
        this.mediumOutput = stack;
        this.mediumOutputMode = mode;
        return this;
    }

    public SelenicAstrolabeRecipeBuilder consumeMedium() {
        return consumeMedium(true);
    }

    public SelenicAstrolabeRecipeBuilder consumeMedium(boolean consumeMedium) {
        this.consumeMedium = consumeMedium;
        return this;
    }


    public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
        validate();
        consumer.accept(new Result(id, this));
    }

    protected RecipeSerializer<?> getSerializer() {
        return EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_SERIALIZER.get();
    }

    protected void writeExtra(JsonObject json) {}

    protected void validate() {
        if (medium == FluidIngredient.EMPTY || mediumOutput.isEmpty() || consumeMedium){
            return;
        }

        if (medium.getAmount(mediumOutput.getFluid()) > 0){
            return;
        }

        throw new IllegalStateException(
                "A non-consuming medium input cannot produce a different medium output. " +
                "Use consumeMedium(true), remove mediumOutput, or make mediumOutput match medium.");
    }

    protected record Result(ResourceLocation id, SelenicAstrolabeRecipeBuilder builder) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            builder.validate();

            if (builder.priority != 0){
                json.addProperty("priority", builder.priority);
            }

            json.addProperty("duration", builder.duration);

            if (builder.elevation != IntRange.ANY){
                json.add("elevation", builder.elevation.toJson());
            }

            writePhases(json);
            writeInput(json);
            writeTestimonies(json);

            if (builder.medium != FluidIngredient.EMPTY){
                json.add("medium", builder.medium.serialize());
            }

            if (!builder.mediumOutput.isEmpty()){
                JsonObject fluidJson = FluidStackLoadable.REQUIRED_STACK_NBT
                        .serialize(builder.mediumOutput)
                        .getAsJsonObject();

                fluidJson.addProperty("mode", builder.mediumOutputMode.serializedName());
                json.add("medium_output", fluidJson);
            }

            if (builder.consumeMedium){
                json.addProperty("consume_medium", true);
            }

            if (!builder.output.isEmpty()){
                json.add("output", builder.output.toJson());
            }

            builder.writeExtra(json);
        }

        private void writePhases(JsonObject json) {
            if (builder.phases.size() == MoonPhase.values().length){
                return;
            }

            JsonArray array = new JsonArray();

            for (MoonPhase phase : builder.phases) {
                array.add(phase.serializedName());
            }

            json.add("lunar_phase", array);
        }

        private void writeInput(JsonObject json) {
            if (SelenicAstrolabeRecipe.isIngredientEmpty(builder.input)){
                return;
            }

            json.add("input", builder.input.toJson());
        }

        private void writeTestimonies(JsonObject json) {
            if (builder.testimonies.isEmpty()){
                return;
            }

            JsonArray array = new JsonArray();

            for (Ingredient testimony : builder.testimonies) {
                array.add(testimony.toJson());
            }

            json.add("testimonies", array);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return builder.getSerializer();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @Nullable
        @Override
        public @NotNull ResourceLocation getAdvancementId() {
            return null;
        }
    }
}