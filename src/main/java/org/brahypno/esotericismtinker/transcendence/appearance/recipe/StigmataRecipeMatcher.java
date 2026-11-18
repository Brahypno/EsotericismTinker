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
 * Matches one part, three independent material stacks, and one stage offering.
 * <p>
 * Every message that talks about a material quotes the Tinkers' Construct material
 * name, so the player reads the same name the material shows everywhere else.
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
        if (!(partStack.getItem() instanceof ToolPartItem) || partStack.getCount() < 1) {
            return Component.translatable("message.esotericism_tinker.stigmata.not_tool_part");
        }
        if (materialSlots.size() != MATERIAL_SLOT_COUNT) {
            return Component.translatable("message.esotericism_tinker.stigmata.invalid_material_slot_count");
        }

        StigmataMaterialInput partMaterial = StigmataMaterialResolver.resolvePart(partStack);
        if (partMaterial == null) {
            return Component.translatable("message.esotericism_tinker.stigmata.unresolved_part_material", partStack.getHoverName());
        }

        Component partMaterialName = partMaterial.displayName();
        double requiredUnitsPerSlot = StigmataConfig.materialUnitsPerSlot();

        for (int slot = 0; slot < MATERIAL_SLOT_COUNT; slot++) {
            ItemStack stack = materialSlots.get(slot);
            int slotNumber = slot + 1;
            if (stack.isEmpty() || stack.getCount() < 1) {
                return Component.translatable("message.esotericism_tinker.stigmata.missing_material", slotNumber);
            }

            StigmataMaterialInput input = StigmataMaterialResolver.resolve(stack);
            if (input == null) {
                return Component.translatable("message.esotericism_tinker.stigmata.invalid_material", slotNumber, stack.getHoverName());
            }

            if (input.tier() < partMaterial.tier()) {
                return Component.translatable("message.esotericism_tinker.stigmata.material_tier_too_low",
                                             slotNumber, input.displayName(), input.tier(),
                                             partMaterialName, partMaterial.tier());
            }
            if (input.unitsPerItem() <= 0.0D) {
                return Component.translatable("message.esotericism_tinker.stigmata.invalid_material_units",
                                             slotNumber, input.displayName());
            }

            int count = (int) Math.ceil((requiredUnitsPerSlot - EPSILON) / input.unitsPerItem());
            if (stack.getCount() < count) {
                return Component.translatable("message.esotericism_tinker.stigmata.insufficient_material",
                                             slotNumber, input.displayName(), count, stack.getCount());
            }
        }

        return null;
    }

    /**
     * Builds the consumption data after {@link #getError} has returned {@code null}.
     *
     * @param materialSlots  the three material stacks, in material order
     * @param containerSlots the matching anvil input indices, in the same order
     */
    public static StigmataRecipeMatch createValidatedMatch(StigmataRecipe recipe, ItemStack partStack,
                                                          List<ItemStack> materialSlots, List<Integer> containerSlots) {
        StigmataMaterialInput partMaterial =
                Objects.requireNonNull(StigmataMaterialResolver.resolvePart(partStack), "createMatch called with an unresolved tool part material");

        if (containerSlots.size() != materialSlots.size()) {
            throw new IllegalArgumentException("container slot list does not match material list");
        }

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

            consumption.add(new StigmataRecipeMatch.SlotConsumption(containerSlots.get(slot), count));
        }

        return new StigmataRecipeMatch(recipe, partMaterial, List.copyOf(consumption));
    }
}
