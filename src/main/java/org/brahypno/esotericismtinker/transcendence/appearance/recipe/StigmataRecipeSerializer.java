package org.brahypno.esotericismtinker.transcendence.appearance.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataStage;
import org.jetbrains.annotations.Nullable;

/**
 * Register this serializer under esotericism_tinker:stigmata.
 */
public final class StigmataRecipeSerializer implements RecipeSerializer<StigmataRecipeAdapter> {
    @Override
    public StigmataRecipeAdapter fromJson(ResourceLocation id, JsonObject json) {
        Ingredient selector = Ingredient.fromJson(json.get("selector"));

        StigmataStage stage = StigmataStage.byName(GsonHelper.getAsString(json, "target_stage"));
        if (stage == null){
            throw new IllegalArgumentException("Invalid target_stage in stigmata recipe " + id);
        }

        return new StigmataRecipeAdapter(new StigmataRecipe(id, selector, stage));
    }

    @Override
    public @Nullable StigmataRecipeAdapter fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        Ingredient selector = Ingredient.fromNetwork(buffer);
        StigmataStage stage = StigmataStage.byIndex(buffer.readVarInt());
        return stage == null ? null : new StigmataRecipeAdapter(new StigmataRecipe(id, selector, stage));
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, StigmataRecipeAdapter adapter) {
        adapter.data().selector().toNetwork(buffer);
        buffer.writeVarInt(adapter.data().targetStage().index());
    }
}
