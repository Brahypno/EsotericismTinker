package org.brahypno.esotericismtinker.tools.data;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerTools;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.recipe.data.IRecipeHelper;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipeBuilder;
import slimeknights.tconstruct.tools.TinkerToolParts;

import java.util.function.Consumer;

public class ToolsRecipesProvider implements IRecipeHelper {
    public void buildRecipes(Consumer<FinishedRecipe> consumer) {
        this.addToolBuildingRecipes(consumer);
        this.addPartRecipes(consumer);
        //this.addRecycleRecipes(consumer);
    }

    protected void addToolBuildingRecipes(Consumer<FinishedRecipe> consumer) {
        String folder = "tools/building/";
        ToolBuildingRecipeBuilder.toolBuildingRecipe(EsotericismTinkerTools.ritual_blade.get())
                                 .addExtraRequirement(Ingredient.of(Tags.Items.GLASS))
                                 .save(consumer, prefix(id(EsotericismTinkerTools.ritual_blade), folder));
    }

    private void addPartRecipes(Consumer<FinishedRecipe> consumer) {
        String partFolder = "tools/parts/";
        MaterialCastingRecipeBuilder.tableRecipe(TinkerToolParts.shieldCore.get())
                                    .setCast(EsotericismTinkerTagKeys.Items.Doors, true)
                                    .setItemCost(4)
                                    .save(consumer, location(partFolder + "shield_core_cast"));
    }

    @Override
    public @NotNull String getModId() {
        return EsotericismTinker.MODID;
    }
}
