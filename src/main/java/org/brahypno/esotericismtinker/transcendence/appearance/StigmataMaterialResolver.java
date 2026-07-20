package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.IRepairKitItem;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

/**
 * Resolves material identity, tier, and per-item ritual units.
 */
public final class StigmataMaterialResolver {
    public static final double REPAIR_KIT_UNITS = 2.0D;

    private StigmataMaterialResolver() {}

    public static @Nullable StigmataMaterialInput resolve(ItemStack stack) {
        if (stack.isEmpty())
            return null;

        MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
        if (recipe != MaterialRecipe.EMPTY && recipe.getValue() > 0 && recipe.getNeeded() > 0){
            return resolveVariant(recipe.getMaterial().getVariant(), recipe.getValue() / (double) recipe.getNeeded());
        }

        if (stack.getItem() instanceof IRepairKitItem repairKit){
            MaterialVariantId variant = repairKit.getMaterial(stack);
            if (variant != null && MaterialRecipeCache.getRecipes(variant).isEmpty())
                return resolveVariant(variant, REPAIR_KIT_UNITS);
        }

        if (stack.getItem() instanceof IMaterialItem)
            return null;
        return null;
    }

    public static @Nullable StigmataMaterialInput resolvePart(ItemStack partStack) {
        if (!(partStack.getItem() instanceof ToolPartItem part))
            return null;
        return resolveVariant(part.getMaterial(partStack), 0.0D);
    }

    private static @Nullable StigmataMaterialInput resolveVariant(MaterialVariantId variant, double unitsPerItem) {
        if (variant == null || variant.getId().equals(IMaterial.UNKNOWN_ID))
            return null;
        IMaterial material = MaterialRegistry.getMaterial(variant.getId());
        if (material == IMaterial.UNKNOWN)
            return null;
        return new StigmataMaterialInput(variant, material.getIdentifier(), material.getTier(), unitsPerItem);
    }
}
