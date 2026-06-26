package org.brahypno.esotericismtinker.utils.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.ETHelper;
import org.jetbrains.annotations.Nullable;

public record DamageContext(@Nullable Entity entity, @Nullable LivingEntity victim, DamageSource source, float amount, DamageOptions options, ForceLevel forceLevel, float initialHealth) {
    public static DamageContext create(@Nullable Entity entity, DamageSource source, float amount, DamageOptions options, ForceLevel forceLevel) {
        LivingEntity victim = ETHelper.getLivingTarget(entity);
        float initialHealth = victim == null ? 0.0F : victim.getHealth();
        return new DamageContext(entity, victim, source, amount, options, forceLevel, initialHealth);
    }

    public boolean invalid() {
        return entity == null || source == null || amount <= 0.0F || victim == null;
    }

    public float remainingOrAmount(DamageProbeResult result) {
        float remaining = result.remainingAmount();
        return remaining > DamageConstants.DAMAGE_EPS ? remaining : amount;
    }
}
