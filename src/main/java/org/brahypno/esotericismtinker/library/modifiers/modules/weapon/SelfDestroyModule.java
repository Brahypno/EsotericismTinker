package org.brahypno.esotericismtinker.library.modifiers.modules.weapon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

public record SelfDestroyModule(EntityType<?> entity) implements ModifierModule, InventoryTickModifierHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<SelfDestroyModule>defaultHooks(ModifierHooks.INVENTORY_TICK);

    public static final RecordLoadable<SelfDestroyModule> LOADER = RecordLoadable.create(
            Loadables.ENTITY_TYPE.requiredField("entity", SelfDestroyModule::entity),
            SelfDestroyModule::new
    );

    @Override
    public RecordLoadable<SelfDestroyModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide || holder.getType() == entity || holder instanceof Player player && player.getAbilities().instabuild){
            return;
        }

        stack.setCount(0);
    }
}