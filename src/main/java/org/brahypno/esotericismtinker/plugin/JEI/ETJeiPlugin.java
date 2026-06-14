package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicAstrolabeRecipe;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicTinkerPartRecipe;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class ETJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            new ResourceLocation(EsotericismTinker.MODID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (ModList.get().isLoaded("ars_nouveau")){
            ArsJeiCompact.registerCategories(registration);
        }
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new SelenicAstrolabeRecipeCategory<>());

        registration.addRecipeCategories(new SelenicTinkerPartRecipeCategory());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null){
            return;
        }
        if (ModList.get().isLoaded("ars_nouveau")){
            ArsJeiCompact.registerRecipes(registration);
        }
        List<SelenicAstrolabeRecipe> astrolabe_recipes = Minecraft.getInstance().level
                .getRecipeManager()
                .getAllRecipesFor(EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_TYPE.get());

        registration.addRecipes(SelenicAstrolabeRecipeCategory.TYPE, astrolabe_recipes);


        List<SelenicTinkerPartRecipe> tinker_recipes = Minecraft.getInstance().level
                .getRecipeManager()
                .getAllRecipesFor(EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_TINKER_TYPE.get());
        List<SelenicTinkerPartJeiRecipe> partDisplays =
                SelenicTinkerPartJeiRecipe.createAll(
                        Minecraft.getInstance().level,
                        tinker_recipes
                );

        registration.addRecipes(
                SelenicTinkerPartRecipeCategory.TYPE,
                partDisplays
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (ModList.get().isLoaded("ars_nouveau")){
            ArsJeiCompact.registerRecipeCatalysts(registration);
        }
        registration.addRecipeCatalyst(
                new ItemStack(EsotericismTinkerSelenic.armillaryCrown),
                SelenicAstrolabeRecipeCategory.TYPE,
                SelenicTinkerPartRecipeCategory.TYPE
        );
    }
}