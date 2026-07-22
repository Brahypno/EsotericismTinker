package org.brahypno.esotericismtinker.smeltery.recipe.entitymelting;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinker;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;

public final class ByproductEntityMeltingRecipeRegistry {
  public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
      DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, EsotericismTinker.MODID);
  public static final DeferredRegister<RecipeType<?>> TYPES =
      DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, EsotericismTinker.MODID);
  public static final RegistryObject<RecipeSerializer<ByproductEntityMeltingRecipe>> SERIALIZER =
      SERIALIZERS.register("byproduct_entity_melting", () -> LoadableRecipeSerializer.of(ByproductEntityMeltingRecipe.LOADER));
  public static final RegistryObject<RecipeType<ByproductEntityMeltingRecipe>> TYPE =
      TYPES.register("byproduct_entity_melting", () -> new RecipeType<>() {
        @Override public String toString() { return EsotericismTinker.MODID + ":byproduct_entity_melting"; }
      });
  private ByproductEntityMeltingRecipeRegistry() {}
}
