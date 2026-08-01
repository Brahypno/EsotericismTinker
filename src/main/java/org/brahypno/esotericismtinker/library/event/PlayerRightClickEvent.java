package org.brahypno.esotericismtinker.library.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.modifiers.hook.RightClickHook;
import org.brahypno.esotericismtinker.utils.CompatUtils.CuriosCompat;
import slimeknights.tconstruct.library.tools.item.IModifiable;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID)
public class PlayerRightClickEvent {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightEmptyClick(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        if (player != null && player.level().isClientSide){
            InteractionHand hand = event.getHand();
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()){
                stack = CuriosCompat.findPreferredModifiable(player);
                if (stack.getItem() instanceof IModifiable)
                    RightClickHook.handleRightClick(stack, player, getSlot(hand));
            }
        }
    }

    private static EquipmentSlot getSlot(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }
}
