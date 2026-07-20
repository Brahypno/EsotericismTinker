package org.brahypno.esotericismtinker.transcendence.appearance.recipe;

import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataLogic;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMutationResult;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;

/**
 * Facility-side execution entry for an already matched five-input rite.
 */
public final class StigmataRecipeExecutor {
    private StigmataRecipeExecutor() {}

    public static StigmataMutationResult execute(ToolStack simulatedTool, ItemStack partStack, List<ItemStack> materialSlots, ItemStack selectorStack, StigmataRecipeMatch match) {
        StigmataMutationResult mutation = StigmataLogic.applyTarget(simulatedTool, partStack, match.recipe().targetStage());
        if (!mutation.success())
            return mutation;

        partStack.shrink(1);
        for (StigmataRecipeMatch.SlotConsumption entry : match.materialConsumption())
            materialSlots.get(entry.slot()).shrink(entry.count());
        selectorStack.shrink(1);
        return mutation;
    }
}
