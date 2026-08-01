package org.brahypno.esotericismtinker.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.esotericismtinker.library.modifiers.hook.LeftClickHook;
import org.brahypno.esotericismtinker.utils.CompatUtils.CuriosCompat;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import java.util.function.Supplier;

public class LeftClickEmptyPacket {
    private final InteractionHand hand;

    public LeftClickEmptyPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public LeftClickEmptyPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(InteractionHand.class));
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
    }

    public static void handle(LeftClickEmptyPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        ServerPlayer serverPlayer = context.getSender();
        if (serverPlayer != null){
            context.enqueueWork(() -> {
                ItemStack stack = serverPlayer.getItemInHand(packet.hand);
                if (stack.isEmpty()){
                    stack = CuriosCompat.findPreferredModifiable(serverPlayer);
                }
                if (stack.getItem() instanceof IModifiable){
                    EquipmentSlot slot = packet.hand == InteractionHand.MAIN_HAND
                                         ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    LeftClickHook.handleLeftClick(stack, serverPlayer, slot);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
