package org.brahypno.esotericismtinker.utils.damage.state;

import net.minecraft.world.entity.LivingEntity;

public record DamageSnapshot(float health, float absorption, int deathTime, boolean alive, boolean removed) {
    public static DamageSnapshot of(LivingEntity entity) {
        return new DamageSnapshot(entity.getHealth(), entity.getAbsorptionAmount(), entity.deathTime, entity.isAlive(), entity.isRemoved());
    }

    public boolean authoritativeChangeFrom(DamageSnapshot before) {
        return health < before.health || absorption < before.absorption || deathTime > before.deathTime || alive != before.alive || removed != before.removed;
    }
}
