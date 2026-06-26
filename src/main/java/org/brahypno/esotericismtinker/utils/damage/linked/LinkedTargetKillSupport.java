package org.brahypno.esotericismtinker.utils.damage.linked;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class LinkedTargetKillSupport {
    private LinkedTargetKillSupport() {}

    public static boolean tryKillLinkedTargets(Entity root, LivingEntity victim, DamageSource source, List<LivingEntity> targets, Predicate<LivingEntity> killOne, Predicate<LivingEntity> victimKillConfirmed, Consumer<String> log) {
        boolean touched = false;

        for (LivingEntity target : targets) {
            if (!LinkedTargetPolicy.shouldKillCandidate(root, victim, target, source, log)) continue;

            float beforeHealth = target.getHealth();
            boolean beforeAlive = target.isAlive();
            boolean beforeRemoved = target.isRemoved();
            boolean targetKilled = killOne.test(target);
            touched = true;

            log.accept("kill_linked_target: " + target.getClass().getSimpleName() + ", targetKilled=" + targetKilled + ", health " + beforeHealth + " -> " + target.getHealth() + ", alive " + beforeAlive + " -> " + target.isAlive() + ", removed " + beforeRemoved + " -> " + target.isRemoved());

            if (victimKillConfirmed.test(victim)) {
                log.accept("kill_linked_target confirmed victim death after linked target operation");
                return true;
            }
        }

        if (touched) log.accept("kill_linked_target touched targets but victim was not confirmed dead");
        return false;
    }
}
