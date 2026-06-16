package org.brahypno.esotericismtinker.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.esotericismtinker.library.modifiers.hook.LeftClickHook;

import java.util.function.Supplier;

public class LeftClickEmptyPacket {
    public LeftClickEmptyPacket() {}

    public LeftClickEmptyPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public static void handle(LeftClickEmptyPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        ServerPlayer serverPlayer = context.getSender();
        if (serverPlayer != null){
            ItemStack stack = serverPlayer.getItemInHand(serverPlayer.getUsedItemHand());
            EquipmentSlot slot = InteractionHand.MAIN_HAND == serverPlayer.getUsedItemHand() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            context.enqueueWork(() -> LeftClickHook.handleLeftClick(stack, serverPlayer, slot));
        }
        context.setPacketHandled(true);
    }
}
