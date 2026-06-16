package org.brahypno.esotericismtinker.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.brahypno.esotericismtinker.EsotericismTinker;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID)
public class EsotericismTinkerNetwork {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(new ResourceLocation(EsotericismTinker.MODID, "msg"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals,
                                             PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    public static void registerPackets() {
        CHANNEL.registerMessage(packetId++, LeftClickEmptyPacket.class, LeftClickEmptyPacket::toBytes, LeftClickEmptyPacket::new, LeftClickEmptyPacket::handle);
        CHANNEL.registerMessage(packetId++, RightClickEmptyPacket.class, RightClickEmptyPacket::toBytes, RightClickEmptyPacket::new,
                                RightClickEmptyPacket::handle);
    }
}
