package org.brahypno.esotericismtinker.library.recipe.selenic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public interface SelenicRecipeAccess {
    Level level();

    List<ItemStack> crownInputs();

    List<ItemStack> testimonyInputs();

    FluidStack inputMedium();

    int freeItemSpace(ItemStack stack);

    int freeFluidSpace(FluidStack stack);

    int countCrownInput(Ingredient ingredient);

    void consumeCrownInput(Ingredient ingredient);

    void consumeCrownInput(Ingredient ingredient, int amount);

    void consumeTestimonies(List<Ingredient> testimonies);

    void drainMedium(int amount);

    void insertItemOutput(ItemStack stack);

    void insertFluidOutput(FluidStack stack);
}