package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated.StartDirection;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.smeltery.recipe.entitymelting.ByproductEntityMeltingRecipe;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.mantle.plugin.jei.MantleJEIConstants;
import slimeknights.mantle.plugin.jei.entity.EntityIngredientRenderer;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.plugin.jei.AlloyRecipeCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;
import slimeknights.tconstruct.plugin.jei.util.FluidTooltipCallback;

import java.awt.*;
import java.util.List;
import java.util.function.Function;

public class ByproductEntityMeltingCategory implements IRecipeCategory<ByproductEntityMeltingRecipe> {
    public static final ResourceLocation BACKGROUND_LOC = EsotericismTinker.getLocation("textures/gui/jei/melting.png");
    public static final RecipeType<ByproductEntityMeltingRecipe> TYPE = RecipeType.create(
            EsotericismTinker.MODID, "byproduct_entity_melting", ByproductEntityMeltingRecipe.class);
    private static final Component TITLE = TConstruct.makeTranslation("jei", "entity_melting.title");
    private static final String KEY_PER_HEARTS = TConstruct.makeTranslationKey("jei", "entity_melting.per_hearts");
    private static final Component TOOLTIP_PER_HEART = Component.translatable(
            TConstruct.makeTranslationKey("jei", "entity_melting.per_heart")).withStyle(ChatFormatting.GRAY);
    private final EntityIngredientRenderer entityRenderer = new EntityIngredientRenderer(32);
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable tank;

    public ByproductEntityMeltingCategory(IGuiHelper helper) {
        background = helper.createDrawable(BACKGROUND_LOC, 0, 41, 150, 62);
        icon = helper.createDrawable(BACKGROUND_LOC, 174, 41, 16, 16);
        arrow = helper.drawableBuilder(BACKGROUND_LOC, 150, 41, 24, 17).buildAnimated(200, StartDirection.LEFT, false);
        tank = helper.createDrawable(BACKGROUND_LOC, 150, 74, 16, 16);
    }

    @Override
    public RecipeType<ByproductEntityMeltingRecipe> getRecipeType() {return TYPE;}

    @Override
    public Component getTitle() {return TITLE;}

    @Override
    @SuppressWarnings({"removal"})
    public IDrawable getBackground() {return background;}

    @Override
    public IDrawable getIcon() {return icon;}

    @Override
    public void draw(
            ByproductEntityMeltingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 71, 21);
        String damage = Float.toString(recipe.getDamage() / 2f);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, damage, 84 - font.width(damage), 8, Color.RED.getRGB(), false);
    }

    @Override
    @SuppressWarnings({"removal"})
    public void setRecipe(IRecipeLayoutBuilder builder, ByproductEntityMeltingRecipe recipe, IFocusGroup focuses) {
        EntityIngredient input = recipe.getIngredient();
        IIngredientAcceptor<?> entities = builder.addSlot(RecipeIngredientRole.INPUT, 19, 11)
                                                 .setCustomRenderer(MantleJEIConstants.ENTITY_TYPE, entityRenderer)
                                                 .addIngredients(MantleJEIConstants.ENTITY_TYPE, input.getDisplay());
        IIngredientAcceptor<?> eggs = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(input.getEggs());
        builder.createFocusLink(entities, eggs);

        AlloyRecipeCategory.drawVariableFluids(
                builder, RecipeIngredientRole.OUTPUT, 115, 11, 16, 32,
                recipe.getOutputWithByproducts(), FluidValues.INGOT * 2, Function.identity(),
                ignored -> new EntityFluidTooltip(recipe.getDamage()));

        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 43)
               .setFluidRenderer(1, false, 16, 16)
               .setOverlay(tank, 0, 0)
               .addTooltipCallback(FluidTooltipCallback.NO_AMOUNT)
               .addIngredients(ForgeTypes.FLUID_STACK, MeltingFuelHandler.getUsableFuels(1));
    }

    @Override
    public ResourceLocation getRegistryName(ByproductEntityMeltingRecipe recipe) {return recipe.getId();}

    private record EntityFluidTooltip(int damage) implements FluidTooltipCallback {
        @Override
        public void onFluidTooltip(FluidStack fluid, IRecipeSlotView recipeSlotView, List<Component> tooltip) {
            FluidTooltipHandler.appendMaterial(fluid, tooltip);
            if (damage == 2)
                tooltip.add(TOOLTIP_PER_HEART);
            else
                tooltip.add(Component.translatable(KEY_PER_HEARTS, damage / 2f).withStyle(ChatFormatting.GRAY));
        }
    }
}
