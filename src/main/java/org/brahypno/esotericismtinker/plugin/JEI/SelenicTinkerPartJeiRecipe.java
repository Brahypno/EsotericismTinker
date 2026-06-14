package org.brahypno.esotericismtinker.plugin.JEI;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.library.recipe.MoonPhase;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicTinkerPartRecipe;
import org.brahypno.esotericismtinker.utils.PartInfoLookup;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import java.util.*;
import java.util.stream.Collectors;

public record SelenicTinkerPartJeiRecipe(
        SelenicTinkerPartRecipe source,
        int cost,
        List<ItemStack> outputs
) {
    private static final int MAX_DISPLAY_COST = 64;

    public static List<SelenicTinkerPartJeiRecipe> createAll(
            Level level,
            Collection<SelenicTinkerPartRecipe> recipes
    ) {
        List<SelenicTinkerPartJeiRecipe> displays = new ArrayList<>();
        Map<GroupKey, MutableDisplay> dynamicGroups = new LinkedHashMap<>();

        for (SelenicTinkerPartRecipe recipe : recipes) {
            if (recipe.getPartItemId() != null){
                addExactDisplays(level, displays, recipe);
            }else {
                addDynamicDisplays(level, dynamicGroups, recipe);
            }
        }

        dynamicGroups.values()
                     .stream()
                     .map(MutableDisplay::toRecipe)
                     .forEach(displays::add);

        return displays.stream()
                       .sorted(Comparator.comparing(display -> display.source().getId().toString()))
                       .toList();
    }

    private static void addExactDisplays(
            Level level,
            List<SelenicTinkerPartJeiRecipe> displays,
            SelenicTinkerPartRecipe recipe
    ) {
        for (int cost = 1; cost <= MAX_DISPLAY_COST; cost++) {
            List<ItemStack> outputs = exactOutputsForCost(level, recipe, cost);

            if (outputs.isEmpty()){
                continue;
            }

            displays.add(new SelenicTinkerPartJeiRecipe(
                    recipe,
                    cost,
                    List.copyOf(outputs)
            ));

            return;
        }
    }

    private static void addDynamicDisplays(
            Level level,
            Map<GroupKey, MutableDisplay> groups,
            SelenicTinkerPartRecipe recipe
    ) {
        for (int cost = 1; cost <= MAX_DISPLAY_COST; cost++) {
            List<ItemStack> outputs = dynamicOutputsForCost(level, recipe, cost);

            if (outputs.isEmpty()){
                continue;
            }

            GroupKey key = new GroupKey(recipe, cost);
            int finalCost = cost;
            groups.computeIfAbsent(key, ignored -> new MutableDisplay(recipe, finalCost))
                  .addOutputs(outputs);
        }
    }

    private static List<ItemStack> dynamicOutputsForCost(
            Level level,
            SelenicTinkerPartRecipe recipe,
            int cost
    ) {
        List<ItemStack> outputs = new ArrayList<>();

        for (ToolPartItem part : PartInfoLookup.runtimeParts(
                level,
                recipe.getMaterialId(),
                recipe.getStatIds(),
                cost
        )) {
            outputs.add(PartInfoLookup.withMaterial(part, recipe.getMaterialId()));
        }

        return outputs;
    }

    private static List<ItemStack> exactOutputsForCost(
            Level level,
            SelenicTinkerPartRecipe recipe,
            int cost
    ) {
        ToolPartItem part = PartInfoLookup.exactPart(
                recipe.getPartItemId(),
                recipe.getMaterialId(),
                recipe.getStatIds()
        );

        if (part == null){
            return List.of();
        }

        PartInfoLookup.CostedPart result = PartInfoLookup.exactPartWithCost(
                level,
                recipe.getMaterialId(),
                part,
                cost
        );

        if (result.isEmpty() || result.cost() != cost){
            return List.of();
        }

        return List.of(result.stack());
    }

    private record GroupKey(
            int cost,
            String material,
            int priority,
            int duration,
            int elevationMin,
            int elevationMax,
            String lunarPhases,
            String input,
            String testimonies,
            String medium,
            String mediumOutput,
            String mediumOutputMode,
            boolean consumeMedium
    ) {
        private GroupKey(SelenicTinkerPartRecipe recipe, int cost) {
            this(
                    cost,
                    recipe.getMaterialId().toString(),
                    recipe.getPriority(),
                    recipe.getDuration(),
                    recipe.getElevation().min(),
                    recipe.getElevation().max(),
                    phasesKey(recipe),
                    ingredientKey(recipe.getInput()),
                    ingredientsKey(recipe.getTestimonies()),
                    String.valueOf(recipe.getMedium()),
                    fluidStackKey(recipe.getMediumOutput()),
                    String.valueOf(recipe.getMediumOutputMode()),
                    recipe.shouldConsumeMedium()
            );
        }
    }

    private static final class MutableDisplay {
        private final SelenicTinkerPartRecipe source;
        private final int cost;
        private final List<ItemStack> outputs = new ArrayList<>();

        private MutableDisplay(SelenicTinkerPartRecipe source, int cost) {
            this.source = source;
            this.cost = cost;
        }

        private void addOutputs(Collection<ItemStack> stacks) {
            for (ItemStack stack : stacks) {
                addOutput(stack);
            }
        }

        private void addOutput(ItemStack stack) {
            if (stack.isEmpty() || containsOutput(stack)){
                return;
            }

            outputs.add(stack.copy());
        }

        private boolean containsOutput(ItemStack stack) {
            for (ItemStack existing : outputs) {
                if (ItemStack.isSameItemSameTags(existing, stack)){
                    return true;
                }
            }

            return false;
        }

        private SelenicTinkerPartJeiRecipe toRecipe() {
            return new SelenicTinkerPartJeiRecipe(
                    source,
                    cost,
                    List.copyOf(outputs)
            );
        }
    }

    private static String phasesKey(SelenicTinkerPartRecipe recipe) {
        return recipe.getLunarPhases()
                     .stream()
                     .map(MoonPhase::serializedName)
                     .sorted()
                     .collect(Collectors.joining("|"));
    }

    private static String ingredientsKey(List<Ingredient> ingredients) {
        return ingredients.stream()
                          .map(SelenicTinkerPartJeiRecipe::ingredientKey)
                          .collect(Collectors.joining("&&"));
    }

    private static String ingredientKey(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems())
                     .map(SelenicTinkerPartJeiRecipe::stackKey)
                     .sorted()
                     .collect(Collectors.joining("|"));
    }

    private static String stackKey(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        CompoundTag tag = stack.getTag();

        return id + "#" +
               stack.getCount() + "#" +
               (tag == null ? "" : tag.toString());
    }

    private static String fluidStackKey(FluidStack stack) {
        if (stack.isEmpty()){
            return "empty";
        }

        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        CompoundTag tag = stack.getTag();

        return id + "#" +
               stack.getAmount() + "#" +
               (tag == null ? "" : tag.toString());
    }
}