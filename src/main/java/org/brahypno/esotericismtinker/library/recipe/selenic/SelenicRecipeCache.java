package org.brahypno.esotericismtinker.library.recipe.selenic;


import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;

import java.util.*;

public final class SelenicRecipeCache {
    private static final Map<RecipeManager, Entry> CACHE = new WeakHashMap<>();

    private static final Comparator<SelenicAstrolabeRecipe> PRIORITY_ORDER =
            Comparator.comparingInt(SelenicAstrolabeRecipe::getPriority).reversed()
                      .thenComparing(Comparator.comparingInt(SelenicAstrolabeRecipe::getSpecificity).reversed())
                      .thenComparing(recipe -> recipe.getId().toString());

    private SelenicRecipeCache() {}

    public static List<SelenicAstrolabeRecipe> getSortedRecipes(Level level) {
        return getSortedRecipes(level.getRecipeManager());
    }

    public static List<SelenicAstrolabeRecipe> getSortedRecipes(MinecraftServer server) {
        return getSortedRecipes(server.getRecipeManager());
    }

    public static List<SelenicAstrolabeRecipe> getSortedRecipes(RecipeManager manager) {
        Entry entry = CACHE.get(manager);

        if (entry != null){
            return entry.sortedRecipes();
        }

        List<SelenicAstrolabeRecipe> recipes = EsotericismTinkerRecipeTypes.SelenicRecipeSources.getRecipes(manager);

        recipes.sort(PRIORITY_ORDER);

        Entry created = new Entry(List.copyOf(recipes));
        CACHE.put(manager, created);

        return created.sortedRecipes();
    }

    public static Optional<SelenicAstrolabeRecipe> findBest(Level level, SelenicAstrolabeContext context) {
        for (SelenicAstrolabeRecipe recipe : getSortedRecipes(level)) {
            if (recipe.matches(context)){
                return Optional.of(recipe);
            }
        }

        return Optional.empty();
    }

    public static void clear() {
        CACHE.clear();
    }

    private record Entry(List<SelenicAstrolabeRecipe> sortedRecipes) {}
}
