package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.library.compat.ars_nouveau.NovaRegistry;
import org.brahypno.esotericismtinker.library.compat.ars_nouveau.recipe.ModifiableEnchantmentRecipe;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static void registerModifierIngredients(IJeiRuntime jeiRuntime) {
        Level level = Minecraft.getInstance().level;
        if (level == null){
            return;
        }

        IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();
        Set<ModifierId> knownModifiers = ingredientManager
                .getAllIngredients(TConstructJEIConstants.MODIFIER_TYPE)
                .stream()
                .map(ModifierEntry::getId)
                .collect(Collectors.toSet());

        List<ModifierEntry> modifiers = level.getRecipeManager()
                .getAllRecipesFor(NovaRegistry.MODIFIABLE_ENCHANTMENT_TYPE.get())
                .stream()
                .map(ModifiableEnchantmentRecipe::getResultModifier)
                .filter(knownModifiers::add)
                .map(modifier -> new ModifierEntry(modifier, 1))
                .toList();

        if (!modifiers.isEmpty()){
            ingredientManager.addIngredientsAtRuntime(TConstructJEIConstants.MODIFIER_TYPE, modifiers);
        }
    }
}
