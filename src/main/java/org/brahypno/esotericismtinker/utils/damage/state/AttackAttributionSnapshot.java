package org.brahypno.esotericismtinker.utils.damage.state;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record AttackAttributionSnapshot(
        @Nullable LivingEntity lastHurtByMob
) {
    public static AttackAttributionSnapshot capture(@Nullable LivingEntity victim) {
        return new AttackAttributionSnapshot(
                victim == null ? null : victim.getLastHurtByMob()
        );
    }

    public void restore(LivingEntity victim, @Nullable DamageSource source) {
        Entity attacker = source == null ? null : source.getEntity();

        if (attacker instanceof Player player){
            /*
             * Vanilla API simultaneously restores:
             *
             * lastHurtByPlayer = player
             * lastHurtByPlayerTime = 100
             *
             * Therefore protected fields do not need to be accessed.
             */
            victim.setLastHurtByPlayer(player);
            victim.setLastHurtByMob(player);
            return;
        }

        if (attacker instanceof LivingEntity livingAttacker){
            victim.setLastHurtByMob(livingAttacker);
            return;
        }

        if (lastHurtByMob != null){
            victim.setLastHurtByMob(lastHurtByMob);
        }
    }
}