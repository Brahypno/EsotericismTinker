package org.brahypno.esotericismtinker.plugin.JEI;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.recipe.FluidOutputMode;
import org.brahypno.esotericismtinker.library.recipe.IntRange;
import org.brahypno.esotericismtinker.library.recipe.MoonPhase;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicAstrolabeRecipe;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import static org.brahypno.esotericismtinker.library.recipe.selenic.SelenicAstrolabeRecipe.isIngredientEmpty;
import static org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic.astrolabeSpine;

public class SelenicAstrolabeRecipeCategory<R> implements IRecipeCategory<R> {
    public static final RecipeType<SelenicAstrolabeRecipe> TYPE = RecipeType.create(
            EsotericismTinker.MODID,
            "selenic_astrolabe",
            SelenicAstrolabeRecipe.class
    );

    protected static final ResourceLocation MOON_PHASES =
            new ResourceLocation("textures/environment/moon_phases.png");
    protected static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(
            EsotericismTinker.MODID,
            "textures/gui/jei/selenic.png"
    );
    protected static final int TEXTURE_WIDTH = 200;
    protected static final int TEXTURE_HEIGHT = 200;


    protected static final int WIDTH = 176;
    protected static final int HEIGHT = 150;
    protected static final int CENTER_X = 88;
    protected static final int CENTER_Y = 87;
    protected static final int TESTIMONY_RADIUS = 42;
    protected static final int CROWN_INPUT_X = CENTER_X - 8;
    protected static final int CROWN_INPUT_Y = CENTER_Y - 8 - TESTIMONY_RADIUS - 26;
    protected static final int INPUT_FLUID_X = 8;
    protected static final int OUTPUT_FLUID_X = 152;
    protected static final int FLUID_Y = 112;
    protected static final int MOON_X = 8;
    protected static final int MOON_Y = 8;
    protected static final int MOON_SIZE = 16;
    protected static final int ELEVATION_X = 132;
    protected static final int ELEVATION_Y = 8;
    protected static final int ELEVATION_ICON_SIZE = 16;

    protected static final int CENTER_IMAGE_SIZE = 96;
    protected static final int CENTER_IMAGE_X = CENTER_X - CENTER_IMAGE_SIZE / 2;
    protected static final int CENTER_IMAGE_Y = CENTER_Y - CENTER_IMAGE_SIZE / 2;

    protected final RecipeType<R> type;
    protected final Component title;

    @SuppressWarnings("unchecked")
    public SelenicAstrolabeRecipeCategory() {
        this(
                (RecipeType<R>) TYPE,
                Component.translatable("jei.esotericism_tinker.selenic_astrolabe")
        );
    }

    protected SelenicAstrolabeRecipeCategory(RecipeType<R> type, Component title) {
        this.type = type;
        this.title = title;
    }

    @Override
    public RecipeType<R> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Nullable
    @Override
    public mezz.jei.api.gui.drawable.IDrawable getIcon() {
        return null;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    protected SelenicAstrolabeRecipe unwrap(R recipe) {
        return (SelenicAstrolabeRecipe) recipe;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, R display, IFocusGroup focuses) {
        SelenicAstrolabeRecipe recipe = unwrap(display);
        addCrownInput(builder, recipe);
        addTestimonyRing(builder, recipe);
        addCenterOutput(builder, recipe);
        addFluidSlots(builder, recipe);
    }

    protected void addCrownInput(IRecipeLayoutBuilder builder, SelenicAstrolabeRecipe recipe) {
        Ingredient input = recipe.getInput();
        if (isIngredientEmpty(input)){
            return;
        }

        builder.addSlot(RecipeIngredientRole.INPUT, CROWN_INPUT_X, CROWN_INPUT_Y)
               .addIngredients(input);
    }

    protected void addTestimonyRing(IRecipeLayoutBuilder builder, SelenicAstrolabeRecipe recipe) {
        List<Ingredient> testimonies = recipe.getTestimonies();
        if (testimonies.isEmpty()){
            return;
        }

        List<int[]> points = ringPoints(
                testimonies.size(),
                CENTER_X - 8,
                CENTER_Y - 8,
                TESTIMONY_RADIUS
        );

        for (int i = 0; i < Math.min(testimonies.size(), points.size()); i++) {
            int[] point = points.get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, point[0], point[1])
                   .addIngredients(testimonies.get(i));
        }
    }

    protected void addCenterOutput(IRecipeLayoutBuilder builder, SelenicAstrolabeRecipe recipe) {
        List<ItemStack> outputs = recipe.getDisplayOutputs();
        if (outputs.isEmpty()){
            return;
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, CENTER_X - 8, CENTER_Y - 8)
               .addIngredients(VanillaTypes.ITEM_STACK, outputs);
    }

