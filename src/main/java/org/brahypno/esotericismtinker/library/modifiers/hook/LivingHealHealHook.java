package org.brahypno.esotericismtinker.library.modifiers.hook;

import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

public interface LivingHealHealHook {
    default float onHeal(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, float amount) {
        return amount;
    }

    record AllMerger(Collection<LivingHealHealHook> modules) implements LivingHealHealHook {
        @Override
        public float onHeal(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, float amount) {
            for (LivingHealHealHook module : this.modules) {
                amount = module.onHeal(tool, modifier, context, slotType, amount);
            }
            return amount;
        }
    }
}
