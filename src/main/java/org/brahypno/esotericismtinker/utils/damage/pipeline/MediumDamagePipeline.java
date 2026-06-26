package org.brahypno.esotericismtinker.utils.damage.pipeline;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.ETHelper;
import org.brahypno.esotericismtinker.utils.MethodHandler;
import org.brahypno.esotericismtinker.utils.damage.*;
import org.brahypno.esotericismtinker.utils.damage.step.StepResult;
import org.jetbrains.annotations.Nullable;

public final class MediumDamagePipeline {
    private MediumDamagePipeline() {}

    public static boolean damageHandler(@Nullable Entity entity, DamageSource source, float amount) {
        if (entity == null)
            return false;
        LivingEntity victim = ETHelper.getLivingTarget(entity);
        if (victim != null && !victim.level().isClientSide)
            return MethodHandler.invokeLivingHurt(victim, source, amount);
        return entity.hurt(source, amount);
    }

    public static DamageProbeResult apply(@Nullable Entity entity, DamageSource source, float amount, DamageOptions options) {
        DamageContext context = DamageContext.create(entity, source, amount, options, ForceLevel.MEDIUM);
        DamageProbeResult result = new DamageProbeResult(context);

        if (context.entity() == null || context.source() == null || context.amount() <= 0.0F)
            return result.fail("invalid args");
        if (context.victim() == null)
            return result.fail("no living target");
        if (context.victim().level().isClientSide)
            return result.fail("client side");
        if (!context.victim().isAlive())
            return result.serverDead("victim already not alive, health=" + context.victim().getHealth());

        result.addHeader("medium begin");

        StepResult basic = PipelineSteps.basicHandler(context, result);
        if (result.reachedExpectedDamage())
            return result.success("basic_handler");
        if (options.scanLevel() == ScanLevel.BASIC_HIT){
            return basic.progress() ? result.success("basic_handler") : result.fail("basic_handler failed");
        }
        StepResult raw = PipelineSteps.rawSetHealth(context, result);
        if (result.reachedExpectedDamage())
            return result.success("raw_set_health");
        if (options.scanLevel() == ScanLevel.SET_HEALTH_EQUIVALENT){
            StepResult field = PipelineSteps.privateFieldUnsafe(context, result, false);
            if (result.reachedExpectedDamage())
                return result.success("private_field_unsafe");

            if (isAuthoritativePartial(basic) || isAuthoritativePartial(raw) || isAuthoritativePartial(field)){
                return result.success("set_health_equivalent_partial");
            }
            return result.fail("set_health_equivalent failed");
        }

        StepResult nbt = PipelineSteps.nbtMutation(context, result, "medium_nbt_mutation");
        if (result.reachedExpectedDamage() || nbt.progress())
            return result.success("medium_nbt_mutation");

        StepResult data = PipelineSteps.entityDataUnsafe(context, result, false);
        if (result.reachedExpectedDamage())
            return result.success("entity_data_unsafe");

        StepResult field = PipelineSteps.privateFieldUnsafe(context, result, false);
        if (result.reachedExpectedDamage())
            return result.success("private_field_unsafe");

        if (isAuthoritativePartial(basic) || isAuthoritativePartial(raw) || isAuthoritativePartial(nbt) || isAuthoritativePartial(data) ||
            isAuthoritativePartial(field))
            return result.success("medium_partial_effect");

        return result.fail("all medium strategies failed");
    }

    private static boolean isAuthoritativePartial(StepResult step) {
        return step.progress() && step.dealt() > DamageConstants.DAMAGE_EPS;
    }
}
