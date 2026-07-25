package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects.ConsequenceState;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.ProtectionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Single native-hook bridge for all armor-side Stigmata consequences.
 */
public final class StigmataArmorConsequenceModule implements ModifierModule, OnAttackedModifierHook,
        ProtectionModifierHook, ToolDamageModifierHook, InventoryTickModifierHook {
    public static final StigmataArmorConsequenceModule INSTANCE =
            new StigmataArmorConsequenceModule();
    public static final RecordLoadable<StigmataArmorConsequenceModule> LOADER =
            new SingletonLoader<>(INSTANCE);

    private static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_KEY =
            TinkerDataCapability.TinkerDataKey.of(EsotericismTinker.getLocation("stigmata_armor_consequence"));

    private static final List<ModuleHook<?>> DEFAULT_HOOKS =
            HookProvider.<StigmataArmorConsequenceModule>defaultHooks(
                    ModifierHooks.ON_ATTACKED, ModifierHooks.PROTECTION,
                    ModifierHooks.TOOL_DAMAGE, ModifierHooks.INVENTORY_TICK);

    private StigmataArmorConsequenceModule() {}

    @Nullable
    private static ConsequenceState getActiveState(IToolStackView tool) {
        StigmataData data = StigmataData.read(tool);
        if (!data.hasConsequenceSeed() || 0 >= data.stage()){
            return null;
        }
        ConsequenceState state = ConsequenceState.of(tool, data);
        return 0 < state.overload() ? state : null;
    }

    @Override
    public void onInventoryTick(
            IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder,
            int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide || !isCorrectSlot || !stack.is(TinkerTags.Items.WORN_ARMOR)){
            return;
        }
        ConsequenceState state = getActiveState(tool);
        if (null == state){
            return;
        }
        StigmataConsequenceEffects.get(state.data().consequence())
                                  .onInventoryTick(
                                          state, modifier, world, holder, itemSlot,
                                          isSelected, true, stack);
    }

    @Override
    public int onDamageTool(
            IToolStackView tool, ModifierEntry modifier, int amount,
            @Nullable LivingEntity holder) {
        return amount;
    }

    @Override
    public int onDamageTool(
            IToolStackView tool, ModifierEntry modifier, int amount,
            @Nullable LivingEntity holder, @Nullable ItemStack stack) {
        if (null == stack || !stack.is(TinkerTags.Items.WORN_ARMOR)){
            return amount;
        }
        ConsequenceState state = getActiveState(tool);
        return null == state
               ? amount
               : StigmataConsequenceEffects.get(state.data().consequence())
                                           .onToolDamage(state, modifier, amount, holder);
    }

    @Override
    public float getProtectionModifier(
            IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
            EquipmentSlot slotType, DamageSource source, float modifierValue) {
        ConsequenceState state = getActiveState(tool);
        return null == state
               ? modifierValue
               : StigmataConsequenceEffects.get(state.data().consequence())
                                           .getProtectionModifier(
                                                   state, modifier, context, slotType,
                                                   source, modifierValue);
    }

    @Override
    public void addModules(ModuleHookMap.Builder builder) {
        builder.addModule(new SlotInChargeModule(SLOT_KEY));
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity wearer = context.getEntity();
        if (StigmataConsequenceEffects.isApplyingConsequenceDamage() || wearer.level().isClientSide ||
            !SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_KEY, slotType)){
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            IToolStackView equippedTool = context.getToolInSlot(slot);
            if (null == equippedTool || equippedTool.isBroken()){
                continue;
            }
            ConsequenceState state = getActiveState(equippedTool);
            if (null == state){
                continue;
            }
            ModifierEntry equippedModifier = equippedTool.getModifier(modifier.getId());
            StigmataConsequenceEffects.get(state.data().consequence()).onArmorAttacked(state, equippedModifier, context, slot, source, amount, isDirectDamage);
        }
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<StigmataArmorConsequenceModule> getLoader() {
        return LOADER;
    }
}
