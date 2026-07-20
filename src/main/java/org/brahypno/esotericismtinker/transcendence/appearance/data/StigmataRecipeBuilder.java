package org.brahypno.esotericismtinker.transcendence.appearance.data;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataStage;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * One builder writes all three target-stage recipes.
 * There are no separate manifestation/alienation/sealing Java recipe interfaces.
 */
public final class StigmataRecipeBuilder {
    private final Ingredient selector;
    private final StigmataStage targetStage;

    private StigmataRecipeBuilder(Ingredient selector, StigmataStage targetStage) {
        this.selector = selector;
        this.targetStage = targetStage;
    }

    public static StigmataRecipeBuilder stigmata(Ingredient selector, StigmataStage targetStage) {
        return new StigmataRecipeBuilder(selector, targetStage);
    }

    public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
        consumer.accept(new Result(id, selector, targetStage));
    }

    private record Result(ResourceLocation id, Ingredient selector, StigmataStage targetStage) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            json.add("selector", selector.toJson());
            json.addProperty("target_stage", targetStage.getSerializedName());
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
            return EsotericismTinkerRecipeTypes.STIGMATA_SERIALIZER.get();
        }

        @Override
        public @Nullable JsonObject serializeAdvancement() {
            return null;
        }

        @Override
        public @Nullable ResourceLocation getAdvancementId() {
            return null;
        }
    }
}
