package org.brahypno.esotericismtinker.plugin.JEI;

import java.awt.Color;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.plugin.JEI.StigmataJeiRecipe.StigmataToolDisplay;
import org.brahypno.esotericismtinker.transcendence.table.EsotericismTinkerTranscendenceTable;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.util.CategoryUtil;

/**
 * Stigmata category using the exact Tinkers' Construct modifier-station layout.
 * <p>
 * The tool slot drives the display: {@link #onDisplayedIngredientsUpdate} rewrites the slots around
 * it with the options that are valid for the tool JEI is currently showing, so nothing is frozen and
 * no combination on screen mixes up unrelated tools, parts, and materials.
 */
public final class StigmataRecipeCategory extends AbstractRecipeCategory<StigmataJeiRecipe> {
    public static final RecipeType<StigmataJeiRecipe> TYPE = RecipeType.create(
            EsotericismTinker.MODID, "stigmata", StigmataJeiRecipe.class);
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("tconstruct", "textures/gui/jei/tinker_station.png");

    /** Slot names, needed to find the slots again once JEI starts cycling. */
    private static final String TOOL_SLOT = "stigmata_tool";
    private static final String PART_SLOT = "stigmata_part";
    private static final String MATERIAL_1_SLOT = "stigmata_material_1";
    private static final String MATERIAL_2_SLOT = "stigmata_material_2";
    private static final String MATERIAL_3_SLOT = "stigmata_material_3";
    private static final String RESULT_SLOT = "stigmata_result";

    private final IDrawable background;
    private final ModifierIngredientRenderer modifierRenderer = new ModifierIngredientRenderer(124, 10);

    public StigmataRecipeCategory(IGuiHelper helper) {
        super(TYPE, Component.translatable("jei.esotericism_tinker.stigmata"),
              helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                                              new ItemStack(EsotericismTinkerTranscendenceTable.transcendenceAnvil)),
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
        // The lists here hold every option of every tool, both so JEI indexes the recipe for
        // ingredient lookups and so the display is complete before the first update hook runs.
        builder.addSlot(RecipeIngredientRole.INPUT, 3, 33)
               .addItemStacks(recipe.parts())
               .setSlotName(PART_SLOT);
        builder.addSlot(RecipeIngredientRole.INPUT, 25, 15)
               .addItemStacks(recipe.materials())
               .setSlotName(MATERIAL_1_SLOT);
        builder.addSlot(RecipeIngredientRole.INPUT, 47, 33)
               .addItemStacks(StigmataJeiRecipe.rotate(recipe.materials(), 1))
               .setSlotName(MATERIAL_2_SLOT);
        builder.addSlot(RecipeIngredientRole.INPUT, 43, 58)
               .addItemStacks(StigmataJeiRecipe.rotate(recipe.materials(), 2))
               .setSlotName(MATERIAL_3_SLOT);
        builder.addSlot(RecipeIngredientRole.INPUT, 7, 58)
               .addItemStacks(recipe.selectors());
        builder.addSlot(RecipeIngredientRole.CATALYST, 25, 38)
               .addItemStacks(recipe.toolsBefore())
               .setSlotName(TOOL_SLOT);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 105, 34)
               .addItemStacks(recipe.results())
               .setSlotName(RESULT_SLOT);

        // Expose the real modifier ingredient, matching TConstruct's modifier category.
        // This supplies both the rendered name and JEI's modifier-name search target.
        builder.addSlot(RecipeIngredientRole.OUTPUT, 3, 3)
               .setCustomRenderer(TConstructJEIConstants.MODIFIER_TYPE, modifierRenderer)
               .addIngredient(TConstructJEIConstants.MODIFIER_TYPE, recipe.modifier());
    }

    @Override
    public void onDisplayedIngredientsUpdate(StigmataJeiRecipe recipe, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
        IRecipeSlotDrawable toolSlot = CategoryUtil.findSlot(recipeSlots, TOOL_SLOT);
        if (null == toolSlot){
            return;
        }

        // looking up a result tool drives the display from the tool that produces it
        StigmataToolDisplay display = null;
        IFocus<ItemStack> focus = focuses.getItemStackFocuses().findFirst().orElse(null);
        if (null != focus && RecipeIngredientRole.OUTPUT == focus.getRole()){
            display = recipe.displayForResult(focus.getTypedValue().getIngredient());
        }

        if (null == display){
            ItemStack tool = toolSlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
            display = recipe.displayFor(tool);
        }

        if (null == display){
            return;
        }

        setOptions(recipeSlots, PART_SLOT, display.parts());
        setOptions(recipeSlots, MATERIAL_1_SLOT, display.materials());
        setOptions(recipeSlots, MATERIAL_2_SLOT, StigmataJeiRecipe.rotate(display.materials(), 1));
        setOptions(recipeSlots, MATERIAL_3_SLOT, StigmataJeiRecipe.rotate(display.materials(), 2));
        setOptions(recipeSlots, RESULT_SLOT, display.results());
    }

    /** Replaces what a slot offers without touching the recipe itself. */
    private static void setOptions(List<IRecipeSlotDrawable> recipeSlots, String name, List<ItemStack> options) {
        IRecipeSlotDrawable slot = CategoryUtil.findSlot(recipeSlots, name);
        if (null == slot){
            return;
        }

        slot.clearDisplayOverrides();
        slot.createDisplayOverrides().addItemStacks(options);
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName(StigmataJeiRecipe recipe) {
        return recipe.id();
    }
}
