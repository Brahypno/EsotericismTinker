package org.brahypno.esotericismtinker.library.recipe;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicAstrolabeRecipe;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicTinkerPartRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EsotericismTinkerRecipeTypes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, EsotericismTinker.MODID);
    private static final List<SelenicRecipeSources.Source<? extends SelenicAstrolabeRecipe>> SOURCES = new ArrayList<>();

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, EsotericismTinker.MODID);

    public static final RegistryObject<RecipeType<SelenicAstrolabeRecipe>> SELENIC_ASTROLABE_TYPE =
            TYPES.register("selenic_astrolabe", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return EsotericismTinker.MODID + ":selenic_astrolabe";
                }
            });
    public static final RegistryObject<RecipeType<SelenicTinkerPartRecipe>> SELENIC_ASTROLABE_TINKER_TYPE =
            TYPES.register("selenic_astrolabe_tinker", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return EsotericismTinker.MODID + ":selenic_astrolabe_tinker";
                }
            });

    public static final RegistryObject<RecipeSerializer<SelenicAstrolabeRecipe>> SELENIC_ASTROLABE_SERIALIZER =
            SERIALIZERS.register("selenic_astrolabe", SelenicAstrolabeRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<SelenicTinkerPartRecipe>> SELENIC_TINKER_PART_SERIALIZER =
            SERIALIZERS.register("selenic_tinker_part", SelenicTinkerPartRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
        registerSources();
    }

    public static void registerSources() {
        SelenicRecipeSources.register(
                EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_TYPE.get(),
                SelenicAstrolabeRecipe.class
        );

        SelenicRecipeSources.register(
                EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_TINKER_TYPE.get(),
                SelenicTinkerPartRecipe.class
        );
    }

    public final class SelenicRecipeSources {
        private static final List<Source<? extends SelenicAstrolabeRecipe>> SOURCES = new ArrayList<>();

        private SelenicRecipeSources() {}

        public static <R extends SelenicAstrolabeRecipe> void register(RecipeType<R> type, Class<R> recipeClass) {
            SOURCES.add(new Source<>(type, recipeClass));
        }

        public static List<SelenicAstrolabeRecipe> getRecipes(RecipeManager recipeManager) {
            List<SelenicAstrolabeRecipe> recipes = new ArrayList<>();

            for (Source<? extends SelenicAstrolabeRecipe> source : SOURCES) {
                recipes.addAll(source.getRecipes(recipeManager));
            }

            recipes.sort(Comparator
                                 .comparingInt(SelenicAstrolabeRecipe::getPriority)
                                 .reversed()
                                 .thenComparing(recipe -> recipe.getId().toString()));

            return recipes;
        }

        private record Source<R extends SelenicAstrolabeRecipe>(
                RecipeType<R> type,
                Class<R> recipeClass
        ) {
            private List<R> getRecipes(RecipeManager recipeManager) {
                return recipeManager.getAllRecipesFor(type);
            }
        }
    }

}
