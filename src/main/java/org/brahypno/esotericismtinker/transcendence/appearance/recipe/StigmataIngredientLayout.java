package org.brahypno.esotericismtinker.transcendence.appearance.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Content based classification of the five anvil inputs.
 * <p>
 * The rite does not depend on slot order. Whatever the player places, the anvil
 * identifies exactly one tool part, one stage offering for the recipe, and three
 * independent materials. Classification always runs in the same order, so the
 * result is identical for every arrangement:
 * <ol>
 *   <li>the first {@link ToolPartItem} is the part;</li>
 *   <li>the first stack matching the recipe selector is the stage offering;</li>
 *   <li>every other non-empty stack is a material.</li>
 * </ol>
 * Extra tool parts are rejected instead of silently guessing, and extra stacks
 * matching the selector fall through to the material side so that a material such
 * as diamond can still be offered for a later stage.
 */
public final class StigmataIngredientLayout {
    /** Input slots of the transcendence anvil. */
    public static final int INPUT_COUNT = 5;
    /** Independent material stacks the rite consumes. */
    public static final int MATERIAL_COUNT = 3;

    private StigmataIngredientLayout() {}

    /** Classified input slot indices, all in the range {@code [0, INPUT_COUNT)}. */
    public record Slots(int selectorSlot, int partSlot, List<Integer> materialSlots) {}

    /** Either the classified slots or the localized reason classification failed. */
    public record Result(@Nullable Slots slots, @Nullable Component error) {
        public boolean success() {
            return slots != null;
        }
    }

    static Result failure(String translationKey, Object... args) {
        return new Result(null, Component.translatable(translationKey, args));
    }

    /** Copies the five input stacks of the container in slot order. */
    public static List<ItemStack> inputs(ITinkerStationContainer container) {
        List<ItemStack> inputs = new ArrayList<>(INPUT_COUNT);
        for (int slot = 0; slot < INPUT_COUNT; slot++) {
            inputs.add(container.getInput(slot));
        }
        return inputs;
    }

    /**
     * Classifies the inputs for one Stigmata recipe.
     *
     * @param recipe recipe providing the stage offering to look for
     * @param inputs five anvil input stacks in slot order
     * @return classified slots, or a localized error describing what is missing
     */
    public static Result classify(StigmataRecipe recipe, List<ItemStack> inputs) {
        if (inputs.size() != INPUT_COUNT) {
            return failure("message.esotericism_tinker.stigmata.invalid_input_count", inputs.size());
        }

        int partSlot = -1;
        int partCount = 0;
        int selectorSlot = -1;
        List<Integer> materialSlots = new ArrayList<>(MATERIAL_COUNT);

        for (int slot = 0; slot < inputs.size(); slot++) {
            ItemStack stack = inputs.get(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof ToolPartItem) {
                partCount++;
                if (partSlot < 0) {
                    partSlot = slot;
                }
                continue;
            }

            if (selectorSlot < 0 && recipe.selector().test(stack)) {
                selectorSlot = slot;
                continue;
            }

            materialSlots.add(slot);
        }

        if (partCount == 0) {
            return failure("message.esotericism_tinker.stigmata.missing_part");
        }
        if (partCount > 1) {
            return failure("message.esotericism_tinker.stigmata.multiple_parts", partCount);
        }
        if (selectorSlot < 0) {
            return failure("message.esotericism_tinker.stigmata.missing_selector", selectorName(recipe));
        }
        if (materialSlots.size() != MATERIAL_COUNT) {
            return failure("message.esotericism_tinker.stigmata.material_slot_count",
                           materialSlots.size(), MATERIAL_COUNT);
        }

        return new Result(new Slots(selectorSlot, partSlot, List.copyOf(materialSlots)), null);
    }

    /** True when any non-empty input matches the given recipe's stage offering. */
    public static boolean offersSelector(StigmataRecipe recipe, List<ItemStack> inputs) {
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty() && recipe.selector().test(stack)) {
                return true;
            }
        }
        return false;
    }

    /** First stack of the recipe selector, used only for error messages. */
    private static Component selectorName(StigmataRecipe recipe) {
        ItemStack[] items = recipe.selector().getItems();
        return 0 == items.length ? Component.empty() : items[0].getHoverName();
    }
}
