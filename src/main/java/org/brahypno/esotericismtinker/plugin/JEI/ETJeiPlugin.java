package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicAstrolabeRecipe;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicTinkerPartRecipe;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.brahypno.esotericismtinker.smeltery.recipe.entitymelting.ByproductEntityMeltingRecipe;
import org.brahypno.esotericismtinker.smeltery.recipe.entitymelting.ByproductEntityMeltingRecipeRegistry;
import org.brahypno.esotericismtinker.transcendence.appearance.recipe.StigmataRecipeAdapter;
import org.brahypno.esotericismtinker.transcendence.table.EsotericismTinkerTranscendenceTable;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
            ArsJeiCompat.registerCategories(registration);
        }
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ByproductEntityMeltingCategory(guiHelper));
        registration.addRecipeCategories(new TransmuteCategory(guiHelper));
        registration.addRecipeCategories(new SelenicAstrolabeRecipeCategory<>());

        registration.addRecipeCategories(new SelenicTinkerPartRecipeCategory());
        registration.addRecipeCategories(new StigmataRecipeCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;
        if (level == null){
            return;
        }
        if (ModList.get().isLoaded("ars_nouveau")){
            ArsJeiCompat.registerRecipes(registration);
        }
        RecipeManager recipeManager = level.getRecipeManager();
        RegistryAccess registryAccess = level.registryAccess();

        List<ByproductEntityMeltingRecipe> byproductEntityMelting = recipeManager
                .getAllRecipesFor(ByproductEntityMeltingRecipeRegistry.TYPE.get());
        registration.addRecipes(ByproductEntityMeltingCategory.TYPE, byproductEntityMelting);

        List<MeltingRecipe> meltingRecipes = RecipeHelper.getJEIRecipes(
                registryAccess, recipeManager,
                TinkerRecipeTypes.MELTING.get(), MeltingRecipe.class);
        registration.addRecipes(TransmuteCategory.TYPE, TransmuteCategory.createDisplays(meltingRecipes));

        List<SelenicAstrolabeRecipe> astrolabe_recipes = recipeManager
                .getAllRecipesFor(EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_TYPE.get());

        registration.addRecipes(SelenicAstrolabeRecipeCategory.TYPE, astrolabe_recipes);


        List<SelenicTinkerPartRecipe> tinker_recipes = recipeManager
                .getAllRecipesFor(EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_TINKER_TYPE.get());
        List<SelenicTinkerPartJeiRecipe> partDisplays =
                SelenicTinkerPartJeiRecipe.createAll(
                        level,
                        tinker_recipes
                );

        registration.addRecipes(
                SelenicTinkerPartRecipeCategory.TYPE,
                partDisplays
        );

        List<StigmataRecipeAdapter> stigmataRecipes = recipeManager
                .getAllRecipesFor(EsotericismTinkerRecipeTypes.STIGMATA_TYPE.get());
        List<StigmataJeiRecipe> stigmataDisplays =
                StigmataJeiDisplayFactory.createAll(
                        level,
                        stigmataRecipes
                );
        registration.addRecipes(StigmataRecipeCategory.TYPE, stigmataDisplays);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (ModList.get().isLoaded("ars_nouveau")){
            ArsJeiCompat.registerRecipeCatalysts(registration);
        }
        registration.addRecipeCatalyst(
                new ItemStack(EsotericismTinkerSelenic.armillaryCrown),
                SelenicAstrolabeRecipeCategory.TYPE,
                SelenicTinkerPartRecipeCategory.TYPE
        );

        // the controller is retextured by the ashen block tag, so every texture variant acts as a catalyst
        addTableCatalyst(
                registration,
                EsotericismTinkerSmeltery.transmuteController,
                EsotericismTinkerTagKeys.Items.ASHEN_BLOCKS,
                true,
                TransmuteCategory.TYPE
        );

        // the anvil is a material item, so every material variant acts as a catalyst
        addMaterialCatalyst(
                registration,
                EsotericismTinkerTranscendenceTable.transcendenceAnvil,
                StigmataRecipeCategory.TYPE
        );
    }

    /**
     * Adds a table with retextured variants as a catalyst, matching Tinkers' Construct.
     *
     * @param addDefault  If true, the untextured variant is added as well
     */
    private static void addTableCatalyst(
            IRecipeCatalystRegistration registration, ItemLike table, TagKey<Item> tag,
            boolean addDefault, RecipeType<?>... types) {
        List<ItemStack> catalysts = new ArrayList<>();

        if (addDefault){
            catalysts.add(new ItemStack(table));
        }

        RetexturedHelper.addTagVariants(stack -> {
            catalysts.add(stack);
            return false;
        }, table, tag);

        addCatalysts(registration, catalysts, types);
    }

    /** Adds an item with material variants as a catalyst, matching Tinkers' Construct anvils. */
    private static void addMaterialCatalyst(
            IRecipeCatalystRegistration registration, ItemLike item, RecipeType<?>... types) {
        List<ItemStack> catalysts = new ArrayList<>();
        catalysts.add(new ItemStack(item));

        if (item.asItem() instanceof IMaterialItem materialItem){
            materialItem.addVariants(catalysts::add, "");
        }

        addCatalysts(registration, catalysts, types);
    }

    /** Registers the same catalyst list for every given recipe type. */
    private static void addCatalysts(
            IRecipeCatalystRegistration registration, List<ItemStack> catalysts, RecipeType<?>... types) {
        Consumer<IIngredientAcceptor<?>> acceptor = ingredients -> ingredients.addItemStacks(catalysts);

        for (RecipeType<?> type : types) {
            registration.addRecipeCatalyst(type, acceptor);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        jeiRuntime.getIngredientManager().addIngredientsAtRuntime(TConstructJEIConstants.MODIFIER_TYPE, List.of(new ModifierEntry(EsotericismTinkerModifiers.STIGMATA, 1), new ModifierEntry(EsotericismTinkerModifiers.STIGMATA, 2), new ModifierEntry(EsotericismTinkerModifiers.STIGMATA, 3)));
        if (ModList.get().isLoaded("ars_nouveau")){
            ArsJeiCompat.registerModifierIngredients(jeiRuntime);
        }
    }
}
