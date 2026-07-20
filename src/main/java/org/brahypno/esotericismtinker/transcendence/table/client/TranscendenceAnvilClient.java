package org.brahypno.esotericismtinker.transcendence.table.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.transcendence.table.EsotericismTinkerTranscendenceTable;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TranscendenceAnvilClient {
    private TranscendenceAnvilClient() {}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> MenuScreens.register(EsotericismTinkerTranscendenceTable.transcendenceAnvilMenu.get(), TranscendenceAnvilScreen::new)); }
}
