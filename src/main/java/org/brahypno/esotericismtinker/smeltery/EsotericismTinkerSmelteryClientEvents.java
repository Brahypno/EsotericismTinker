package org.brahypno.esotericismtinker.smeltery;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.EsotericismTinker;
import slimeknights.tconstruct.smeltery.client.render.HeatingStructureBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.TankBlockEntityRenderer;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EsotericismTinkerSmelteryClientEvents {

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EsotericismTinkerSmeltery.tank.get(), TankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(EsotericismTinkerSmeltery.Transmute.get(), HeatingStructureBlockEntityRenderer::new);
    }

}
