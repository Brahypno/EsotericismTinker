package org.brahypno.esotericismtinker.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.brahypno.esotericismtinker.EsotericismTinker;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID)
public final class EsotericismTinkerNetwork {
    public static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EsotericismTinker.MODID, "msg"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private EsotericismTinkerNetwork() {}

    public static void registerPackets() {
        CHANNEL.registerMessage(
                packetId++,
                LeftClickEmptyPacket.class,
                LeftClickEmptyPacket::toBytes,
                LeftClickEmptyPacket::new,
                LeftClickEmptyPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                RightClickEmptyPacket.class,
                RightClickEmptyPacket::toBytes,
                RightClickEmptyPacket::new,
                RightClickEmptyPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                TranscendenceReceptionPacket.class,
                TranscendenceReceptionPacket::toBytes,
                TranscendenceReceptionPacket::new,
                TranscendenceReceptionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                packetId++,
                TranscendenceInvestiturePacket.class,
                TranscendenceInvestiturePacket::toBytes,
                TranscendenceInvestiturePacket::new,
                TranscendenceInvestiturePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                packetId++,
                UpdateTranscendenceAnvilRecipePacket.class,
                UpdateTranscendenceAnvilRecipePacket::toBytes,
                UpdateTranscendenceAnvilRecipePacket::new,
                UpdateTranscendenceAnvilRecipePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }
}