    protected void addFluidSlots(IRecipeLayoutBuilder builder, SelenicAstrolabeRecipe recipe) {
        FluidIngredient medium = recipe.getMedium();
        if (medium != FluidIngredient.EMPTY){
            List<FluidStack> fluids = medium.getFluids();
            if (!fluids.isEmpty()){
                builder.addSlot(RecipeIngredientRole.INPUT, INPUT_FLUID_X, FLUID_Y)
                       .addIngredients(ForgeTypes.FLUID_STACK, fluids)
                       .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                           Component line = recipe.shouldConsumeMedium()
                                            ? Component.translatable(
                                   "jei.esotericism_tinker.selenic_astrolabe.medium_consumes",
                                   describeMediumAmount(medium)
                           )
                                            : Component.translatable(
                                   "jei.esotericism_tinker.selenic_astrolabe.medium_requires",
                                   describeMediumAmount(medium)
                           );
                           tooltip.add(line);
                       });
            }
        }

        FluidStack mediumOutput = recipe.getMediumOutput();
        if (!mediumOutput.isEmpty()){
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_FLUID_X, FLUID_Y)
                   .addIngredient(ForgeTypes.FLUID_STACK, mediumOutput)
                   .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                       Component line = recipe.getMediumOutputMode() == FluidOutputMode.OVER_TIME
                                        ? Component.translatable(
                               "jei.esotericism_tinker.selenic_astrolabe.fluid_over_time",
                               mediumOutput.getAmount()
                       )
                                        : Component.translatable(
                               "jei.esotericism_tinker.selenic_astrolabe.fluid_instant",
                               mediumOutput.getAmount()
                       );
                       tooltip.add(line);
                   });
        }
    }

    @Override
    public void draw(
            R display,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        drawBackground(guiGraphics);
        SelenicAstrolabeRecipe recipe = unwrap(display);
        drawMoonIfRestricted(recipe, guiGraphics);
        drawElevationIcon(recipe, guiGraphics);
        drawDurationText(recipe, guiGraphics);
        drawConditionalText(recipe, guiGraphics);
    }

    protected void drawDurationText(SelenicAstrolabeRecipe recipe, GuiGraphics guiGraphics) {
        if (recipe.getDuration() <= 0){
            return;
        }

        Font font = Minecraft.getInstance().font;
        Component text = Component.translatable(
                "jei.esotericism_tinker.selenic_astrolabe.duration",
                describeDurationSeconds(recipe.getDuration())
        );

        guiGraphics.drawString(font, text, WIDTH - 20, HEIGHT - 12, 0xFFE8E0FF, false);
    }

    protected Component describeDurationSeconds(int ticks) {
        double seconds = ticks / 20.0D;
        if (ticks % 20 == 0){
            return Component.literal(String.valueOf(ticks / 20));
        }

        return Component.literal(String.format(java.util.Locale.ROOT, "%.1f", seconds));
    }


    protected void drawMoonIfRestricted(SelenicAstrolabeRecipe recipe, GuiGraphics guiGraphics) {
        if (!hasMoonRestriction(recipe)){
            return;
        }

        MoonPhase phase = getDisplayedPhase(recipe);
        int index = phase.vanillaId();
        int u = (index % 4) * 16;
        int v = (index / 4) * 16;

        RenderSystem.setShaderTexture(0, MOON_PHASES);
        guiGraphics.blit(MOON_PHASES, MOON_X, MOON_Y, u, v, MOON_SIZE, MOON_SIZE, 64, 32);
    }

    protected void drawElevationIcon(SelenicAstrolabeRecipe recipe, GuiGraphics guiGraphics) {
        if (!hasElevationRestriction(recipe.getElevation())){
            return;
        }

        Font font = Minecraft.getInstance().font;
        guiGraphics.renderItem(new ItemStack(astrolabeSpine.get()), ELEVATION_X, ELEVATION_Y);
        guiGraphics.drawString(
                font,
                describeElevationCompat(recipe.getElevation()),
                ELEVATION_X + 18,
                ELEVATION_Y + 4,
                0xFFE8E0FF,
                false
        );
    }

    protected boolean hasElevationRestriction(IntRange range) {
        return !(range.min() == 0 && range.max() == Integer.MAX_VALUE);
    }

    protected Component describeElevationCompat(IntRange range) {
        if (range.max() == Integer.MAX_VALUE){
            return Component.literal(range.min() + "+");
        }

        if (range.min() == range.max()){
            return Component.literal(String.valueOf(range.min()));
        }

        return Component.literal(range.min() + "-" + range.max());
    }

    protected void drawConditionalText(SelenicAstrolabeRecipe recipe, GuiGraphics guiGraphics) {
        Font font = Minecraft.getInstance().font;
        int color = 0xFFE8E0FF;

        if (recipe.getPriority() != 0){
            guiGraphics.drawString(
                    font,
                    Component.translatable(
                            "jei.esotericism_tinker.selenic_astrolabe.priority",
                            recipe.getPriority()
                    ),
                    8,
                    HEIGHT - 12,
                    color,
                    false
            );
        }
    }

    @Override
    public void getTooltip(
            ITooltipBuilder tooltip,
            R display,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY
    ) {
        SelenicAstrolabeRecipe recipe = unwrap(display);

        if (hasMoonRestriction(recipe) && isMouseOverMoon(mouseX, mouseY)){
            MoonPhase phase = getDisplayedPhase(recipe);
            tooltip.add(Component.translatable(
                    "jei.esotericism_tinker.selenic_astrolabe.moon_phase",
                    Component.translatable("moon_phase.esotericism_tinker." + phase.serializedName())
            ));
        }

        if (hasElevationRestriction(recipe.getElevation()) && isMouseOverElevation(mouseX, mouseY)){
            tooltip.add(Component.translatable(
                    "jei.esotericism_tinker.selenic_astrolabe.elevation",
                    describeElevation(recipe.getElevation())
            ));
        }
    }

    protected boolean isMouseOverElevation(double mouseX, double mouseY) {
        return mouseX >= ELEVATION_X
               && mouseX < ELEVATION_X + 36
               && mouseY >= ELEVATION_Y
               && mouseY < ELEVATION_Y + ELEVATION_ICON_SIZE;
    }

    protected boolean isMouseOverMoon(double mouseX, double mouseY) {
        return mouseX >= MOON_X
               && mouseX < MOON_X + MOON_SIZE
               && mouseY >= MOON_Y
               && mouseY < MOON_Y + MOON_SIZE;
    }

    protected boolean hasMoonRestriction(SelenicAstrolabeRecipe recipe) {
        return recipe.getLunarPhases().size() < MoonPhase.values().length;
    }

    protected MoonPhase getDisplayedPhase(SelenicAstrolabeRecipe recipe) {
        EnumSet<MoonPhase> phases = recipe.getLunarPhases();
        if (phases.isEmpty()){
            return MoonPhase.FULL_MOON;
        }

        List<MoonPhase> ordered = phases.stream()
                                        .sorted(Comparator.comparingInt(MoonPhase::vanillaId))
                                        .toList();

        int index = (int) ((Util.getMillis() / 1000L) % ordered.size());
        return ordered.get(index);
    }

    protected List<int[]> ringPoints(int count, int centerX, int centerY, int radius) {
        int diagonal = (int) Math.round(radius * 0.70710678D);
        List<int[]> points = new ArrayList<>();

        points.add(new int[]{centerX + radius, centerY});
        points.add(new int[]{centerX - radius, centerY});
        points.add(new int[]{centerX, centerY - radius});
        points.add(new int[]{centerX, centerY + radius});
        points.add(new int[]{centerX + diagonal, centerY - diagonal});
        points.add(new int[]{centerX - diagonal, centerY - diagonal});
        points.add(new int[]{centerX + diagonal, centerY + diagonal});
        points.add(new int[]{centerX - diagonal, centerY + diagonal});

        return points.subList(0, Math.min(count, points.size()));
    }

    protected Component describeElevation(IntRange range) {
        if (range.max() == Integer.MAX_VALUE){
            return Component.literal(range.min() + "+");
        }

        return Component.literal(range.min() + " - " + range.max());
    }

    protected Component describeMediumAmount(FluidIngredient ingredient) {
        List<FluidStack> fluids = ingredient.getFluids();
        if (fluids.isEmpty()){
            return Component.literal("?");
        }

        int min = Integer.MAX_VALUE;
        int max = 0;

        for (FluidStack stack : fluids) {
            if (!stack.isEmpty()){
                min = Math.min(min, stack.getAmount());
                max = Math.max(max, stack.getAmount());
            }
        }

        if (min == Integer.MAX_VALUE){
            return Component.literal("?");
        }

        if (min == max){
            return Component.literal(min + " mB");
        }

        return Component.literal(min + "-" + max + " mB");
    }

    protected void drawBackground(GuiGraphics guiGraphics) {
        guiGraphics.blit(BACKGROUND_TEXTURE, CENTER_IMAGE_X, CENTER_IMAGE_Y, CENTER_IMAGE_SIZE, CENTER_IMAGE_SIZE, 0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                         TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}