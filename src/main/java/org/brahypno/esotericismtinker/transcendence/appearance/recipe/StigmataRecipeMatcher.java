package org.brahypno.esotericismtinker.transcendence.appearance.recipe;

import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMaterialInput;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMaterialResolver;
import org.brahypno.esotericismtinker.transcendence.appearance.config.StigmataConfig;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches one part, three independent material slots, and one stage selector.
 */
public final class StigmataRecipeMatcher {
    private static final int MATERIAL_SLOT_COUNT = 3;
    private static final double EPSILON = 1.0E-7D;

    private StigmataRecipeMatcher() {}

    public static @Nullable StigmataRecipeMatch match(StigmataRecipe recipe, ItemStack partStack, List<ItemStack> materialSlots, ItemStack selectorStack) {
        if (!(partStack.getItem() instanceof ToolPartItem) || partStack.getCount() < 1)
            return null;
        if (!recipe.selector().test(selectorStack) || selectorStack.getCount() < 1)
            return null;
        if (materialSlots.size() != MATERIAL_SLOT_COUNT)
            return null;

        StigmataMaterialInput partMaterial = StigmataMaterialResolver.resolvePart(partStack);
        if (partMaterial == null)
            return null;

        double requiredUnitsPerSlot = StigmataConfig.materialUnitsPerSlot();
        List<StigmataRecipeMatch.SlotConsumption> consumption = new ArrayList<>(MATERIAL_SLOT_COUNT);
        for (int slot = 0; slot < MATERIAL_SLOT_COUNT; slot++) {
            ItemStack stack = materialSlots.get(slot);
            if (stack.isEmpty() || stack.getCount() < 1)
                return null;
            StigmataMaterialInput input = StigmataMaterialResolver.resolve(stack);
            if (input == null || input.tier() < partMaterial.tier() || input.unitsPerItem() <= 0.0D)
                return null;
            int count = (int) Math.ceil((requiredUnitsPerSlot - EPSILON) / input.unitsPerItem());
            if (stack.getCount() < count)
                return null;
            consumption.add(new StigmataRecipeMatch.SlotConsumption(slot, count));
        }

        return new StigmataRecipeMatch(recipe, partMaterial, List.copyOf(consumption));
    }
}
