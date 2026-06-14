package org.brahypno.esotericismtinker.utils;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderRecipe;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PartInfoLookup {
    public static final int DEFAULT_COST = 5;

    private static final Map<ToolPartItem, Integer> RUNTIME_COSTS = new HashMap<>();
    private static boolean runtimeBuilt = false;

    private PartInfoLookup() {}

    public static CostedPart runtimePartWithCost(
            Level level,
            MaterialVariantId material,
            MaterialStatsId statsId,
            int maxCost,
            RandomSource random) {
        return runtimePartWithCost(level, material, List.of(statsId), maxCost, random);
    }

    public static CostedPart runtimePartWithCost(
            Level level,
            MaterialVariantId material,
            List<MaterialStatsId> statsIds,
            int maxCost,
            RandomSource random) {
        ensureRuntime(level);

        if (!MaterialRegistry.isFullyLoaded() || maxCost <= 0){
            return CostedPart.empty();
        }

        for (int candidateCost = maxCost; candidateCost > 0; candidateCost--) {
            List<ToolPartItem> parts = runtimeParts(material, statsIds, candidateCost);

            if (parts.isEmpty()){
                continue;
            }

            ToolPartItem part = random == null
                                ? parts.get(0)
                                : parts.get(random.nextInt(parts.size()));

            return new CostedPart(withMaterial(part, material), candidateCost);
        }

        return CostedPart.empty();
    }

    public static CostedPart exactPartWithCost(
            Level level,
            MaterialVariantId material,
            ToolPartItem part,
            int maxCost) {
        ensureRuntime(level);

        if (!MaterialRegistry.isFullyLoaded() || maxCost <= 0){
            return CostedPart.empty();
        }

        int cost = runtimeCost(part);

        if (cost <= 0 || cost > maxCost){
            return CostedPart.empty();
        }

        return new CostedPart(withMaterial(part, material), cost);
    }

    public static int runtimeCost(Level level, ToolPartItem part) {
        ensureRuntime(level);
        return runtimeCost(part);
    }

    public static void clearRuntime() {
        RUNTIME_COSTS.clear();
        runtimeBuilt = false;
    }

    private static void ensureRuntime(Level level) {
        if (!runtimeBuilt){
            rebuildRuntime(level.getRecipeManager(), level.registryAccess());
        }
    }

    private static void rebuildRuntime(RecipeManager recipes, RegistryAccess access) {
        RUNTIME_COSTS.clear();

        recipes.getAllRecipesFor(TinkerRecipeTypes.PART_BUILDER.get())
               .stream()
               .sorted(Comparator.comparing(IPartBuilderRecipe::getCost))
               .forEach(recipe -> putRuntimeCost(recipe, access));

        runtimeBuilt = true;
    }

    private static void putRuntimeCost(IPartBuilderRecipe recipe, RegistryAccess access) {
        ItemStack output = recipe.getResultItem(access);

        if (output.getItem() instanceof ToolPartItem part){
            RUNTIME_COSTS.putIfAbsent(part, recipe.getCost());
        }
    }

    private static int runtimeCost(ToolPartItem part) {
        return RUNTIME_COSTS.getOrDefault(part, DEFAULT_COST);
    }

    public static List<ToolPartItem> runtimeParts(
            MaterialVariantId material,
            MaterialStatsId statsId,
            int cost) {
        return runtimeParts(material, List.of(statsId), cost);
    }

    public static List<ToolPartItem> runtimeParts(
            MaterialVariantId material,
            List<MaterialStatsId> statsIds,
            int cost) {
        return ForgeRegistries.ITEMS.getValues()
                                    .stream()
                                    .filter(item -> item instanceof ToolPartItem)
                                    .map(item -> (ToolPartItem) item)
                                    .filter(part -> statsIds.contains(part.getStatType()))
                                    .filter(part -> part.canUseMaterial(material.getId()))
                                    .filter(part -> runtimeCost(part) == cost)
                                    .toList();
    }

    public static ToolPartItem exactPart(
            ResourceLocation id,
            MaterialVariantId material,
            MaterialStatsId statsId) {
        return exactPart(id, material, List.of(statsId));
    }

    public static ToolPartItem exactPart(
            ResourceLocation id,
            MaterialVariantId material,
            List<MaterialStatsId> statsIds) {
        Item item = ForgeRegistries.ITEMS.getValue(id);

        if (item instanceof ToolPartItem part
            && statsIds.contains(part.getStatType())
            && part.canUseMaterial(material.getId())){
            return part;
        }

        return null;
    }

    public static ItemStack withMaterial(ToolPartItem part, MaterialVariantId material) {
        ItemStack stack = part.withMaterial(material);
        stack.setCount(1);
        return stack;
    }

    public static List<ToolPartItem> runtimeParts(
            Level level,
            MaterialVariantId material,
            MaterialStatsId statsId,
            int cost
    ) {
        return runtimeParts(level, material, List.of(statsId), cost);
    }

    public static List<ToolPartItem> runtimeParts(
            Level level,
            MaterialVariantId material,
            List<MaterialStatsId> statsIds,
            int cost
    ) {
        ensureRuntime(level);
        return runtimeParts(material, statsIds, cost);
    }

    public record CostedPart(ItemStack stack, int cost) {
        public static CostedPart empty() {
            return new CostedPart(ItemStack.EMPTY, 0);
        }

        public boolean isEmpty() {
            return stack.isEmpty() || cost <= 0;
        }
    }
}