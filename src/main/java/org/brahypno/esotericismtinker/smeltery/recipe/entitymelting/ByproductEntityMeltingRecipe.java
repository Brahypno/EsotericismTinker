package org.brahypno.esotericismtinker.smeltery.recipe.entitymelting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICustomOutputRecipe;
import slimeknights.mantle.recipe.container.IEmptyContainer;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class ByproductEntityMeltingRecipe implements ICustomOutputRecipe<IEmptyContainer> {
  private static final LoadableField<List<FluidOutput>, ByproductEntityMeltingRecipe> BYPRODUCTS =
      FluidOutput.Loadable.REQUIRED.list(1).requiredField("byproducts", recipe -> recipe.byproducts);

  public static final RecordLoadable<ByproductEntityMeltingRecipe> LOADER = RecordLoadable.create(
      ContextKey.ID.requiredField(),
      EntityIngredient.LOADABLE.requiredField("entity", recipe -> recipe.ingredient),
      FluidOutput.Loadable.REQUIRED.requiredField("result", recipe -> recipe.output),
      IntLoadable.FROM_ONE.defaultField("damage", 2, true, recipe -> recipe.damage),
      BYPRODUCTS,
      IntLoadable.FROM_ZERO.defaultField("priority", 0, true, recipe -> recipe.priority),
      ByproductEntityMeltingRecipe::new
  );

  private final ResourceLocation id;
  private final EntityIngredient ingredient;
  private final FluidOutput output;
  private final int damage;
  private final List<FluidOutput> byproducts;
  private final int priority;
  private List<List<FluidStack>> outputWithByproducts;

  public ByproductEntityMeltingRecipe(ResourceLocation id, EntityIngredient ingredient, FluidOutput output,
                                      int damage, List<FluidOutput> byproducts, int priority) {
    if (byproducts.isEmpty()) throw new IllegalArgumentException("Byproduct entity melting recipes require at least one byproduct");
    this.id = id;
    this.ingredient = ingredient;
    this.output = output;
    this.damage = damage;
    this.byproducts = List.copyOf(byproducts);
    this.priority = priority;
  }

  public boolean matches(EntityType<?> type) { return ingredient.test(type); }
  public ResourceLocation getId() { return id; }
  public EntityIngredient getIngredient() { return ingredient; }
  public int getDamage() { return damage; }
  public int getPriority() { return priority; }
  public FluidStack getOutput() { return output.get(); }
  public FluidStack getOutput(LivingEntity entity) { return output.copy(); }
  public List<FluidStack> getByproducts(LivingEntity entity) { return byproducts.stream().map(FluidOutput::copy).toList(); }

  public List<List<FluidStack>> getOutputWithByproducts() {
    if (outputWithByproducts == null) {
      outputWithByproducts = Stream.concat(Stream.of(output), byproducts.stream())
          .map(fluid -> List.of(fluid.get())).toList();
    }
    return outputWithByproducts;
  }

  public Collection<EntityType<?>> getInputs() { return ingredient.getTypes(); }
  @Override public RecipeSerializer<?> getSerializer() { return ByproductEntityMeltingRecipeRegistry.SERIALIZER.get(); }
  @Override public RecipeType<?> getType() { return ByproductEntityMeltingRecipeRegistry.TYPE.get(); }

  @Deprecated
  @Override public boolean matches(IEmptyContainer inv, Level level) { return false; }
}
