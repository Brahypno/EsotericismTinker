package org.brahypno.esotericismtinker.plugin.JEI;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.transcendence.appearance.recipe.StigmataRecipeAdapter;

import java.util.List;

/**
 * One JEI display recipe for one actually loaded Stigmata recipe.
 * All lists are kept at the same length, so JEI's normal ingredient cycler
 * advances the complete example as one aligned row instead of producing a
 * cartesian product of cached recipes.
 */
public record StigmataJeiRecipe(
    StigmataRecipeAdapter source,
    List<ItemStack> toolsBefore,
    List<ItemStack> parts,
    List<ItemStack> material1,
    List<ItemStack> material2,
    List<ItemStack> material3,
    List<ItemStack> selectors,
    List<ItemStack> toolsAfter
) {
  public ResourceLocation id() {
    return source.getId();
  }
}

