package org.brahypno.esotericismtinker.utils.damage.linked;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.function.Consumer;

public final class LinkedTargetPolicy {
    private LinkedTargetPolicy() {}

    public static boolean canScanDeclaringClass(Class<?> cls, Class<?> victimClass) {
        if (cls == null || cls == Object.class) return false;
        if (cls == Entity.class || cls == LivingEntity.class || cls == Mob.class || cls == PathfinderMob.class) return false;
        if (cls.getName().startsWith("net.minecraft.")) return false;
        return cls.isAssignableFrom(victimClass) || victimClass.isAssignableFrom(cls);
    }

    public static boolean canScanContainerClass(Class<?> cls) {
        if (cls == null || cls == Object.class) return false;
        if (cls == Entity.class || cls == LivingEntity.class || cls == Mob.class || cls == PathfinderMob.class) return false;
        if (cls.getName().startsWith("net.minecraft.")) return false;
        if (cls.isPrimitive() || cls.isEnum() || cls.isArray()) return false;
        String name = cls.getName().toUpperCase(Locale.ROOT);
        return containsAny(name, "BOSS", "INFO", "HOLDER", "CONTROLLER", "PART", "PROXY", "CORE", "BODY", "GOETY");
    }

    public static boolean canReadField(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) return false;
        if (field.getType().isPrimitive() || field.getType().isEnum()) return false;
        if (field.getDeclaringClass().getName().startsWith("java.")) return false;
        return true;
    }

    public static boolean isLinkedTargetField(Field field) {
        String name = field.getName().toUpperCase(Locale.ROOT);
        if (isCombatReferenceFieldName(name)) return false;
        if (containsAny(name, "BOSS", "HOST", "OWNER", "PARENT", "MASTER", "CONTROLLER", "CORE", "BODY", "MAIN", "SOURCE", "ORIGIN", "TRUE", "PROXY", "PART")) return true;
        String owner = field.getDeclaringClass().getSimpleName().toUpperCase(Locale.ROOT);
        return containsAny(owner, "BOSSINFO", "BOSS_INFO", "CONTROLLER", "HOLDER") && containsAny(name, "BOSS", "ENTITY", "MOB");
    }

    public static boolean isContainerField(Field field) {
        String name = field.getName().toUpperCase(Locale.ROOT);
        if (isCombatReferenceFieldName(name)) return false;
        if (containsAny(name, "BOSS", "INFO", "HOLDER", "CONTROLLER", "CORE", "BODY", "PROXY", "PART")) return true;
        return canScanContainerClass(field.getType());
    }

    public static boolean isForbiddenCandidate(Entity root, LivingEntity victim, LivingEntity candidate, DamageSource source, Consumer<String> log) {
        if (candidate == null) return true;
        if (candidate == victim) {
            log.accept("linked_target_reject: candidate is victim/self " + describe(candidate));
            return true;
        }
        if (candidate == root) {
            log.accept("linked_target_reject: candidate is root " + describe(candidate));
            return true;
        }
        if (candidate instanceof Player) {
            log.accept("linked_target_reject: player candidate " + describe(candidate));
            return true;
        }
        Entity sourceEntity = source == null ? null : source.getEntity();
        if (candidate == sourceEntity) {
            log.accept("linked_target_reject: source entity candidate " + describe(candidate));
            return true;
        }
        if (sourceEntity instanceof Player && candidate.getUUID().equals(sourceEntity.getUUID())) {
            log.accept("linked_target_reject: source player uuid candidate " + describe(candidate));
            return true;
        }
        if (candidate.level() != victim.level()) {
            log.accept("linked_target_reject: different level candidate " + describe(candidate));
            return true;
        }
        return false;
    }

    public static boolean shouldKillCandidate(Entity root, LivingEntity victim, LivingEntity candidate, DamageSource source, Consumer<String> log) {
        if (isForbiddenCandidate(root, victim, candidate, source, log)) return false;
        if (candidate.getType() == victim.getType()) return true;
        String victimClass = victim.getClass().getName();
        String candidateClass = candidate.getClass().getName();
        boolean sameModClassFamily = victimClass.contains("Goety") && candidateClass.contains("Goety");
        if (sameModClassFamily) return true;
        log.accept("linked_target_reject: not same type/family candidate " + describe(candidate) + ", victimType=" + victim.getType());
        return false;
    }

    public static boolean isCombatReferenceFieldName(String upperName) {
        return containsAny(upperName, "TARGET", "ATTACK", "HURT", "KILL", "LAST", "CREDIT", "REVENGE", "AGGRO", "ANGER", "COMBAT", "ENEMY");
    }

    public static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    public static String describe(Entity entity) {
        if (entity == null) return "null";
        return entity.getClass().getName() + ", type=" + entity.getType() + ", uuid=" + entity.getUUID() + ", removed=" + entity.isRemoved();
    }
}
