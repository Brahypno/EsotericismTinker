package org.brahypno.esotericismtinker.transcendence.appearance.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMaterialInput;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMaterialResolver;
import org.brahypno.esotericismtinker.transcendence.appearance.config.StigmataConfig;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Matches one part, three independent material slots, and one stage selector.
 */
public final class StigmataRecipeMatcher {
    private static final int MATERIAL_SLOT_COUNT = 3;
    private static final double EPSILON = 1.0E-7D;

    private StigmataRecipeMatcher() {}

    /**
     * Validates the recipe inputs.
     *
     * @return the error shown to the player, or {@code null} when the inputs are valid
     */
    public static @Nullable Component getError(ItemStack partStack, List<ItemStack> materialSlots) {
        if (!(partStack.getItem() instanceof ToolPartItem) || partStack.getCount() < 1)
            return Component.translatable("message.esotericism_tinker.stigmata.not_tool_part");
        if (materialSlots.size() != MATERIAL_SLOT_COUNT)
            return Component.translatable("message.esotericism_tinker.stigmata.invalid_material_slot_count");

        StigmataMaterialInput partMaterial = StigmataMaterialResolver.resolvePart(partStack);
        if (partMaterial == null)
            return Component.translatable("message.esotericism_tinker.stigmata.unresolved_part_material", partStack.getHoverName());

        double requiredUnitsPerSlot = StigmataConfig.materialUnitsPerSlot();
        for (int slot = 0; slot < MATERIAL_SLOT_COUNT; slot++) {
            ItemStack stack = materialSlots.get(slot);
            if (stack.isEmpty() || stack.getCount() < 1)
                return Component.translatable("message.esotericism_tinker.stigmata.missing_material", slot + 1);

            StigmataMaterialInput input = StigmataMaterialResolver.resolve(stack);
            if (input == null)
                return Component.translatable("message.esotericism_tinker.stigmata.invalid_material", slot + 1, stack.getHoverName());
            if (input.tier() < partMaterial.tier())
                return Component.translatable("message.esotericism_tinker.stigmata.material_tier_too_low", slot + 1, input.tier(), partMaterial.tier());
            if (input.unitsPerItem() <= 0.0D)
                return Component.translatable("message.esotericism_tinker.stigmata.invalid_material_units", slot + 1, stack.getHoverName());

            int count = (int) Math.ceil((requiredUnitsPerSlot - EPSILON) / input.unitsPerItem());
            if (stack.getCount() < count)
                return Component.translatable("message.esotericism_tinker.stigmata.insufficient_material", slot + 1, count, stack.getCount());
        }

        return null;
    }

    /**
     * Builds the consumption data after {@link #getError} has returned {@code null}.
     */
    public static StigmataRecipeMatch createValidatedMatch(StigmataRecipe recipe, ItemStack partStack, List<ItemStack> materialSlots) {
        StigmataMaterialInput partMaterial =
                Objects.requireNonNull(StigmataMaterialResolver.resolvePart(partStack), "createMatch called with an unresolved tool part material");

        double requiredUnitsPerSlot = StigmataConfig.materialUnitsPerSlot();
        List<StigmataRecipeMatch.SlotConsumption> consumption =
                new ArrayList<>(MATERIAL_SLOT_COUNT);

        for (int slot = 0; slot < MATERIAL_SLOT_COUNT; slot++) {
            StigmataMaterialInput input = Objects.requireNonNull(
                    StigmataMaterialResolver.resolve(materialSlots.get(slot)),
                    "createMatch called with an unresolved material in slot " + slot
            );

            int count = (int) Math.ceil(
                    (requiredUnitsPerSlot - EPSILON) / input.unitsPerItem()
            );

            consumption.add(new StigmataRecipeMatch.SlotConsumption(slot, count));
        }

        return new StigmataRecipeMatch(recipe, partMaterial, List.copyOf(consumption));
    }
}
