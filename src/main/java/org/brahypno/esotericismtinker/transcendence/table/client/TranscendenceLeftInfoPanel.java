package org.brahypno.esotericismtinker.transcendence.table.client;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import org.brahypno.esotericismtinker.transcendence.table.menu.TranscendenceAnvilMenu;
import slimeknights.tconstruct.tables.client.inventory.module.InfoPanelScreen;

/** Info panel sized to hold the complete transcendence navigation and allocation list. */
final class TranscendenceLeftInfoPanel
        extends InfoPanelScreen<TranscendenceAnvilScreen, TranscendenceAnvilMenu> {
    TranscendenceLeftInfoPanel(
            TranscendenceAnvilScreen parent,
            TranscendenceAnvilMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(parent, menu, inventory, title);
        imageWidth = TranscendenceLeftStationLayout.WIDTH;
        imageHeight = TranscendenceLeftStationLayout.HEIGHT;
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        parent.renderReceptionScrollbar(graphics);
    }
}
