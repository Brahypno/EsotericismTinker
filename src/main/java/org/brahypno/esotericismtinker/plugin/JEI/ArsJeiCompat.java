package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.library.compat.ars_nouveau.NovaRegistry;
import org.brahypno.esotericismtinker.library.compat.ars_nouveau.recipe.ModifiableEnchantmentRecipe;

import java.util.List;

public class ArsJeiCompat {
    public static void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ModifiableEnchantmentCategory(guiHelper));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;
        if (level == null){
            return;
        }

        List<ModifiableEnchantmentRecipe> recipes = level.getRecipeManager()
                                                         .getAllRecipesFor(NovaRegistry.MODIFIABLE_ENCHANTMENT_TYPE.get());

        registration.addRecipes(ModifiableEnchantmentCategory.RECIPE_TYPE, recipes);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        Item apparatus = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ars_nouveau", "enchanting_apparatus"));
        if (apparatus != null){
            registration.addRecipeCatalyst(
                    new ItemStack(apparatus),
                    ModifiableEnchantmentCategory.RECIPE_TYPE
            );
        }
    }
}
