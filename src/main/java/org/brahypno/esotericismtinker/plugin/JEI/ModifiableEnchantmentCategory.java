package org.brahypno.esotericismtinker.plugin.JEI;

import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.compat.ars_nouveau.recipe.ModifiableEnchantmentRecipe;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientRenderer;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ModifiableEnchantmentCategory implements IRecipeCategory<ModifiableEnchantmentRecipe> {
    public static final RecipeType<ModifiableEnchantmentRecipe> RECIPE_TYPE =
            RecipeType.create(EsotericismTinker.MODID, "modifiable_enchantment", ModifiableEnchantmentRecipe.class);

    private static final int WIDTH = 142;
    private static final int HEIGHT = 112;

    /**
     * Left/down shift for the whole Ars apparatus layout, to leave room for TConstruct text
     */
    private static final int APPARATUS_SHIFT_X = -8;
    private static final int APPARATUS_SHIFT_Y = 6;

    /**
     * Ars-style center catalyst/tool, shifted together with pedestal ring
     */
    private static final int CENTER_X = 48 + APPARATUS_SHIFT_X;
    private static final int CENTER_Y = 45 + APPARATUS_SHIFT_Y;

    /**
     * Pedestal ring, same shift as center
     */
    private static final int START_X = 48 + APPARATUS_SHIFT_X;
    private static final int START_Y = 13 + APPARATUS_SHIFT_Y;

    /**
     * Output: shift right/down symmetrically from original output position
     */
    private static final int OUTPUT_SHIFT_X = 8;
    private static final int OUTPUT_SHIFT_Y = 6;
    private static final int OUTPUT_X = 104 + OUTPUT_SHIFT_X;
    private static final int OUTPUT_Y = 20 + OUTPUT_SHIFT_Y;

    /**
     * TConstruct modifier text area
     */
    private static final int MODIFIER_X = 4;
    private static final int MODIFIER_Y = 4;
    private static final int LEVEL_CENTER_X = 86;
    private static final int LEVEL_Y = 16;

    /**
     * Bottom layer: TConstruct slot + Ars source
     */

    private static final int SOURCE_X = 8;
    private static final int SOURCE_Y = 100 + 4;

    private static final int REQUIREMENTS_X = 90;
    private static final int REQUIREMENTS_Y = 100 - 4;

    private static final int SLOT_X = 108;
    private static final int SLOT_Y = 100 - 4;

    private static final String KEY_MIN = TConstruct.makeTranslationKey("jei", "modifiers.level.min");
    private static final String KEY_MAX = TConstruct.makeTranslationKey("jei", "modifiers.level.max");
    private static final String KEY_RANGE = TConstruct.makeTranslationKey("jei", "modifiers.level.range");
    private static final String KEY_EXACT = TConstruct.makeTranslationKey("jei", "modifiers.level.exact");


    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable requirements;
    private final Component title = Component.translatable("jei.esotericism_tinker.modifiable_enchantment");

    private final ModifierIngredientRenderer modifierRenderer = new ModifierIngredientRenderer(124, 10);
    protected static final ResourceLocation TCON_BACKGROUND =
            TConstruct.getResource("textures/gui/jei/tinker_station.png");

    public ModifiableEnchantmentCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(BlockRegistry.ENCHANTING_APP_BLOCK)
        );
        this.requirements = guiHelper.createDrawable(TCON_BACKGROUND, 128, 17, 16, 16);
    }

    @Override
    public @NotNull RecipeType<ModifiableEnchantmentRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
            @NotNull IRecipeLayoutBuilder builder,
            @NotNull ModifiableEnchantmentRecipe recipe,
            @NotNull IFocusGroup focuses
    ) {
        // TConstruct modifier display: name + tooltip are handled by ModifierIngredientRenderer.
        builder.addSlot(RecipeIngredientRole.OUTPUT, MODIFIER_X, MODIFIER_Y)
               .setCustomRenderer(TConstructJEIConstants.MODIFIER_TYPE, modifierRenderer)
               .addIngredient(TConstructJEIConstants.MODIFIER_TYPE, getDisplayResult(recipe));

        // Ars-style center catalyst/tool.
        List<ItemStack> toolWithoutModifier = getInputDisplayTools(recipe);
        List<ItemStack> toolWithModifier = getOutputDisplayTools(recipe, toolWithoutModifier);

        addSinglePartLookupTools(builder, toolWithoutModifier);
        applyToolFocus(focuses, toolWithoutModifier, toolWithModifier);

        builder.addSlot(RecipeIngredientRole.CATALYST, CENTER_X, CENTER_Y)
               .addItemStacks(toolWithoutModifier);

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
               .addItemStacks(toolWithModifier);

        // Ars-style circular pedestal item placement.
        int inputCount = recipe.getPedestalIngredients().size();
        if (inputCount > 0){
            double angleBetweenEach = 360.0D / inputCount;
            Vec2 point = new Vec2(START_X, START_Y);
            Vec2 center = new Vec2(CENTER_X, CENTER_Y);

            for (int i = 0; i < inputCount; i++) {
                builder.addSlot(RecipeIngredientRole.INPUT, (int) point.x, (int) point.y)
                       .addIngredients(recipe.getPedestalIngredients().get(i));

                point = rotatePointAbout(point, center, angleBetweenEach);
            }
        }

        // TConstruct slot display: use TConstruct slot ingredient type and renderer.
        SlotCount slots = recipe.getSlots();
        if (slots != null){
            builder.addSlot(RecipeIngredientRole.INPUT, SLOT_X, SLOT_Y)
                   .setCustomRenderer(TConstructJEIConstants.SLOT_TYPE, SlotIngredientRenderer.INPUT)
                   .addIngredient(TConstructJEIConstants.SLOT_TYPE, slots);
        }
    }

    @Override
    public void draw(
            @NotNull ModifiableEnchantmentRecipe recipe,
            @NotNull IRecipeSlotsView recipeSlotsView,
            @NotNull GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        Font font = Minecraft.getInstance().font;

        Component levelText = getLevelText(recipe);
        if (levelText != null){
            graphics.drawString(
                    font,
                    levelText,
                    LEVEL_CENTER_X - font.width(levelText) / 2,
                    LEVEL_Y,
                    Color.GRAY.getRGB(),
                    false
            );
        }

        // Ars source display, same bottom layer as TConstruct requirements and slot.
        if (recipe.getSourceCost() > 0){
            graphics.drawString(
                    font,
                    Component.translatable("ars_nouveau.source", recipe.getSourceCost()),
                    SOURCE_X,
                    SOURCE_Y,
                    10,
                    false
            );
        }

        // TConstruct requirements icon: prerequisites / conflicts / modifier constraints.
        if (getRequirementError(recipe) != null){
            requirements.draw(graphics, REQUIREMENTS_X, REQUIREMENTS_Y);
        }

        // TConstruct slotless display: if slots exist, JEI slot renderer handles it.
        if (recipe.getSlots() == null){
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(SLOT_X, SLOT_Y, 0);
            SlotIngredientRenderer.INPUT.render(graphics, null);
            pose.popPose();
        }
    }

    @Override
    public void getTooltip(
            @NotNull ITooltipBuilder tooltip,
            @NotNull ModifiableEnchantmentRecipe recipe,
            @NotNull IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY
    ) {
        int checkX = (int) mouseX;
        int checkY = (int) mouseY;

        Component requirementError = getRequirementError(recipe);
        if (requirementError != null && GuiUtil.isHovered(checkX, checkY, REQUIREMENTS_X, REQUIREMENTS_Y, 16, 16)){
            tooltip.add(requirementError);
        }
    }

    private static ModifierEntry getDisplayResult(ModifiableEnchantmentRecipe recipe) {
        return new ModifierEntry(recipe.getResultModifier(), recipe.getLevel().min());
    }

    @Nullable
    private static Component getLevelText(ModifiableEnchantmentRecipe recipe) {
        int min = recipe.getLevel().min();
        int max = recipe.getLevel().max();

        // This is copied from TConstruct semantics:
        // min == 1 prefers "Max Level" over "Exact Level".
        if (min == 1){
            if (max < ModifierEntry.VALID_LEVEL.max()){
                return Component.translatable(KEY_MAX, max);
            }
            return null;
        }

        if (min == max){
            return Component.translatable(KEY_EXACT, min);
        }

        if (max == ModifierEntry.VALID_LEVEL.max()){
            return Component.translatable(KEY_MIN, min);
        }

        return Component.translatable(KEY_RANGE, min, max);
    }

    private static List<ItemStack> getInputDisplayTools(ModifiableEnchantmentRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();

        int inputLevel = recipe.getLevel().min() - 1;

        for (ItemStack input : recipe.getTools().getItems()) {
            if (input.isEmpty()){
                continue;
            }

            ItemStack stack = IModifiableDisplay.getDisplayStack(input);
            if (!(stack.getItem() instanceof IModifiable)){
                continue;
            }

            if (inputLevel > 0){
                ToolStack tool = ToolStack.from(stack);
                tool.addModifier(recipe.getResultModifier(), inputLevel);
                tool.updateStack(stack);
            }

            stacks.add(stack);
        }

        if (stacks.isEmpty()){
            stacks.add(ItemStack.EMPTY);
        }

        return stacks;
    }

    private static List<ItemStack> getOutputDisplayTools(
            ModifiableEnchantmentRecipe recipe,
            List<ItemStack> inputTools
    ) {
        List<ItemStack> outputs = new ArrayList<>();

        for (ItemStack input : inputTools) {
            if (input.isEmpty()){
                continue;
            }

            ItemStack output = recipe.applyModifierToTool(input);
            if (!output.isEmpty()){
                outputs.add(output);
            }
        }

        if (outputs.isEmpty()){
            outputs.add(ItemStack.EMPTY);
        }

        return outputs;
    }

    private static void addSinglePartLookupTools(IRecipeLayoutBuilder builder, List<ItemStack> toolWithoutModifier) {
        for (ItemStack stack : toolWithoutModifier) {
            if (stack.is(TinkerTags.Items.SINGLEPART_TOOL) && stack.getItem() instanceof IModifiable modifiable){
                builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
                       .addItemStacks(getLookupTools(modifiable));
            }
        }
    }

    private static void applyToolFocus(
            IFocusGroup focuses,
            List<ItemStack> toolWithoutModifier,
            List<ItemStack> toolWithModifier
    ) {
        IFocus<ItemStack> focus = focuses.getFocuses(VanillaTypes.ITEM_STACK)
                                         .filter(f -> f.getRole() == RecipeIngredientRole.CATALYST)
                                         .findFirst()
                                         .orElse(null);

        if (focus == null){
            return;
        }

        Item item = focus.getTypedValue().getIngredient().getItem();

        ItemStack focusedWithout = null;
        for (ItemStack stack : toolWithoutModifier) {
            if (stack.is(item)){
                focusedWithout = stack;
                break;
            }
        }

        ItemStack focusedWith = null;
        for (ItemStack stack : toolWithModifier) {
            if (stack.is(item)){
                focusedWith = stack;
                break;
            }
        }

        if (focusedWithout != null){
            toolWithoutModifier.clear();
            toolWithoutModifier.add(focusedWithout);
        }

        if (focusedWith != null){
            toolWithModifier.clear();
            toolWithModifier.add(focusedWith);
        }
    }

    private static Vec2 rotatePointAbout(Vec2 in, Vec2 about, double degrees) {
        double rad = degrees * Math.PI / 180.0D;
        double newX = Math.cos(rad) * (in.x - about.x) - Math.sin(rad) * (in.y - about.y) + about.x;
        double newY = Math.sin(rad) * (in.x - about.x) + Math.cos(rad) * (in.y - about.y) + about.y;
        return new Vec2((float) newX, (float) newY);
    }

    private static final Map<IModifiable, List<ItemStack>> LOOKUP_CACHE = new ConcurrentHashMap<>();

    private static final Function<IModifiable, List<ItemStack>> LOOKUP_GETTER = modifiable -> {
        List<ItemStack> variants = new ArrayList<>();
        ToolBuildHandler.addVariants(variants::add, modifiable, "");
        return variants;
    };

    private static List<ItemStack> getLookupTools(IModifiable modifiable) {
        return LOOKUP_CACHE.computeIfAbsent(modifiable, LOOKUP_GETTER);
    }

    public static void clearTconLookupCache() {
        LOOKUP_CACHE.clear();
        SlotIngredientRenderer.clearCache();
    }

    private static Component getRequirementError(ModifiableEnchantmentRecipe recipe) {
        ModifierEntry result = getDisplayResult(recipe);
        return result.getHook(ModifierHooks.REQUIREMENTS).requirementsError(result);
    }
}