package org.brahypno.esotericismtinker.library.client.book.content;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.plugin.JEI.StigmataJeiDisplayFactory;
import org.brahypno.esotericismtinker.plugin.JEI.StigmataJeiRecipe;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.element.ImageData;
import slimeknights.mantle.client.book.data.element.TextData;
import slimeknights.mantle.client.screen.book.ArrowButton;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.client.screen.book.element.BookElement;
import slimeknights.mantle.client.screen.book.element.ImageElement;
import slimeknights.mantle.client.screen.book.element.TextElement;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.book.content.ContentModifier;
import slimeknights.tconstruct.library.client.book.elements.TinkerItemElement;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modifier book page for Stigmata.
 *
 * <p>The heading, description, effects, tool preview, and input layout deliberately
 * match {@link ContentModifier}. Only recipe discovery differs: Stigmata recipes
 * are not {@code ModifierRecipe}s, so their live data-pack recipes are adapted to
 * {@link IDisplayModifierRecipe} solely for book rendering.</p>
 */
public final class ContentStigmata extends ContentModifier {
    public static final ResourceLocation ID =
            new ResourceLocation(EsotericismTinker.MODID, "stigmata");

    private static final String KEY_EFFECTS =
            TConstruct.makeTranslationKey("book", "modifiers.effect");

    private transient List<IDisplayModifierRecipe> stigmataRecipes;
    private transient int currentStigmataRecipe;
    private final transient List<BookElement> recipeElements = new ArrayList<>();

    /** Localized page title supplied by the book JSON. */
    public String title;

    @Override
    public String getTitle() {
        return title == null || title.isBlank() ? super.getTitle() : title;
    }

    @Override
    public void load() {
        if (stigmataRecipes != null) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) {
            stigmataRecipes = Collections.emptyList();
            return;
        }

