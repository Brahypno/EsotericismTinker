package org.brahypno.esotericismtinker.library.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public final class IngredientStackUtil {
    private IngredientStackUtil() {}

    public static boolean matchesAll(List<ItemStack> stacks, List<Ingredient> ingredients) {
        return consumeAll(stacks, ingredients, true);
    }

    public static boolean consumeAll(List<ItemStack> stacks, List<Ingredient> ingredients, boolean simulate) {
        List<ItemStack> remaining = stacks.stream().map(ItemStack::copy).toList();

        for (Ingredient ingredient : ingredients) {
            if (!consumeOne(remaining, ingredient, false)){
                return false;
            }
        }

        if (!simulate){
            for (int i = 0; i < stacks.size(); i++) {
                stacks.get(i).setCount(remaining.get(i).getCount());
            }
        }

        return true;
    }

    public static boolean consumeOne(List<ItemStack> stacks, Ingredient ingredient, boolean simulate) {
        int needed = -1;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || !ingredient.test(stack)){
                continue;
            }

            if (needed < 0){
                needed = getRequiredCount(ingredient, stack);
            }

            int used = Math.min(needed, stack.getCount());

            if (!simulate){
                stack.shrink(used);
            }

            needed -= used;

            if (needed <= 0){
                return true;
            }
        }

        return needed == 0;
    }

    public static int getRequiredCount(Ingredient ingredient, ItemStack candidate) {
        ItemStack[] displays = ingredient.getItems();
        int fallback = 1;

        for (ItemStack display : displays) {
            if (display.isEmpty()){
                continue;
            }

            fallback = Math.max(fallback, display.getCount());

            if (ItemStack.isSameItemSameTags(display, candidate)){
                return Math.max(1, display.getCount());
            }
        }

        return fallback;
    }
}