package org.brahypno.esotericismtinker.plugin.JEI;

import java.awt.Color;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierIngredientRenderer;

/**
 * Stigmata category using the exact Tinkers' Construct modifier-station layout.
 */
public final class StigmataRecipeCategory extends AbstractRecipeCategory<StigmataJeiRecipe> {
    public static final RecipeType<StigmataJeiRecipe> TYPE = RecipeType.create(
            EsotericismTinker.MODID, "stigmata", StigmataJeiRecipe.class);
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("tconstruct", "textures/gui/jei/tinker_station.png");

    private final IDrawable background;
    private final ModifierIngredientRenderer modifierRenderer = new ModifierIngredientRenderer(124, 10);

    public StigmataRecipeCategory(IGuiHelper helper) {
        super(TYPE, Component.translatable("jei.esotericism_tinker.stigmata"),
              helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                                              new ItemStack(org.brahypno.esotericismtinker.transcendence.table.EsotericismTinkerTranscendenceTable.transcendenceAnvil)),
              128, 77);
        this.background = helper.createDrawable(BACKGROUND, 0, 0, 128, 77);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, StigmataJeiRecipe recipe, IFocusGroup focuses) {
        Component stageText = Component.translatable("jei.esotericism_tinker.stigmata.stage", recipe.source().data().targetStage().index());
        builder.addText(stageText, 80, 9)
               .setPosition(46, 16)
               .setColor(Color.GRAY.getRGB())
               .setTextAlignment(HorizontalAlignment.CENTER);
    }

    @Override
    public void draw(StigmataJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StigmataJeiRecipe recipe, IFocusGroup focuses) {
        // Same five ingredient positions as TConstruct ModifierRecipeCategory.
        IRecipeSlotBuilder part = builder.addSlot(RecipeIngredientRole.INPUT, 3, 33)
                .addItemStacks(recipe.parts());
        IRecipeSlotBuilder material1 = builder.addSlot(RecipeIngredientRole.INPUT, 25, 15)
                .addItemStacks(recipe.material1());
        IRecipeSlotBuilder material2 = builder.addSlot(RecipeIngredientRole.INPUT, 47, 33)
                .addItemStacks(recipe.material2());
        IRecipeSlotBuilder material3 = builder.addSlot(RecipeIngredientRole.INPUT, 43, 58)
                .addItemStacks(recipe.material3());
        IRecipeSlotBuilder selector = builder.addSlot(RecipeIngredientRole.INPUT, 7, 58)
                .addItemStacks(recipe.selectors());
        IRecipeSlotBuilder before = builder.addSlot(RecipeIngredientRole.CATALYST, 25, 38)
                .addItemStacks(recipe.toolsBefore());
        IRecipeSlotBuilder after = builder.addSlot(RecipeIngredientRole.OUTPUT, 105, 34)
                .addItemStacks(recipe.toolsAfter());

        // Expose the real modifier ingredient, matching TConstruct's modifier category.
        // This supplies both the rendered name and JEI's modifier-name search target.
        builder.addSlot(RecipeIngredientRole.OUTPUT, 3, 3)
                .setCustomRenderer(TConstructJEIConstants.MODIFIER_TYPE, modifierRenderer)
                .addIngredient(TConstructJEIConstants.MODIFIER_TYPE, recipe.modifier());

        // Every list in StigmataJeiRecipe represents the same indexed display rows.
        // Without a focus link JEI cycles each slot independently, producing combinations
        // whose part, materials, tool state, and output do not belong to one another.
        builder.createFocusLink(part, material1, material2, material3, selector, before, after);
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName(StigmataJeiRecipe recipe) {
        return recipe.id();
    }
}
