package org.brahypno.esotericismtinker.transcendence.intrinsic.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.adding.ModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A thin {@link ModifierRecipe} variant whose recipe level is backed by
 * {@link NoumenonData#level}, not by the TConstruct modifier level.
 *
 * <p>The Noumenon core modifier remains a one-level/NoLevelsModifier. It is
 * added only when the independent Noumenon level changes from 0 to 1.</p>
 */
public class NoumenonLevelRecipe extends ModifierRecipe {
    public static final RecordLoadable<NoumenonLevelRecipe> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(),
            INPUTS_FIELD,
            TOOLS_FIELD,
            MAX_TOOL_SIZE_FIELD,
            RESULT_FIELD,
            LEVEL_FIELD,
            SLOTS_FIELD,
            ALLOW_CRYSTAL_FIELD,
            CHECK_TRAIT_LEVEL_FIELD,
            NoumenonLevelRecipe::new
    );

    public NoumenonLevelRecipe(
            ResourceLocation id,
            List<SizedIngredient> inputs,
            Ingredient toolRequirement,
            int maxToolSize,
            ModifierId result,
            IntRange level,
            @Nullable SlotCount slots,
            boolean allowCrystal,
            boolean checkTraitLevel
    ) {
        super(id, inputs, toolRequirement, maxToolSize, result, level, slots, allowCrystal, checkTraitLevel);
    }

    /**
     * Makes inherited level validation use the independently stored Noumenon level.
     */
    @Override
    protected int getNewLevel(IToolStackView tool) {
        return NoumenonData.read(tool).level + 1;
    }

    @Override
    public RecipeResult<LazyToolStack> getValidatedResult(
            ITinkerStationContainer inv,
            RegistryAccess access
    ) {
        ToolStack tool = inv.getTinkerable();
        int resultLevel = getNewLevel(tool);

        Component commonError = validatePrerequisites(tool, resultLevel);
        if (commonError != null) {
            return RecipeResult.failure(commonError);
        }

        tool = tool.copy();
        ToolDataNBT persistentData = tool.getPersistentData();

        SlotCount slots = getSlots();
        if (slots != null) {
            persistentData.addSlots(slots.type(), -slots.count());
        }

        NoumenonData data = NoumenonData.read(tool);

        // The actual modifier is only a marker/hook carrier and must remain level 1.
        if (data.level == 0) {
            tool.addModifier(result.getId(), 1);
        }

        data.level = resultLevel;
        data.write(persistentData);

        Component toolValidation = tool.tryValidate();
        if (toolValidation != null) {
            return RecipeResult.failure(toolValidation);
        }

        return success(tool, inv);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EsotericismTinkerRecipeTypes.NOUMENON_LEVEL_SERIALIZER.get();
    }
}
