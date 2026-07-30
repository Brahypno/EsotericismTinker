package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects.ConsequenceState;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Hooks shared by offensive tools and defensive equipment.
 *
 * <p>This bridge owns effects intrinsic to the marked item itself: attributes, inventory ticks,
 * durability consequences, and consequence tooltips.</p>
 */
public record StigmataCommonConsequenceModule(ModifierCondition<IToolContext> condition)
        implements ModifierModule, StigmataConsequenceModuleBridge,
        AttributesModifierHook, ToolDamageModifierHook, InventoryTickModifierHook,
        TooltipModifierHook {
    public static final RecordLoadable<StigmataCommonConsequenceModule> LOADER =
            RecordLoadable.create(
                    ModifierCondition.CONTEXT_FIELD,
                    StigmataCommonConsequenceModule::new);

    private static final List<ModuleHook<?>> DEFAULT_HOOKS =
            HookProvider.<StigmataCommonConsequenceModule>defaultHooks(
                    ModifierHooks.ATTRIBUTES, ModifierHooks.TOOL_DAMAGE,
                    ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void addAttributes(
            IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot,
            BiConsumer<Attribute, AttributeModifier> consumer) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state){
            StigmataConsequenceEffects.get(state.consequence())
                                      .addAttributes(state, modifier, slot, consumer);
        }
    }

    @Override
    public int onDamageTool(
            IToolStackView tool, ModifierEntry modifier, int amount,
            @Nullable LivingEntity holder) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        return null == state
               ? amount
               : StigmataConsequenceEffects.get(state.consequence())
                                           .onToolDamage(state, modifier, amount, holder);
    }

    @Override
    public void onInventoryTick(
            IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder,
            int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide || !isCorrectSlot){
            return;
        }
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state){
            StigmataConsequenceEffects.get(state.consequence())
                                      .onInventoryTick(
                                              state, modifier, world, holder, itemSlot,
                                              isSelected, true, stack);
        }
    }

    @Override
    public void addTooltip(
            IToolStackView tool, ModifierEntry modifier, @Nullable Player player,
            List<Component> tooltip, TooltipKey tooltipKey,
            TooltipFlag tooltipFlag) {
        if (TooltipKey.SHIFT != tooltipKey){
            return;
        }
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state){
            StigmataConsequenceEffects.get(state.consequence())
                                      .addTooltip(
                                              state,
                                              tool.hasTag(TinkerTags.Items.ARMOR),
                                              tooltip);
        }
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<StigmataCommonConsequenceModule> getLoader() {
        return LOADER;
    }

    public static final class Builder extends ModuleBuilder.Context<Builder> {
        private Builder() {}

        public StigmataCommonConsequenceModule build() {
            return new StigmataCommonConsequenceModule(condition);
        }
    }
}
