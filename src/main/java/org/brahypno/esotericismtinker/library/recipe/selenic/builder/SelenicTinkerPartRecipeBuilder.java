package org.brahypno.esotericismtinker.library.recipe.selenic.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicAstrolabeRecipe;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class SelenicTinkerPartRecipeBuilder extends SelenicAstrolabeRecipeBuilder {
    private final MaterialVariantId materialId;
    private final List<MaterialStatsId> statIds;

    @Nullable
    private ResourceLocation partItemId;

    private SelenicTinkerPartRecipeBuilder(MaterialVariantId materialId, List<MaterialStatsId> statIds) {
        this.materialId = materialId;
        this.statIds = List.copyOf(statIds);
    }

    public static SelenicTinkerPartRecipeBuilder part(MaterialVariantId materialId, MaterialStatsId statId) {
        return new SelenicTinkerPartRecipeBuilder(materialId, List.of(statId));
    }

    public static SelenicTinkerPartRecipeBuilder part(ResourceLocation materialId, ResourceLocation statId) {
        return part(MaterialVariantId.parse(materialId.toString()), new MaterialStatsId(statId));
    }

    public static SelenicTinkerPartRecipeBuilder parts(MaterialVariantId materialId, List<MaterialStatsId> statIds) {
        return new SelenicTinkerPartRecipeBuilder(materialId, statIds);
    }

    public static SelenicTinkerPartRecipeBuilder parts(MaterialVariantId materialId, MaterialStatsId... statIds) {
        return parts(materialId, List.of(statIds));
    }

    public static SelenicTinkerPartRecipeBuilder parts(ResourceLocation materialId, ResourceLocation... statIds) {
        List<MaterialStatsId> stats = Arrays.stream(statIds)
                                            .map(MaterialStatsId::new)
                                            .toList();

        return parts(MaterialVariantId.parse(materialId.toString()), stats);
    }

    public SelenicTinkerPartRecipeBuilder partItem(ResourceLocation partItemId) {
        this.partItemId = partItemId;
        return this;
    }

    @Override
    protected RecipeSerializer<?> getSerializer() {
        return EsotericismTinkerRecipeTypes.SELENIC_TINKER_PART_SERIALIZER.get();
    }

    @Override
    protected void writeExtra(JsonObject json) {
        JsonObject part = new JsonObject();

        part.addProperty("material", materialId.toString());

        if (statIds.size() == 1){
            part.addProperty("stat", statIds.get(0).toString());
        }else {
            JsonArray stats = new JsonArray();

            for (MaterialStatsId statId : statIds) {
                stats.add(statId.toString());
            }

            part.add("stats", stats);
        }

        if (partItemId != null){
            part.addProperty("part", partItemId.toString());
        }

        json.add("tconstruct_runtime_part", part);
    }

    @Override
    protected void validate() {
        super.validate();

        if (SelenicAstrolabeRecipe.isIngredientEmpty(input)){
            throw new IllegalStateException("Selenic tinker part recipe requires a non-empty crown input.");
        }

        if (materialId == null){
            throw new IllegalStateException("Selenic tinker part recipe requires a material.");
        }

        if (statIds == null || statIds.isEmpty()){
            throw new IllegalStateException("Selenic tinker part recipe requires at least one stat.");
        }
    }
}