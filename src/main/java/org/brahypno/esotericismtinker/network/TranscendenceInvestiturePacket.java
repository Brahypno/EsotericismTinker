package org.brahypno.esotericismtinker.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.esotericismtinker.transcendence.table.menu.TranscendenceAnvilMenu;

import java.util.function.Supplier;

/** Client-to-server selection of one definition trait from the offhand tool. */
public final class TranscendenceInvestiturePacket {
    private final int containerId;
    private final int traitIndex;

    public TranscendenceInvestiturePacket(int containerId, int traitIndex) {
        this.containerId = containerId;
        this.traitIndex = traitIndex;
    }

    public TranscendenceInvestiturePacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeVarInt(traitIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || traitIndex < 0) {
                return;
            }
            if (!(player.containerMenu instanceof TranscendenceAnvilMenu menu)
                    || menu.containerId != containerId
                    || menu.getStation() == null
                    || !menu.stillValid(player)) {
                return;
            }
            if (menu.getStation().selectInvestiture(player, traitIndex)) {
                menu.broadcastChanges();
                menu.getStation().syncRecipe(player);
            }
        });
        context.setPacketHandled(true);
    }
}
