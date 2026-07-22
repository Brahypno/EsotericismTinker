package org.brahypno.esotericismtinker.smeltery.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.smeltery.recipe.entitymelting.ByproductEntityMeltingRecipe;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ByproductEntityMeltingRecipeBuilder extends AbstractRecipeBuilder<ByproductEntityMeltingRecipeBuilder> {
  private final EntityIngredient ingredient;
  private final FluidOutput output;
  private int damage;
  private final List<FluidOutput> byproducts = new ArrayList<>();
  private int priority;

  private ByproductEntityMeltingRecipeBuilder(EntityIngredient ingredient, FluidOutput output, int damage) {
    this.ingredient = ingredient;
    this.output = output;
    this.damage = damage;
  }

  public static ByproductEntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidOutput output, int damage) {
    return new ByproductEntityMeltingRecipeBuilder(ingredient, output, damage);
  }

  public static ByproductEntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidStack output, int damage) {
    return melting(ingredient, FluidOutput.fromStack(output), damage);
  }
  public static ByproductEntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidOutput output) {
    return melting(ingredient, output, 2);
  }
  public static ByproductEntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidStack output) {
    return melting(ingredient, output, 2);
  }
  public ByproductEntityMeltingRecipeBuilder addByproduct(FluidOutput output) {
    byproducts.add(output);
    return this;
  }
  public ByproductEntityMeltingRecipeBuilder addByproduct(FluidStack output) {
    return addByproduct(FluidOutput.fromStack(output));
  }
  public ByproductEntityMeltingRecipeBuilder byproduct(FluidStack output) {
    return addByproduct(output);
  }
  public ByproductEntityMeltingRecipeBuilder damage(int damage) {
    if (damage < 1) throw new IllegalArgumentException("Damage must be positive");
    this.damage = damage;
    return this;
  }
  public ByproductEntityMeltingRecipeBuilder priority(int priority) {
    if (priority < 0) throw new IllegalArgumentException("Priority must be non-negative");
    this.priority = priority;
    return this;
  }
  @Override public void save(Consumer<FinishedRecipe> consumer) {
    save(consumer, BuiltInRegistries.FLUID.getKey(output.get().getFluid()));
  }
  @Override public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
    if (byproducts.isEmpty()) throw new IllegalStateException("Byproduct entity melting recipe requires at least one byproduct: " + id);
    ResourceLocation advancementId = buildOptionalAdvancement(id, "byproduct_entity_melting");
    consumer.accept(new LoadableFinishedRecipe<>(
        new ByproductEntityMeltingRecipe(id, ingredient, output, damage, List.copyOf(byproducts), priority),
        ByproductEntityMeltingRecipe.LOADER, advancementId));
  }
}
