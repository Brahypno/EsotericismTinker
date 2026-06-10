package org.brahypno.esotericismtinker.common.event.client;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.Config;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemTooltip {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {

        if (e.getItemStack().is(EsotericismTinkerTagKeys.Items.TRANSMUTE_HEATER)){
            e.getToolTip().add(Component.translatable("tooltip.esotericism_tinker.transmute_heater", Config.transmuteHeaterTemperature));
        }
        if (e.getItemStack().is(EsotericismTinkerTagKeys.Items.TRANSMUTE_ACCEL)){
            e.getToolTip().add(Component.translatable("tooltip.esotericism_tinker.transmute_accel", Config.transmuteAcceleratorTemperature));
        }
    }
}