        stigmataRecipes = StigmataJeiDisplayFactory.createAll(
                        level,
                        level.getRecipeManager().getAllRecipesFor(
                                EsotericismTinkerRecipeTypes.STIGMATA_TYPE.get()
                        )
                ).stream()
                .map(StigmataBookRecipe::new)
                .map(IDisplayModifierRecipe.class::cast)
                .toList();
    }

    @Override
    public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
        addTitle(list, getTitle(), true, getModifier().getColor());

        int y = getTitleHeight();
        int h = more_text_space
                ? BookScreen.PAGE_HEIGHT / 2 - 5
                : BookScreen.PAGE_HEIGHT * 2 / 7;
        list.add(new TextElement(5, y, BookScreen.PAGE_WIDTH - 10, h, text));

        if (effects != null && effects.length > 0) {
            TextData head = new TextData(I18n.get(KEY_EFFECTS));
            head.underlined = true;
            list.add(new TextElement(
                    5, y + h, BookScreen.PAGE_WIDTH / 2 - 5,
                    BookScreen.PAGE_HEIGHT - h - 20, head
            ));

            List<TextData> effectData = Lists.newArrayList();
            for (String effect : effects) {
                effectData.add(new TextData("● "));
                effectData.add(new TextData(effect));
                effectData.add(TextData.LINEBREAK);
            }
            list.add(new TextElement(
                    5, y + 14 + h, BookScreen.PAGE_WIDTH / 2 + 7,
                    BookScreen.PAGE_HEIGHT - h - 20, effectData
            ));
        }

        if (stigmataRecipes.isEmpty()) {
            return;
        }

        if (stigmataRecipes.size() > 1) {
            int color = book.appearance.structureButtonColor;
            int hoverColor = book.appearance.structureButtonColorHovered;
            list.add(new StigmataRecipeCycleElement(
                    BookScreen.PAGE_WIDTH - ArrowButton.ArrowType.RIGHT.w - 32,
                    160,
                    color,
                    hoverColor,
                    this,
                    book,
                    list
            ));
        }
        buildStigmataRecipe(
                book,
                list,
                stigmataRecipes.get(currentStigmataRecipe),
                null
        );
    }

    private void buildStigmataRecipe(
            BookData book,
            ArrayList<BookElement> list,
            IDisplayModifierRecipe recipe,
            @Nullable BookScreen parent
    ) {
        int inputs = recipe.getInputCount();
        ImageData slotsImage = IMG_SLOTS[Math.min(inputs - 1, 4)];
        if (inputs > 5) {
            TConstruct.LOG.warn(
                    "Too many inputs in Stigmata book recipe {}, size {}",
                    recipe.getRecipeId(),
                    inputs
            );
        }

        int[] slotsX = inputs == 4 ? SLOTS_X_4 : SLOTS_X;
        int[] slotsY = inputs == 4 ? SLOTS_Y_4 : SLOTS_Y;
        int imageX = BookScreen.PAGE_WIDTH / 2 + 49 - slotsImage.width / 2;
        int imageY = BookScreen.PAGE_HEIGHT / 2 + 50 - slotsImage.height / 2;

        addRecipeElement(list, new ImageElement(
                imageX + (slotsImage.width - IMG_TABLE.width) / 2,
                imageY - 24, -1, -1, IMG_TABLE
        ), parent);
        addRecipeElement(list, new ImageElement(
                imageX, imageY, -1, -1, slotsImage, book.appearance.slotColor
        ), parent);

        List<ItemStack> tools = recipe.getToolWithModifier();
        if (!tools.isEmpty()) {
            addRecipeElement(list, new TinkerItemElement(
                    imageX + (slotsImage.width - 16) / 2,
                    imageY - 24, 1.0F, getDemoTools(tools)
            ), parent);
        }
        addRecipeElement(list, new ImageElement(
                imageX + (slotsImage.width - 22) / 2,
                imageY - 27, -1, -1, IMG_SLOT_1, 0xffffff
        ), parent);

        for (int i = 0; i < Math.min(inputs, 5); i++) {
            addRecipeElement(list, new TinkerItemElement(
                    imageX + slotsX[i],
                    imageY + slotsY[i],
                    1.0F,
                    getDemoTools(recipe.getDisplayItems(i))
            ), parent);
        }
    }

    private void addRecipeElement(
            ArrayList<BookElement> list,
            BookElement element,
            @Nullable BookScreen parent
    ) {
        element.parent = parent;
        recipeElements.add(element);
        list.add(element);
    }

    @Override
    public void nextRecipe(BookData book, ArrayList<BookElement> list) {
        if (stigmataRecipes.isEmpty()) {
            return;
        }

        currentStigmataRecipe =
                (currentStigmataRecipe + 1) % stigmataRecipes.size();
        BookScreen parent = recipeElements.isEmpty()
                ? null
                : recipeElements.get(0).parent;
        list.removeAll(recipeElements);
        recipeElements.clear();
        buildStigmataRecipe(
                book,
                list,
                stigmataRecipes.get(currentStigmataRecipe),
                parent
        );
    }

    /** Makes a non-standard Stigmata recipe look like a modifier recipe to the book renderer. */
    private record StigmataBookRecipe(StigmataJeiRecipe recipe)
            implements IDisplayModifierRecipe {
        @Override
        public ResourceLocation getRecipeId() {
            return recipe.id();
        }

        @Override
        public int getInputCount() {
            return 5;
        }

        @Override
        public List<ItemStack> getDisplayItems(int slot) {
            return switch (slot) {
                case 0 -> recipe.parts();
                case 1 -> recipe.material1();
                case 2 -> recipe.material2();
                case 3 -> recipe.material3();
                case 4 -> recipe.selectors();
                default -> List.of();
            };
        }

        @Override
        public List<ItemStack> getToolWithoutModifier() {
            return recipe.toolsBefore();
        }

        @Override
        public List<ItemStack> getToolWithModifier() {
            return recipe.toolsAfter();
        }

        @Override
        public ModifierEntry getDisplayResult() {
            return recipe.modifier();
        }
    }
}
