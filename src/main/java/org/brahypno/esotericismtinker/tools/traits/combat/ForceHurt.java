package org.brahypno.esotericismtinker.tools.traits.combat;

import org.brahypno.changelib.DamageHelper.DamageOptions;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.changelib.DamageHelper.DamageProbeResult;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MonsterMeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class ForceHurt extends Modifier implements MeleeHitModifierHook, MonsterMeleeHitModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.MONSTER_MELEE_HIT);
    }

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        forceDamage(context, "before hit");
        return knockback;
    }

    @Override
    public void failedMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageAttempted) {
        forceDamage(context, "fail hit");
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage) {
        forceDamage(context, "after hit");
    }

    @Override
    public void onMonsterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage) {
        failedMeleeHit(tool, modifier, context, damage);
    }

    private static void forceDamage(ToolAttackContext context, String phase) {
        boolean debug = EsotericismTinker.LOGGER.isDebugEnabled();
        if (debug){
            EsotericismTinker.LOGGER.debug(phase);
        }
        DamageProbeResult result = DamageProbe.finalDamageMethod(
                context.getTarget(),
                context.makeDamageSource(),
                4000,
                DamageOptions.finalNoRemove().withDebug(debug));
        if (debug){
            EsotericismTinker.LOGGER.debug(result.debugText());
        }
    }
}
