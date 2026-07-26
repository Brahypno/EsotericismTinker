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

import java.util.List;

/**
 * Minimal vanilla Recipe adapter for registration/sync.
 * The custom anvil performs actual slot matching and calls StigmataLogic.
 */
public record StigmataRecipeAdapter(StigmataRecipe data) implements ITinkerStationRecipe {
    private static final int SELECTOR_INPUT = 0;
    private static final int PART_INPUT = 1;
    private static final int FIRST_MATERIAL_INPUT = 2;
    private static final int SECOND_MATERIAL_INPUT = 3;
    private static final int THIRD_MATERIAL_INPUT = 4;

    @Override
    public boolean matches(ITinkerStationContainer container, Level level) {
        return null == getMatchError(container);
    }

    @Override
    public RecipeResult<LazyToolStack> getValidatedResult(ITinkerStationContainer container, RegistryAccess access) {
        Component error = getMatchError(container);
        if (null != error){
            return RecipeResult.failure(error);
        }

        ToolStack tool = ToolStack.from(container.getTinkerableStack().copy());
        StigmataMutationResult mutation = StigmataLogic.applyTarget(tool, container.getInput(PART_INPUT), data.targetStage(), RandomSource.create(), false);
        return mutation.success() ? ITinkerStationRecipe.success(tool, container) : RecipeResult.failure(mutation.error());
    }

    @Override
    public int shrinkToolSlotBy() {return 1;}

    @Override
    public void updateInputs(LazyToolStack result, IMutableTinkerStationContainer container, boolean isServer) {
        List<ItemStack> materialSlots = materialSlots(container);
        Component error = StigmataRecipeMatcher.getError(container.getInput(PART_INPUT), materialSlots);
        if (null != error)
            return;

        StigmataRecipeMatch match = StigmataRecipeMatcher.createValidatedMatch(data, container.getInput(PART_INPUT), materialSlots);
        container.shrinkInput(PART_INPUT, 1);
        for (StigmataRecipeMatch.SlotConsumption entry : match.materialConsumption()) {
            container.shrinkInput(materialContainerIndex(entry.slot()), entry.count());
        }
        container.shrinkInput(SELECTOR_INPUT, 1);
    }

    private static int materialContainerIndex(int materialIndex) {
        return switch (materialIndex) {
            case 0 -> FIRST_MATERIAL_INPUT;
            case 1 -> SECOND_MATERIAL_INPUT;
            case 2 -> THIRD_MATERIAL_INPUT;
            default -> throw new IllegalArgumentException();
        };
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
        if (5 != container.getInputCount()){
            return Component.translatable("message.esotericism_tinker.stigmata.invalid_input_count", container.getInputCount());
        }

        return StigmataRecipeMatcher.getError(container.getInput(PART_INPUT), materialSlots(container));
    }

    private static List<ItemStack> materialSlots(ITinkerStationContainer container) {
        return List.of(container.getInput(FIRST_MATERIAL_INPUT), container.getInput(SECOND_MATERIAL_INPUT), container.getInput(THIRD_MATERIAL_INPUT));
    }
}
