package org.brahypno.esotericismtinker.smeltery.recipe.melting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;

import java.util.List;

/** Standard Tinkers' melting data and behavior, isolated to the Transmute v2 recipe type. */
public class TransmuteMeltingRecipe extends MeltingRecipe {
    public static final RecordLoadable<TransmuteMeltingRecipe> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
            INPUT, OUTPUT, TEMPERATURE, TIME, BYPRODUCTS, TransmuteMeltingRecipe::new);

    @SuppressWarnings("deprecation")
    public TransmuteMeltingRecipe(ResourceLocation id, String group, Ingredient input, FluidOutput output,
                                 int temperature, int time, List<FluidOutput> byproducts) {
        // Do not register v2 fluids in Tinkers' modifier lookup.
        // Our furnace inventory maintains its own recipe cache.
        super(id, group, input, output, temperature, time, byproducts, false);
    }

    @Override
    public RecipeType<?> getType() {
        return EsotericismTinkerRecipeTypes.TRANSMUTE_V2_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EsotericismTinkerRecipeTypes.TRANSMUTE_V2_SERIALIZER.get();
    }
}
