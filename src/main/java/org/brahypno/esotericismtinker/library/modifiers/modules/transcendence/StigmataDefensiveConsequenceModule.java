package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects.ConsequenceState;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.ProtectionModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * Incoming-damage hooks for armor and other defensive equipment.
 */
public record StigmataDefensiveConsequenceModule(ModifierCondition<IToolContext> condition)
        implements ModifierModule, StigmataConsequenceModuleBridge,
        OnAttackedModifierHook, ModifyDamageModifierHook,
        ProtectionModifierHook {
    public static final RecordLoadable<StigmataDefensiveConsequenceModule> LOADER =
            RecordLoadable.create(
                    ModifierCondition.CONTEXT_FIELD,
                    StigmataDefensiveConsequenceModule::new);

    private static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge>
            SLOT_KEY = TinkerDataCapability.TinkerDataKey.of(
                    EsotericismTinker.getLocation(
                            "stigmata_defensive_consequence"));

    private static final List<ModuleHook<?>> DEFAULT_HOOKS =
            HookProvider.<StigmataDefensiveConsequenceModule>defaultHooks(
                    ModifierHooks.ON_ATTACKED, ModifierHooks.MODIFY_HURT,
                    ModifierHooks.PROTECTION);

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public float getProtectionModifier(
            IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
            EquipmentSlot slotType, DamageSource source, float modifierValue) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        return null == state
               ? modifierValue
               : StigmataConsequenceEffects.get(state.consequence())
                                           .getProtectionModifier(
                                                   state, modifier, context,
                                                   slotType, source,
                                                   modifierValue);
    }

    @Override
    public float modifyDamageTaken(
            IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
            EquipmentSlot slotType, DamageSource source, float amount,
            boolean isDirectDamage) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        return null == state
               ? amount
               : StigmataConsequenceEffects.get(state.consequence())
                                           .modifyHurt(
                                                   state, modifier, context,
                                                   slotType, source, amount,
                                                   isDirectDamage);
    }

    @Override
    public void addModules(ModuleHookMap.Builder builder) {
        builder.addModule(new SlotInChargeModule(SLOT_KEY));
    }

    @Override
    public void onAttacked(
            IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
            EquipmentSlot slotType, DamageSource source, float amount,
            boolean isDirectDamage) {
        LivingEntity wearer = context.getEntity();
        if (!condition.matches(tool, modifier)
            || StigmataConsequenceEffects.isApplyingConsequenceDamage()
            || wearer.level().isClientSide
            || !SlotInChargeModule.isInCharge(
                    context.getTinkerData(), SLOT_KEY, slotType)){
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            IToolStackView equippedTool = context.getToolInSlot(slot);
            if (null == equippedTool
                || equippedTool.isBroken()
                || !equippedTool.hasTag(TinkerTags.Items.ARMOR)){
                continue;
            }
            ConsequenceState state =
                    StigmataConsequenceModuleBridge.getActiveState(equippedTool);
            if (null == state){
                continue;
            }
            ModifierEntry equippedModifier =
                    equippedTool.getModifier(modifier.getId());
            StigmataConsequenceEffects.get(state.consequence())
                                      .onArmorAttacked(
                                              state, equippedModifier, context,
                                              slot, source, amount,
                                              isDirectDamage);
        }
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<StigmataDefensiveConsequenceModule> getLoader() {
        return LOADER;
    }

    public static final class Builder extends ModuleBuilder.Context<Builder> {
        private Builder() {}

        public StigmataDefensiveConsequenceModule build() {
            return new StigmataDefensiveConsequenceModule(condition);
        }
    }
}
