package org.brahypno.esotericismtinker.library.modifiers.hook;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.brahypno.esotericismtinker.library.modifiers.EsotericismTinkerHook;
import org.brahypno.esotericismtinker.network.EsotericismTinkerNetwork;
import org.brahypno.esotericismtinker.network.RightClickEmptyPacket;
import org.brahypno.esotericismtinker.utils.CompatUtils.CuriosCompat;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Collection;

public interface RightClickHook {
    default void onRightClickEmpty(IToolStackView tool, ModifierEntry entry, Player player, Level level, EquipmentSlot equipmentSlot) {}

    static void handleRightClick(ItemStack stack, Player player, EquipmentSlot slot) {
        Level level = player.level();
        if (stack.isEmpty() && !level.isClientSide)
            stack = CuriosCompat.findPreferredModifiable(player);
        IToolStackView tool = ToolStack.from(stack);
        for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(EsotericismTinkerHook.RIGHT_CLICK).onRightClickEmpty(tool, entry, player, level, slot);
        }
        if (level.isClientSide){
            EsotericismTinkerNetwork.CHANNEL.sendToServer(new RightClickEmptyPacket());
        }
    }

    record AllMerger(Collection<RightClickHook> modules) implements RightClickHook {
        @Override
        public void onRightClickEmpty(IToolStackView tool, ModifierEntry entry, Player player, Level level, EquipmentSlot equipmentSlot) {
            for (RightClickHook module : this.modules) {
                module.onRightClickEmpty(tool, entry, player, level, equipmentSlot);
            }
        }
    }
}
