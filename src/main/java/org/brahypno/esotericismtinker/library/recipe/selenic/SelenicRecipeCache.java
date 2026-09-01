package org.brahypno.esotericismtinker.library.recipe.selenic;


import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
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
        return getEntry(manager).sortedRecipes();
    }

    public static Optional<SelenicAstrolabeRecipe> getById(Level level, ResourceLocation id) {
        return Optional.ofNullable(getEntry(level.getRecipeManager()).byId().get(id));
    }

    private static synchronized Entry getEntry(RecipeManager manager) {
        Entry entry = CACHE.get(manager);

        if (entry != null){
            return entry;
        }

        List<SelenicAstrolabeRecipe> recipes = EsotericismTinkerRecipeTypes.SelenicRecipeSources.getRecipes(manager);

        recipes.sort(PRIORITY_ORDER);

        Map<ResourceLocation, SelenicAstrolabeRecipe> byId = new HashMap<>();
        for (SelenicAstrolabeRecipe recipe : recipes) {
            // Keep the same first match as the priority-sorted list if sources share an ID.
            byId.putIfAbsent(recipe.getId(), recipe);
        }
        Entry created = new Entry(List.copyOf(recipes), Map.copyOf(byId));
        CACHE.put(manager, created);

        return created;
    }

    public static Optional<SelenicAstrolabeRecipe> findBest(Level level, SelenicAstrolabeContext context) {
        for (SelenicAstrolabeRecipe recipe : getSortedRecipes(level)) {
            if (recipe.matches(context)){
                return Optional.of(recipe);
            }
        }

        return Optional.empty();
    }

    public static synchronized void clear() {
        CACHE.clear();
    }

    private record Entry(List<SelenicAstrolabeRecipe> sortedRecipes,
                         Map<ResourceLocation, SelenicAstrolabeRecipe> byId) {}
}
