package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects.ConsequenceState;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockBreakModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.LauncherHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Single native-hook bridge for all tool-side Stigmata consequences.
 */
public record StigmataToolConsequenceModule(ModifierCondition<IToolContext> condition) implements ModifierModule, MeleeHitModifierHook, LauncherHitModifierHook,
        MeleeDamageModifierHook, BreakSpeedModifierHook, BlockBreakModifierHook, ToolDamageModifierHook,
        InventoryTickModifierHook, TooltipModifierHook, ConditionalModule<IToolContext> {
    public static final RecordLoadable<StigmataToolConsequenceModule> LOADER = RecordLoadable.create(
            ModifierCondition.CONTEXT_FIELD, StigmataToolConsequenceModule::new);

    private static final List<ModuleHook<?>> DEFAULT_HOOKS =
            HookProvider.<StigmataToolConsequenceModule>defaultHooks(ModifierHooks.MELEE_HIT, ModifierHooks.LAUNCHER_HIT,
                                                                     ModifierHooks.MELEE_DAMAGE, ModifierHooks.BREAK_SPEED,
                                                                     ModifierHooks.BLOCK_BREAK, ModifierHooks.TOOL_DAMAGE,
                                                                     ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int onDamageTool(
            IToolStackView tool, ModifierEntry modifier, int amount,
            @Nullable LivingEntity holder) {
        ConsequenceState state = getActiveState(tool, modifier);
        return null == state
               ? amount
               : StigmataConsequenceEffects.get(state.data().consequence())
                                           .onToolDamage(state, modifier, amount, holder);
    }

    @Override
    public float getMeleeDamage(
            IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
            float baseDamage, float damage) {
        ConsequenceState state = getActiveState(tool, modifier);
        return null == state
               ? damage
               : StigmataConsequenceEffects.get(state.data().consequence())
                                           .getMeleeDamage(state, modifier, context, baseDamage, damage);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBreakSpeed(
            IToolStackView tool, ModifierEntry modifier, PlayerEvent.BreakSpeed event,
            @Nullable Direction sideHit, boolean isEffective, float miningSpeedModifier) {}

    @Override
    public float modifyBreakSpeed(
            IToolStackView tool, ModifierEntry modifier, BreakSpeedContext context, float speed) {
        ConsequenceState state = getActiveState(tool, modifier);
        return null == state
               ? speed
               : StigmataConsequenceEffects.get(state.data().consequence())
                                           .modifyBreakSpeed(state, modifier, context, speed);
    }

    @Override
    public void afterBlockBreak(
            IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        ConsequenceState state = getActiveState(tool, modifier);
        if (null != state){
            StigmataConsequenceEffects.get(state.data().consequence())
                                      .afterBlockBreak(state, modifier, context);
        }
    }

    @Nullable
    private ConsequenceState getActiveState(IToolStackView tool, ModifierEntry modifier) {
        if (!condition.matches(tool, modifier)){
            return null;
        }
        StigmataData data = StigmataData.read(tool);
        if (!data.hasConsequenceSeed() || 0 >= data.stage()){
            return null;
        }
        ConsequenceState state = ConsequenceState.of(tool, data);
        return 0 < state.overload() ? state : null;
    }

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        ConsequenceState state = getActiveState(tool, modifier);
        if (null == state){
            return knockback;
        }
        return StigmataConsequenceEffects.get(state.data().consequence()).beforeMeleeHit(state, modifier, context, damage, baseKnockback, knockback);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        ConsequenceState state = getActiveState(tool, modifier);
        if (null == state || context.getLevel().isClientSide){
            return;
        }
        StigmataConsequenceEffects.get(state.data().consequence()).afterMeleeHit(state, modifier, context, damageDealt);
    }

    @Override
    public void onLauncherHitEntity(
            IToolStackView tool, ModifierEntry modifier, Projectile projectile, LivingEntity attacker, Entity target,
            @Nullable LivingEntity livingTarget, float damageDealt) {
        ConsequenceState state = getActiveState(tool, modifier);
        if (null == state || attacker.level().isClientSide){
            return;
        }
        StigmataConsequenceEffects.get(state.data().consequence())
                                  .onLauncherHitEntity(state, modifier, projectile, attacker, target, livingTarget, damageDealt);
    }

    @Override
    public void onLauncherHitBlock(IToolStackView tool, ModifierEntry modifier, Projectile projectile, LivingEntity owner, BlockPos target) {
        ConsequenceState state = getActiveState(tool, modifier);
        if (null == state || owner.level().isClientSide){
            return;
        }
        StigmataConsequenceEffects.get(state.data().consequence()).onLauncherHitBlock(state, modifier, projectile, owner, target);
    }

    @Override
    public void onInventoryTick(
            IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected,
            boolean isCorrectSlot, ItemStack stack) {
        ConsequenceState state = getActiveState(tool, modifier);
        if (null == state || world.isClientSide || !isCorrectSlot){
            return;
        }
        StigmataConsequenceEffects.get(state.data().consequence()).onInventoryTick(state, modifier, world, holder, itemSlot, isSelected, true, stack);
    }

    @Override
    public void addTooltip(
            IToolStackView tool, ModifierEntry modifier, @Nullable Player player,
            List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (TooltipKey.SHIFT != tooltipKey){
            return;
        }
        ConsequenceState state = getActiveState(tool, modifier);
        if (null != state){
            StigmataConsequenceEffects.get(state.data().consequence())
                                      .addTooltip(state, false, tooltip);
        }
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<StigmataToolConsequenceModule> getLoader() {
        return LOADER;
    }

    public static final class Builder extends ModuleBuilder.Context<Builder> {
        private Builder() {}

        public StigmataToolConsequenceModule build() {
            return new StigmataToolConsequenceModule(condition);
        }
    }
}
