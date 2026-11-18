package org.brahypno.esotericismtinker.transcendence.appearance.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataLogic;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMutationResult;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.tinkerstation.IMutableTinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal vanilla Recipe adapter for registration/sync.
 * <p>
 * The custom anvil performs the actual matching. Inputs are classified by content
 * through {@link StigmataIngredientLayout}, so the five input slots may be filled in
 * any order: the recipe itself decides which stack is the part, which is the stage
 * offering, and which three are materials.
 */
public record StigmataRecipeAdapter(StigmataRecipe data) implements ITinkerStationRecipe {
    @Override
    public boolean matches(ITinkerStationContainer container, Level level) {
        return null == getMatchError(container);
    }

    @Override
    public RecipeResult<LazyToolStack> getValidatedResult(ITinkerStationContainer container, RegistryAccess access) {
        StigmataIngredientLayout.Result layout = classify(container);
        if (!layout.success()) {
            return RecipeResult.failure(layout.error());
        }

        StigmataIngredientLayout.Slots slots = layout.slots();
        Component error = StigmataRecipeMatcher.getError(partStack(container, slots), materials(container, slots));
        if (null != error) {
            return RecipeResult.failure(error);
        }

        ToolStack tool = ToolStack.from(container.getTinkerableStack().copy());
        StigmataMutationResult mutation = StigmataLogic.applyTarget(
                tool, partStack(container, slots), data.targetStage(), RandomSource.create(), false);
        return mutation.success() ? ITinkerStationRecipe.success(tool, container) : RecipeResult.failure(mutation.error());
    }

    @Override
    public int shrinkToolSlotBy() {return 1;}

    @Override
    public void updateInputs(LazyToolStack result, IMutableTinkerStationContainer container, boolean isServer) {
        StigmataIngredientLayout.Result layout = classify(container);
        if (!layout.success()) {
            return;
        }

        StigmataIngredientLayout.Slots slots = layout.slots();
        List<ItemStack> materials = materials(container, slots);
        if (null != StigmataRecipeMatcher.getError(partStack(container, slots), materials)) {
            return;
        }

        StigmataRecipeMatch match = StigmataRecipeMatcher.createValidatedMatch(
                data, partStack(container, slots), materials, slots.materialSlots());

        container.shrinkInput(slots.partSlot(), 1);
        for (StigmataRecipeMatch.SlotConsumption entry : match.materialConsumption()) {
            container.shrinkInput(entry.slot(), entry.count());
        }
        container.shrinkInput(slots.selectorSlot(), 1);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return data.id();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EsotericismTinkerRecipeTypes.STIGMATA_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return EsotericismTinkerRecipeTypes.STIGMATA_TYPE.get();
    }

    private Component getMatchError(ITinkerStationContainer container) {
        StigmataIngredientLayout.Result layout = classify(container);
        if (!layout.success()) {
            return layout.error();
        }
        return StigmataRecipeMatcher.getError(
                partStack(container, layout.slots()), materials(container, layout.slots()));
    }

    private StigmataIngredientLayout.Result classify(ITinkerStationContainer container) {
        if (container.getInputCount() != StigmataIngredientLayout.INPUT_COUNT) {
            return new StigmataIngredientLayout.Result(
                    null,
                    Component.translatable("message.esotericism_tinker.stigmata.invalid_input_count",
                                           container.getInputCount())
            );
        }
        return StigmataIngredientLayout.classify(data, StigmataIngredientLayout.inputs(container));
    }

    private static ItemStack partStack(ITinkerStationContainer container, StigmataIngredientLayout.Slots slots) {
        return container.getInput(slots.partSlot());
    }

    private static List<ItemStack> materials(ITinkerStationContainer container, StigmataIngredientLayout.Slots slots) {
        List<ItemStack> materials = new ArrayList<>(StigmataIngredientLayout.MATERIAL_COUNT);
        for (int slot : slots.materialSlots()) {
            materials.add(container.getInput(slot));
        }
        return materials;
    }
}
