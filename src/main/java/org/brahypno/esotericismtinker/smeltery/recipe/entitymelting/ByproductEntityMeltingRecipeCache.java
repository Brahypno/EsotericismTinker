package org.brahypno.esotericismtinker.smeltery.recipe.entitymelting;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.RecipeManager;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class ByproductEntityMeltingRecipeCache {
  private static final Map<EntityType<?>, ByproductEntityMeltingRecipe> CACHE = new HashMap<>();

  static { RecipeCacheInvalidator.addReloadListener(client -> CACHE.clear()); }

  private ByproductEntityMeltingRecipeCache() {}

  @Nullable
  public static ByproductEntityMeltingRecipe findRecipe(RecipeManager manager, EntityType<?> type) {
    if (CACHE.containsKey(type)) return CACHE.get(type);
    ByproductEntityMeltingRecipe best = null;
    for (ByproductEntityMeltingRecipe recipe : RecipeHelper.getRecipes(
        manager, ByproductEntityMeltingRecipeRegistry.TYPE.get(), ByproductEntityMeltingRecipe.class)) {
      if (!recipe.matches(type)) continue;
      if (best == null || recipe.getPriority() > best.getPriority()
          || recipe.getPriority() == best.getPriority()
          && recipe.getId().toString().compareTo(best.getId().toString()) < 0) {
        best = recipe;
      }
    }
    CACHE.put(type, best);
    return best;
  }
}
