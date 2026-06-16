package org.brahypno.esotericismtinker.library.event;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.modifiers.hook.RightClickHook;
import org.brahypno.esotericismtinker.utils.CompactUtils.CuriosCompact;
import slimeknights.tconstruct.library.tools.item.IModifiable;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID)
public class PlayerRightClickEvent {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightEmptyClick(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        if (player != null && player.level().isClientSide){
            ItemStack stack = player.getItemInHand(player.getUsedItemHand());
            if (stack.isEmpty()){
                stack = CuriosCompact.findPreferredModifiable(player);
                if (stack.getItem() instanceof IModifiable)
                    RightClickHook.handleRightClick(stack, player, EquipmentSlot.MAINHAND);
            }
        }
    }
}
