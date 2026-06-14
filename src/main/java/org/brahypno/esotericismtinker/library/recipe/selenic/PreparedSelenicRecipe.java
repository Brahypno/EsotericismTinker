package org.brahypno.esotericismtinker.library.recipe.selenic;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public record PreparedSelenicRecipe(
        SelenicFailure failure,
        ItemStack itemOutput,
        FluidStack fluidOutput,
        int crownInputCost
) {
    public static PreparedSelenicRecipe ok(ItemStack itemOutput, FluidStack fluidOutput) {
        return new PreparedSelenicRecipe(
                SelenicFailure.NONE,
                itemOutput.copy(),
                fluidOutput.copy(),
                -1);
    }

    public static PreparedSelenicRecipe ok(ItemStack itemOutput, FluidStack fluidOutput, int crownInputCost) {
        return new PreparedSelenicRecipe(
                SelenicFailure.NONE,
                itemOutput.copy(),
                fluidOutput.copy(),
                crownInputCost);
    }

    public static PreparedSelenicRecipe fail(SelenicFailure failure) {
        return new PreparedSelenicRecipe(
                failure,
                ItemStack.EMPTY,
                FluidStack.EMPTY,
                -1);
    }

    public boolean isOk() {
        return failure == SelenicFailure.NONE;
    }

    public void apply(SelenicAstrolabeRecipe recipe, SelenicRecipeAccess access) {
        if (!isOk()){
            return;
        }

        if (crownInputCost >= 0){
            access.consumeCrownInput(recipe.getInput(), crownInputCost);
        }else {
            access.consumeCrownInput(recipe.getInput());
        }

        access.consumeTestimonies(recipe.getTestimonies());

        if (recipe.shouldConsumeMedium()){
            access.drainMedium(recipe.getRequiredMediumAmount(access.inputMedium()));
        }

        if (!itemOutput.isEmpty()){
            access.insertItemOutput(itemOutput.copy());
        }

        if (!fluidOutput.isEmpty()){
            access.insertFluidOutput(fluidOutput.copy());
        }
    }
}