package org.brahypno.esotericismtinker.tools;

import net.minecraft.client.color.item.ItemColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.esotericismtinker.EsotericismTinker;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.model.TinkerItemProperties;

import static slimeknights.tconstruct.library.client.model.tools.ToolModel.registerItemColors;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EsotericismTinkerToolClientEvents extends ClientEventBase {
    @SubscribeEvent
    static void clientSetupEvent(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            TinkerItemProperties.registerToolProperties(EsotericismTinkerTools.ritual_blade);
        });
    }

    @SubscribeEvent
    static void itemColors(RegisterColorHandlersEvent.Item event) {
        final ItemColors colors = event.getItemColors();

        // tint modifiers
        //
        registerItemColors(colors, EsotericismTinkerTools.ritual_blade);
    }
}
