package org.brahypno.esotericismtinker.smeltery.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.smeltery.recipe.melting.TransmuteMeltingRecipe;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.FluidOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Generates recipes exclusively for {@code esotericism_tinker:transmute_v2}. */
public class TransmuteMeltingRecipeBuilder extends AbstractRecipeBuilder<TransmuteMeltingRecipeBuilder> {
    private final Ingredient input;
    private final FluidOutput output;
    private final int temperature;
    private final int time;
    private final List<FluidOutput> byproducts = new ArrayList<>();

    private TransmuteMeltingRecipeBuilder(Ingredient input, FluidOutput output, int temperature, int time) {
        this.input = input;
        this.output = output;
        this.temperature = temperature;
        this.time = time;
    }

    /**
     * Creates a recipe with an explicit temperature and melting time.
     *
     * @param input       item or tag ingredient to melt
     * @param output      main fluid output
     * @param temperature required temperature, at least zero
     * @param time        positive melting steps, using Tinkers' units rather than game ticks
     */
    public static TransmuteMeltingRecipeBuilder melting(Ingredient input, FluidOutput output, int temperature, int time) {
        if (temperature < 0) throw new IllegalArgumentException("Temperature must be non-negative");
        if (time <= 0) throw new IllegalArgumentException("Melting time must be positive");
        return new TransmuteMeltingRecipeBuilder(input, output, temperature, time);
    }

    /** Creates a recipe from a fluid stack, including its amount and NBT. */
    public static TransmuteMeltingRecipeBuilder melting(Ingredient input, FluidStack output, int temperature, int time) {
        return melting(input, FluidOutput.fromStack(output), temperature, time);
    }

    /** Adds a fluid byproduct; may be called multiple times. */
    public TransmuteMeltingRecipeBuilder addByproduct(FluidOutput output) {
        byproducts.add(output);
        return this;
    }

    /** Adds a fluid stack byproduct, including its amount and NBT. */
    public TransmuteMeltingRecipeBuilder addByproduct(FluidStack output) {
        return addByproduct(FluidOutput.fromStack(output));
    }

    /**
     * Saves using the output fluid's registry ID, following Tinkers' melting builder convention.
     * Prefer {@link #save(Consumer, ResourceLocation)} to use your own mod's namespace and recipe path.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void save(Consumer<FinishedRecipe> consumer) {
        save(consumer, BuiltInRegistries.FLUID.getKey(output.get().getFluid()));
    }

    /** Saves the v2 recipe under the supplied ID, with an optional advancement if unlockedBy was used. */
    @Override
    public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
        ResourceLocation advancementId = buildOptionalAdvancement(id, "transmute_v2");
        consumer.accept(new LoadableFinishedRecipe<>(
                new TransmuteMeltingRecipe(id, group, input, output, temperature, time, List.copyOf(byproducts)),
                TransmuteMeltingRecipe.LOADER, advancementId));
    }
}
