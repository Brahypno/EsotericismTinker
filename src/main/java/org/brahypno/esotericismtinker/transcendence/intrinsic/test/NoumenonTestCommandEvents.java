package org.brahypno.esotericismtinker.transcendence.intrinsic.test;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonKeys;

@Mod.EventBusSubscriber(modid = NoumenonKeys.MOD_ID)
public final class NoumenonTestCommandEvents {
    private NoumenonTestCommandEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NoumenonTestCommands.register(event.getDispatcher());
    }
}
