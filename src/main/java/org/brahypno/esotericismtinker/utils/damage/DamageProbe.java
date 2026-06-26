package org.brahypno.esotericismtinker.utils.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.ETHelper;
import org.brahypno.esotericismtinker.utils.damage.method.GuardStateSupport;
import org.brahypno.esotericismtinker.utils.damage.pipeline.FinalDamagePipeline;
import org.brahypno.esotericismtinker.utils.damage.pipeline.MediumDamagePipeline;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class DamageProbe {
    private DamageProbe() {}

    public static boolean damageHandler(@Nullable Entity entity, DamageSource source, float amount) {
        return MediumDamagePipeline.apply(entity, source, amount, DamageOptions.basicHitOnly()).success();
    }

    public static DamageProbeResult mediumDamageMethod(@Nullable Entity entity, DamageSource source, float amount) {
        return MediumDamagePipeline.apply(entity, source, amount, DamageOptions.medium());
    }

    public static DamageProbeResult mediumDamageMethod(@Nullable Entity entity, DamageSource source, float amount, DamageOptions options) {
        return MediumDamagePipeline.apply(entity, source, amount, options);
    }

    public static DamageProbeResult finalDamageMethod(@Nullable Entity entity, DamageSource source, float amount) {
        return FinalDamagePipeline.apply(entity, source, amount, DamageOptions.finalNoRemove());
    }

    public static DamageProbeResult finalDamageMethod(@Nullable Entity entity, DamageSource source, float amount, DamageOptions options) {
        return FinalDamagePipeline.apply(entity, source, amount, options);
    }

    public static boolean mediumDamageBoolean(@Nullable Entity entity, DamageSource source, float amount) {
        return mediumDamageMethod(entity, source, amount).success();
    }

    public static boolean finalDamageBoolean(@Nullable Entity entity, DamageSource source, float amount) {
        return finalDamageMethod(entity, source, amount).success();
    }

    /**
     * Clears cached/discovered generic damage guards for outside callers.
     * This only opens likely cooldown/invulnerability/hurt gates; it does not guarantee real health damage.
     *
     * @return number of guard methods successfully invoked
     */
    public static int clearDamageGuards(Entity victim) {
        LivingEntity livingEntity = ETHelper.getLivingTarget(victim);
        return GuardStateSupport.clearDamageGuards(livingEntity);
    }

    /**
     * Clears cached/discovered generic damage guards with a diagnostic prefix and optional logger.
     * Pass a logger such as LOGGER::debug, or null for silent use.
     *
     * @return number of guard methods successfully invoked
     */
    public static int clearDamageGuards(@Nullable LivingEntity victim, String prefix, @Nullable Consumer<String> logger) {
        return GuardStateSupport.clearDamageGuards(victim, prefix, logger);
    }

    /**
     * Returns currently cached guard clearer count for this victim's entity type.
     */
    public static int cachedDamageGuardCount(Entity victim) {
        LivingEntity livingEntity = ETHelper.getLivingTarget(victim);
        return GuardStateSupport.cachedGuardCount(livingEntity);
    }
}
