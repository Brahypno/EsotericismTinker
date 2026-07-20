package org.brahypno.esotericismtinker.smeltery.recipe.casting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ItemCastingRecipe;

/**
 * Retextured casting recipe which preserves the texture carried by a
 * retextured cast instead of using the cast block itself as the texture.
 */
public final class RecursiveRetexturedCastingRecipe extends ItemCastingRecipe {
    public static final RecordLoadable<RecursiveRetexturedCastingRecipe> LOADER = RecordLoadable.create(
            LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(),
            ContextKey.ID.requiredField(),
            LoadableRecipeSerializer.RECIPE_GROUP,
            CAST_FIELD,
            FLUID_FIELD,
            RESULT_FIELD,
            COOLING_TIME_FIELD,
            CAST_CONSUMED_FIELD,
            SWITCH_SLOTS_FIELD,
            RecursiveRetexturedCastingRecipe::new
    );

    public RecursiveRetexturedCastingRecipe(
            TypeAwareRecipeSerializer<?> serializer,
            ResourceLocation id,
            String group,
            Ingredient cast,
            FluidIngredient fluid,
            ItemOutput result,
            int coolingTime,
            boolean consumed,
            boolean switchSlots
    ) {
        super(serializer, id, group, cast, fluid, result, coolingTime, consumed, switchSlots);
    }

    @Override
    public ItemStack assemble(ICastingContainer inv, RegistryAccess access) {
        ItemStack result = getResultItem(access).copy();
        ItemStack cast = inv.getStack();

        // A Mantle retextured block item already carries its real texture in NBT.
        // Resolve that first, then fall back to the cast block itself.
        Block texture = RetexturedHelper.getTexture(cast);
        if (texture == Blocks.AIR && cast.getItem() instanceof BlockItem blockItem) {
            texture = blockItem.getBlock();
        }
        if (texture != Blocks.AIR) {
            return RetexturedHelper.setTexture(result, texture);
        }
        return result;
    }
}
