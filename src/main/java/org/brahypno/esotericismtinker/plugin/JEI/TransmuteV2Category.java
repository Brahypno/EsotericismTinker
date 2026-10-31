package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import org.brahypno.esotericismtinker.EsotericismTinker;
import slimeknights.tconstruct.library.recipe.melting.IDisplayableMeltingRecipe;

/** Uses the shared melting layout with the recipe's original output amounts, without the v1 exchange. */
public class TransmuteV2Category extends TransmuteCategory {
    public static final RecipeType<IDisplayableMeltingRecipe> TYPE =
            RecipeType.create(EsotericismTinker.MODID, "transmute_v2", IDisplayableMeltingRecipe.class);

    public TransmuteV2Category(IGuiHelper helper) {
        super(helper, TYPE, Component.translatable("jei.esotericism_tinker.transmute_v2.title"));
    }
}
