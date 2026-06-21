package org.brahypno.esotericismtinker.selenic.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;
import org.brahypno.esotericismtinker.selenic.client.renderer.ArmillaryCrownBlockEntityRenderer;
import org.brahypno.esotericismtinker.selenic.client.renderer.LunarFontBlockEntityRenderer;
import org.brahypno.esotericismtinker.selenic.client.renderer.TestimonyStandBlockEntityRenderer;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SelenicClient {
    private SelenicClient() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                          {
                              BlockEntityRenderers.register(
                                      EsotericismTinkerSelenic.lunarFontBE.get(),
                                      LunarFontBlockEntityRenderer::new);
                              BlockEntityRenderers.register(
                                      EsotericismTinkerSelenic.armillaryCrownBE.get(),
                                      ArmillaryCrownBlockEntityRenderer::new
                              );
                              BlockEntityRenderers.register(
                                      EsotericismTinkerSelenic.testimonyStandBE.get(),
                                      TestimonyStandBlockEntityRenderer::new
                              );
                          });
    }
}