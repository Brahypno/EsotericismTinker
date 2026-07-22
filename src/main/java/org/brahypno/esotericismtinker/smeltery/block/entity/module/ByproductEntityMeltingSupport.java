package org.brahypno.esotericismtinker.smeltery.block.entity.module;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.brahypno.esotericismtinker.smeltery.recipe.entitymelting.ByproductEntityMeltingRecipe;
import org.brahypno.esotericismtinker.smeltery.recipe.entitymelting.ByproductEntityMeltingRecipeCache;

public final class ByproductEntityMeltingSupport {
  private ByproductEntityMeltingSupport() {}
  public static boolean tryMelt(LivingEntity entity, RecipeManager recipes, IFluidHandler tank, DamageSource source) {
    ByproductEntityMeltingRecipe recipe = ByproductEntityMeltingRecipeCache.findRecipe(recipes, entity.getType());
    if (recipe == null || !entity.hurt(source, recipe.getDamage())) return false;
    tank.fill(recipe.getOutput(entity), IFluidHandler.FluidAction.EXECUTE);
    recipe.getByproducts(entity).forEach(stack -> tank.fill(stack, IFluidHandler.FluidAction.EXECUTE));
    return true;
  }
}
