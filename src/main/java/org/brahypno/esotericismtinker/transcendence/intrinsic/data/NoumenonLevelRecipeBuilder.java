package org.brahypno.esotericismtinker.transcendence.intrinsic.data;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.esotericismtinker.transcendence.intrinsic.recipe.NoumenonLevelRecipe;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.modifiers.adding.ModifierRecipeBuilder;

import java.util.function.Consumer;

public class NoumenonLevelRecipeBuilder extends ModifierRecipeBuilder {
    protected NoumenonLevelRecipeBuilder(ModifierId result) {
        super(result);
    }

    public static NoumenonLevelRecipeBuilder modifier(
            ModifierId modifier) {
        return new NoumenonLevelRecipeBuilder(modifier);
    }

    @Override
    public void save(
            Consumer<FinishedRecipe> consumer,
            ResourceLocation id) {
        if (inputs.isEmpty() && !allowCrystal){
            throw new IllegalStateException(
                    "Must have at least 1 input");
        }

        ResourceLocation advancementId =
                buildOptionalAdvancement(id, "modifiers");

        consumer.accept(new LoadableFinishedRecipe<>(
                new NoumenonLevelRecipe(
                        id,
                        inputs,
                        tools,
                        maxToolSize,
                        result,
                        ModifierEntry.VALID_LEVEL.range(
                                minLevel, maxLevel),
                        slots,
                        allowCrystal,
                        checkTraitLevel),
                NoumenonLevelRecipe.LOADER,
                advancementId));
    }
}
