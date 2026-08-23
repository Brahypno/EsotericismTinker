package org.brahypno.esotericismtinker.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.esotericismtinker.transcendence.table.menu.TranscendenceAnvilMenu;

import java.util.function.Supplier;

public final class TranscendenceSublimationPacket {
    private final int containerId;
    private final ResourceLocation pathId;
    private final int delta;

    public TranscendenceSublimationPacket(int containerId, ResourceLocation pathId, int delta) {
        this.containerId = containerId;
        this.pathId = pathId;
        this.delta = delta;
    }

    public TranscendenceSublimationPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readResourceLocation(), buffer.readByte());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeResourceLocation(pathId);
        buffer.writeByte(delta);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || (delta != 1 && delta != -1)) return;
            if (!(player.containerMenu instanceof TranscendenceAnvilMenu menu)
                    || menu.containerId != containerId
                    || menu.getStation() == null
                    || !menu.stillValid(player)) return;

            if (menu.getStation().adjustSublimation(pathId, delta)) {
                menu.broadcastChanges();
                menu.getStation().syncRecipe(player);
            }
        });
        context.setPacketHandled(true);
    }
}
