package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.modifiers.EsotericismTinkerHook;
import org.brahypno.esotericismtinker.library.modifiers.hook.LeftClickHook;
import org.brahypno.esotericismtinker.library.modifiers.hook.ProjectileHurtHook;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects.ConsequenceState;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataConsequence;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockBreakModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.LauncherHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Active output hooks for non-armor tools.
 *
 * <p>Melee, launcher, and harvesting consequences live here so defensive equipment cannot trigger
 * them merely because it carries the same modifier.</p>
 */
public record StigmataOffensiveConsequenceModule(ModifierCondition<IToolContext> condition)
        implements ModifierModule, StigmataConsequenceModuleBridge,
        MeleeHitModifierHook, LauncherHitModifierHook, MeleeDamageModifierHook,
        BreakSpeedModifierHook, BlockBreakModifierHook,
        ProjectileLaunchModifierHook, ProjectileHurtHook, LeftClickHook {
    public static final RecordLoadable<StigmataOffensiveConsequenceModule> LOADER =
            RecordLoadable.create(
                    ModifierCondition.CONTEXT_FIELD,
                    StigmataOffensiveConsequenceModule::new);

    private static final List<ModuleHook<?>> DEFAULT_HOOKS =
            HookProvider.<StigmataOffensiveConsequenceModule>defaultHooks(
                    ModifierHooks.MELEE_HIT, ModifierHooks.LAUNCHER_HIT,
                    ModifierHooks.MELEE_DAMAGE, ModifierHooks.BREAK_SPEED,
                    ModifierHooks.BLOCK_BREAK, ModifierHooks.PROJECTILE_LAUNCH,
                    EsotericismTinkerHook.PROJECTILE_HURT,
                    EsotericismTinkerHook.LEFT_CLICK);

    private static final ResourceLocation PROJECTILE_CONSEQUENCE_SEED =
            EsotericismTinker.getLocation(
                    "stigmata_projectile_consequence_seed");
    private static final ResourceLocation PROJECTILE_STAGE =
            EsotericismTinker.getLocation("stigmata_projectile_stage");
    private static final ResourceLocation PROJECTILE_OVERLOAD =
            EsotericismTinker.getLocation("stigmata_projectile_overload");

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    private static ConsequenceState getProjectileState(ModDataNBT persistentData) {
        if (!persistentData.contains(PROJECTILE_CONSEQUENCE_SEED)
            || !persistentData.contains(PROJECTILE_STAGE)
            || !persistentData.contains(PROJECTILE_OVERLOAD)){
            return null;
        }
        int seed = persistentData.getInt(PROJECTILE_CONSEQUENCE_SEED);
        int stage = persistentData.getInt(PROJECTILE_STAGE);
        int overload = persistentData.getInt(PROJECTILE_OVERLOAD);
        if (0 >= stage || 0 >= overload){
            return null;
        }
        return new ConsequenceState(
                StigmataConsequence.fromSeed(seed), seed, stage, overload);
    }

    @Override
    public float getMeleeDamage(
            IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
            float baseDamage, float damage) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        return null == state
               ? damage
               : StigmataConsequenceEffects.get(state.consequence())
                                           .getMeleeDamage(
                                                   state, modifier, context,
                                                   baseDamage, damage);
    }

    @Override
    public float beforeMeleeHit(
            IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
            float damage, float baseKnockback, float knockback) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        return null == state
               ? knockback
               : StigmataConsequenceEffects.get(state.consequence())
                                           .beforeMeleeHit(
                                                   state, modifier, context, damage,
                                                   baseKnockback, knockback);
    }

    @Override
    public void afterMeleeHit(
            IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
            float damageDealt) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state && !context.getLevel().isClientSide){
            StigmataConsequenceEffects.get(state.consequence())
                                      .afterMeleeHit(
                                              state, modifier, context, damageDealt);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBreakSpeed(
            IToolStackView tool, ModifierEntry modifier, PlayerEvent.BreakSpeed event,
            @Nullable Direction sideHit, boolean isEffective,
            float miningSpeedModifier) {}

    @Override
    public float modifyBreakSpeed(
            IToolStackView tool, ModifierEntry modifier, BreakSpeedContext context,
            float speed) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        return null == state
               ? speed
               : StigmataConsequenceEffects.get(state.consequence())
                                           .modifyBreakSpeed(
                                                   state, modifier, context, speed);
    }

    @Override
    public void afterBlockBreak(
            IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state){
            StigmataConsequenceEffects.get(state.consequence())
                                      .afterBlockBreak(state, modifier, context);
        }
    }

    @Override
    public void onLauncherHitEntity(
            IToolStackView tool, ModifierEntry modifier, Projectile projectile,
            LivingEntity attacker, Entity target,
            @Nullable LivingEntity livingTarget, float damageDealt) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state && !attacker.level().isClientSide){
            StigmataConsequenceEffects.get(state.consequence())
                                      .onLauncherHitEntity(
                                              state, modifier, projectile, attacker,
                                              target, livingTarget, damageDealt);
        }
    }

    @Override
    public void onLauncherHitBlock(
            IToolStackView tool, ModifierEntry modifier, Projectile projectile,
            LivingEntity owner, BlockPos target) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state && !owner.level().isClientSide){
            StigmataConsequenceEffects.get(state.consequence())
                                      .onLauncherHitBlock(
                                              state, modifier, projectile, owner,
                                              target);
        }
    }

    @Override
    public void onProjectileLaunch(
            IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
            Projectile projectile, @Nullable AbstractArrow arrow,
            ModDataNBT persistentData, boolean primary) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null == state || shooter.level().isClientSide){
            return;
        }
        persistentData.putInt(
                PROJECTILE_CONSEQUENCE_SEED, state.consequenceSeed());
        persistentData.putInt(PROJECTILE_STAGE, state.stage());
        persistentData.putInt(PROJECTILE_OVERLOAD, state.overload());
        StigmataConsequenceEffects.get(state.consequence())
                                  .onProjectileLaunch(
                                          state, modifier, shooter, projectile,
                                          persistentData, primary);
    }

    @Override
    public float modifyProjectileHurt(
            ModifierNBT modifiers, ModDataNBT persistentData,
            ModifierEntry modifier, Projectile projectile, DamageSource source,
            @Nullable LivingEntity attacker, LivingEntity target, float amount) {
        ConsequenceState state = getProjectileState(persistentData);
        return null == state
               ? amount
               : StigmataConsequenceEffects.get(state.consequence())
                                           .modifyProjectileHurt(
                                                   state, modifier, projectile,
                                                   source, attacker, target,
                                                   amount);
    }

    @Override
    public void onLeftClickEmpty(
            IToolStackView tool, ModifierEntry modifier, Player player,
            Level level, EquipmentSlot equipmentSlot) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state && !level.isClientSide){
            StigmataConsequenceEffects.get(state.consequence())
                                      .onLeftClickEmpty(
                                              state, modifier, player, level,
                                              equipmentSlot);
        }
    }

    @Override
    public void onLeftClickBlock(
            PlayerInteractEvent.LeftClickBlock event, IToolStackView tool,
            ModifierEntry modifier, Player player, Level level,
            EquipmentSlot equipmentSlot, BlockState blockState, BlockPos pos) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state && !level.isClientSide){
            StigmataConsequenceEffects.get(state.consequence())
                                      .onLeftClickBlock(
                                              state, modifier, event, player,
                                              level, equipmentSlot, blockState,
                                              pos);
        }
    }

    @Override
    public void onLeftClickEntity(
            AttackEntityEvent event, IToolStackView tool,
            ModifierEntry modifier, Player player, Level level,
            EquipmentSlot equipmentSlot, Entity target) {
        ConsequenceState state = getOwnActiveState(tool, modifier);
        if (null != state && !level.isClientSide){
            StigmataConsequenceEffects.get(state.consequence())
                                      .onLeftClickEntity(
                                              state, modifier, event, player,
                                              level, equipmentSlot, target);
        }
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<StigmataOffensiveConsequenceModule> getLoader() {
        return LOADER;
    }

    public static final class Builder extends ModuleBuilder.Context<Builder> {
        private Builder() {}

        public StigmataOffensiveConsequenceModule build() {
            return new StigmataOffensiveConsequenceModule(condition);
        }
    }
}
