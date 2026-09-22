package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.melting.IDisplayableMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.plugin.jei.melting.AbstractMeltingCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;
import slimeknights.tconstruct.plugin.jei.util.CategoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Melting display for the Transmute mode, which exchanges main and byproduct totals.
 * <p>
 * Follows the foundry layout from Tinkers' Construct, but shows one display recipe per
 * melting recipe with the exchanged fluids already baked in, so the category itself
 * needs no special output handling.
 */
public class TransmuteCategory extends AbstractMeltingCategory {
    public static final RecipeType<IDisplayableMeltingRecipe> TYPE =
            RecipeType.create(EsotericismTinker.MODID, "transmute", IDisplayableMeltingRecipe.class);
    private static final Component TITLE = Component.translatable("jei.esotericism_tinker.transmute.title");

    public TransmuteCategory(IGuiHelper helper) {
        super(helper, TYPE, TITLE, helper.createDrawableItemLike(EsotericismTinkerSmeltery.transmuteController));
    }

    /** Wraps every melting recipe into a display recipe whose totals are exchanged. */
    public static List<IDisplayableMeltingRecipe> createDisplays(List<MeltingRecipe> recipes) {
        return recipes.stream()
                      .map(recipe -> (IDisplayableMeltingRecipe) new TransmuteDisplay(
                              recipe,
                              createTransmuteOutputs(recipe)
                      ))
                      .toList();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IDisplayableMeltingRecipe recipe, IFocusGroup focuses) {
        // input
        List<ItemStack> inputs = recipe.getInputs();
        IRecipeSlotBuilder inputSlot = builder.addInputSlot(24, 18).addItemStacks(inputs);

        // output fluids, byproducts included
        List<List<FluidStack>> fluids = recipe.getOutputWithByproducts();
        List<IRecipeSlotBuilder> slots = new ArrayList<>(fluids.size() + 1);
        CategoryUtil.drawMultipleFluids(
                builder, ignored -> RecipeIngredientRole.OUTPUT, 96, 4, 32, 32,
                fluids, FluidValues.METAL_BLOCK, Function.identity(),
                ignored -> MeltingFluidCallback.INSTANCE, slots::add);

        if (!fluids.isEmpty()){
            // first one is the main output, should always be present
            slots.get(0).setSlotName(FLUID_SLOT);

            // apply focus links to anything matching the main output size
            int size = fluids.get(0).size();
            if (size > 1){
                // remove any byproducts that have the wrong size
                for (int i = fluids.size() - 1; i >= 1; i--) {
                    if (fluids.get(i).size() != size){
                        slots.remove(i);
                    }
                }

                // add the input if its size matches
                if (inputs.size() == size){
                    slots.add(inputSlot);
                }

                if (slots.size() > 1){
                    builder.createFocusLink(slots.toArray(IRecipeSlotBuilder[]::new));
                }
            }
        }

        // fuel
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 4, 4)
               .addRichTooltipCallback(FUEL_TOOLTIP)
               .setFluidRenderer(1L, false, 12, 32)
               .addIngredients(ForgeTypes.FLUID_STACK, MeltingFuelHandler.getUsableFuels(recipe.getTemperature()));
    }

    /**
     * Applies the same exchange as the Transmute inventory:
     * the main fluid receives the original byproduct total, while byproducts share the original
     * main total in their original proportions.
     */
    static List<List<FluidStack>> createTransmuteOutputs(MeltingRecipe recipe) {
        List<List<FluidStack>> source = recipe.getOutputWithByproducts();
        if (source.isEmpty() || source.get(0).isEmpty()){
            return source;
        }

        List<FluidStack> mainVariants = copyStacks(source.get(0));
        OreRateType oreType = recipe.getOreType();
        if (oreType != null){
            for (FluidStack main : mainVariants) {
                main.setAmount(Config.COMMON.foundryOreRate.applyOreBoost(oreType, main.getAmount()));
            }
        }

        if (source.size() == 1){
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
            if (!variants.isEmpty()){
                int amount = variants.get(0).getAmount();
                byproductTotal += amount;
                if (amount > largestAmount){
                    largestAmount = amount;
                    largestIndex = i - 1;
                }
            }
        }

        if (byproductTotal <= 0){
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
            if (variants.isEmpty()){
                continue;
            }
            long share = mainTotal * variants.get(0).getAmount() / byproductTotal;
            distributed += share;
            for (FluidStack variant : variants) {
                variant.setAmount(saturatedInt(share));
            }
        }

        long remainder = mainTotal - distributed;
        if (remainder > 0 && largestIndex >= 0){
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

    /**
     * Display recipe delegating everything but the exchanged outputs to the source melting recipe,
     * so temperature, time, and ore tooltips keep matching the real recipe.
     */
    private record TransmuteDisplay(
            MeltingRecipe source,
            List<List<FluidStack>> outputWithByproducts
    ) implements IDisplayableMeltingRecipe {
        @Override
        public ResourceLocation getRecipeId() {
            return source.getRecipeId();
        }

        @Override
        public List<ItemStack> getInputs() {
            return source.getInputs();
        }

        @Override
        public List<FluidStack> getOutputs() {
            return outputWithByproducts.isEmpty() ? List.of() : outputWithByproducts.get(0);
        }

        @Override
        public OreRateType getOreType() {
            return source.getOreType();
        }

        @Override
        public int getTemperature() {
            return source.getTemperature();
        }

        @Override
        public int getTime() {
            return source.getTime();
        }

        @Override
        public boolean isTimeDynamic() {
            return source.isTimeDynamic();
        }

        @Override
        public int getTime(FluidStack fluid) {
            return source.getTime(fluid);
        }
    }
}
