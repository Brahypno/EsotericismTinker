package org.brahypno.esotericismtinker.library.client.book.content;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.screen.book.ArrowButton;
import slimeknights.mantle.client.screen.book.element.ArrowElement;
import slimeknights.mantle.client.screen.book.element.BookElement;

import java.util.ArrayList;
import java.util.Collections;

/** Cycles between all Stigmata recipes loaded from data packs. */
final class StigmataRecipeCycleElement extends ArrowElement {
    StigmataRecipeCycleElement(
            int x,
            int y,
            int color,
            int hoverColor,
            ContentStigmata content,
            BookData book,
            ArrayList<BookElement> elements
    ) {
        super(
                x,
                y,
                ArrowButton.ArrowType.RIGHT,
                color,
                hoverColor,
                button -> content.nextRecipe(book, elements)
        );
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (button != null && isHovered(mouseX, mouseY)) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
            button.onPress();
        }
    }

    @Override
    public void drawOverlay(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTicks,
            Font fontRenderer
    ) {
        if (isHovered(mouseX, mouseY)) {
            drawTooltip(
                    graphics,
                    Collections.singletonList(
                            Component.translatable("gui.tconstruct.manual.cycle.recipes")
                    ),
                    mouseX,
                    mouseY,
                    fontRenderer
            );
        }
    }
}
