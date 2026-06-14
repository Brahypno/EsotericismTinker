package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicAstrolabeRecipe;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicTinkerPartRecipe;

import java.util.Arrays;
import java.util.List;

public class SelenicTinkerPartRecipeCategory
        extends SelenicAstrolabeRecipeCategory<SelenicTinkerPartJeiRecipe> {
    public static final RecipeType<SelenicTinkerPartJeiRecipe> TYPE = RecipeType.create(
            EsotericismTinker.MODID,
            "selenic_tinker_part",
            SelenicTinkerPartJeiRecipe.class
    );

    private static final int RULE_X = 8;
    private static final int RULE_Y = 132;

    public SelenicTinkerPartRecipeCategory() {
        super(
                TYPE,
                Component.translatable("jei.esotericism_tinker.selenic_tinker_part")
        );
    }

    @Override
    protected SelenicAstrolabeRecipe unwrap(SelenicTinkerPartJeiRecipe display) {
        return display.source();
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            SelenicTinkerPartJeiRecipe display,
            IFocusGroup focuses
    ) {
        SelenicTinkerPartRecipe recipe = display.source();

        addCostedCrownInput(builder, display);
        addTestimonyRing(builder, recipe);
        addCostedOutputs(builder, display);
        addFluidSlots(builder, recipe);
    }

    private void addCostedCrownInput(
            IRecipeLayoutBuilder builder,
            SelenicTinkerPartJeiRecipe display
    ) {
        ItemStack[] rawInputs = display.source().getInput().getItems();
        if (rawInputs.length == 0){
            return;
        }

        List<ItemStack> inputs = Arrays.stream(rawInputs)
                                       .map(ItemStack::copy)
                                       .peek(stack -> stack.setCount(display.cost()))
                                       .toList();

        builder.addSlot(RecipeIngredientRole.INPUT, CROWN_INPUT_X, CROWN_INPUT_Y)
               .addIngredients(VanillaTypes.ITEM_STACK, inputs)
               .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                       Component.translatable(
                               "jei.esotericism_tinker.selenic_tinker_part.input_cost",
                               display.cost()
                       )
               ));
    }

    private void addCostedOutputs(
            IRecipeLayoutBuilder builder,
            SelenicTinkerPartJeiRecipe display
    ) {
        if (display.outputs().isEmpty()){
            return;
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, CENTER_X - 8, CENTER_Y - 8)
               .addIngredients(VanillaTypes.ITEM_STACK, display.outputs())
               .addRichTooltipCallback((slot, tooltip) -> {
                   tooltip.add(Component.translatable(
                           "jei.esotericism_tinker.selenic_tinker_part.output_cost",
                           display.cost()
                   ));

                   if (display.outputs().size() > 1){
                       tooltip.add(Component.translatable(
                               "jei.esotericism_tinker.selenic_tinker_part.random_one"
                       ));
                   }
               });
    }
}