package org.brahypno.esotericismtinker.library.event;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.modifiers.EsotericismTinkerHook;
import org.brahypno.esotericismtinker.utils.ETModifierCheck;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID)
public class LivingHealEvent {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHeal(net.minecraftforge.event.entity.living.LivingHealEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide){
            return;
        }

        LivingEntity entity = event.getEntity();
        EquipmentContext context = new EquipmentContext(entity);
        float amount = event.getAmount();
        for (EquipmentSlot slotType : ETModifierCheck.slots) {
            ItemStack stack = entity.getItemBySlot(slotType);
            if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable)){
                continue;
            }

            IToolStackView tool = ToolStack.from(stack);
            if (tool.isBroken()){
                continue;
            }

            for (ModifierEntry modifier : tool.getModifierList()) {
                amount = modifier.getHook(EsotericismTinkerHook.HEAL).onHeal(tool, modifier, context, slotType, amount);
                if (amount < 0){
                    event.setCanceled(true);
                    return;
                }
            }
        }

        event.setAmount(amount);
    }
}
