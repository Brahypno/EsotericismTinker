package org.brahypno.esotericismtinker.transcendence.table.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.transcendence.table.EsotericismTinkerTranscendenceTable;

@Mod.EventBusSubscriber(
        modid = EsotericismTinker.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class TranscendenceAnvilClientEvents {
    private TranscendenceAnvilClientEvents() {}

    @SubscribeEvent
    public static void registerBlockEntityRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                EsotericismTinkerTranscendenceTable.transcendenceAnvilBE.get(),
                TranscendenceAnvilBlockEntityRenderer::new
        );
    }
}
