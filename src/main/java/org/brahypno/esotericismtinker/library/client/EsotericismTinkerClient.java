package org.brahypno.esotericismtinker.library.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.esotericismtinker.library.client.book.EsotericismBook;
import org.brahypno.esotericismtinker.library.tools.EsotericismSlotType;

import static org.brahypno.esotericismtinker.EsotericismTinker.MODID;
import static slimeknights.tconstruct.shared.CommonsClientEvents.unicodeFontRender;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EsotericismTinkerClient {
    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        EsotericismSlotType.init();
        EsotericismBook.initBook();
        EsotericismBook.HYPNAGOGIC_TRANSMUTE.fontRenderer = unicodeFontRender();
    }
}
