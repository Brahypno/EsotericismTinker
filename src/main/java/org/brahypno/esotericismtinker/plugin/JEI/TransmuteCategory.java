package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.plugin.jei.AlloyRecipeCategory;
import slimeknights.tconstruct.plugin.jei.melting.AbstractMeltingCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Melting display for the Transmute mode, which exchanges main and byproduct totals. */
public class TransmuteCategory extends AbstractMeltingCategory {
    public static final RecipeType<MeltingRecipe> TYPE =
            RecipeType.create(EsotericismTinker.MODID, "transmute", MeltingRecipe.class);
    private static final Component TITLE = Component.translatable("jei.esotericism_tinker.transmute.title");

    private final IDrawable icon;

    public TransmuteCategory(IGuiHelper helper) {
        super(helper);
        icon = helper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(EsotericismTinkerSmeltery.transmuteController));
    }

    @Override
    public RecipeType<MeltingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MeltingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 18).addIngredients(recipe.getInput());

        AlloyRecipeCategory.drawVariableFluids(
                builder, RecipeIngredientRole.OUTPUT, 96, 4, 32, 32,
                createTransmuteOutputs(recipe), FluidValues.METAL_BLOCK, Function.identity(),
                ignored -> MeltingFluidCallback.INSTANCE);

        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 4, 4)
                .addTooltipCallback(FUEL_TOOLTIP)
                .setFluidRenderer(1, false, 12, 32)
                .addIngredients(ForgeTypes.FLUID_STACK, MeltingFuelHandler.getUsableFuels(recipe.getTemperature()));
    }

    /**
     * Applies the same exchange as the Transmute inventory:
     * the main fluid receives the original byproduct total, while byproducts share the original
     * main total in their original proportions.
     */
    static List<List<FluidStack>> createTransmuteOutputs(MeltingRecipe recipe) {
        List<List<FluidStack>> source = recipe.getOutputWithByproducts();
        if (source.isEmpty() || source.get(0).isEmpty()) {
            return source;
        }

        List<FluidStack> mainVariants = copyStacks(source.get(0));
        OreRateType oreType = recipe.getOreType();
        if (oreType != null) {
            for (FluidStack main : mainVariants) {
                main.setAmount(Config.COMMON.foundryOreRate.applyOreBoost(oreType, main.getAmount()));
            }
        }

        if (source.size() == 1) {
            return List.of(mainVariants);
        }

        long mainTotal = mainVariants.get(0).getAmount();
        long byproductTotal = 0;
        int largestIndex = -1;
        int largestAmount = 0;
        List<List<FluidStack>> byproducts = new ArrayList<>(source.size() - 1);
        for (int i = 1; i < source.size(); i++) {
            List<FluidStack> variants = copyStacks(source.get(i));
            byproducts.add(variants);
            if (!variants.isEmpty()) {
                int amount = variants.get(0).getAmount();
                byproductTotal += amount;
                if (amount > largestAmount) {
                    largestAmount = amount;
                    largestIndex = i - 1;
                }
            }
        }

        if (byproductTotal <= 0) {
            List<List<FluidStack>> unchanged = new ArrayList<>(source.size());
            unchanged.add(mainVariants);
            unchanged.addAll(byproducts);
            return unchanged;
        }

        for (FluidStack main : mainVariants) {
            main.setAmount(saturatedInt(byproductTotal));
        }

        long distributed = 0;
        for (List<FluidStack> variants : byproducts) {
            if (variants.isEmpty()) {
                continue;
            }
            long share = mainTotal * variants.get(0).getAmount() / byproductTotal;
            distributed += share;
            for (FluidStack variant : variants) {
                variant.setAmount(saturatedInt(share));
            }
        }

        long remainder = mainTotal - distributed;
        if (remainder > 0 && largestIndex >= 0) {
            for (FluidStack variant : byproducts.get(largestIndex)) {
                variant.setAmount(saturatedInt((long) variant.getAmount() + remainder));
            }
        }

        List<List<FluidStack>> result = new ArrayList<>(source.size());
        result.add(mainVariants);
        result.addAll(byproducts);
        return result;
    }

    private static List<FluidStack> copyStacks(List<FluidStack> stacks) {
        return stacks.stream().map(FluidStack::copy).toList();
    }

    private static int saturatedInt(long amount) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, amount));
    }
}
