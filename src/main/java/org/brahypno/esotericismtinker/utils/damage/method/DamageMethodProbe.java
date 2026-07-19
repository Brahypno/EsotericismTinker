package org.brahypno.esotericismtinker.utils.damage.method;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageConstants;
import org.brahypno.esotericismtinker.utils.damage.DamageContext;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;
import org.brahypno.esotericismtinker.utils.damage.state.DamageSnapshot;
import org.brahypno.esotericismtinker.utils.damage.step.StepResult;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic runtime probe for instance methods structurally shaped like a damage entry point:
 * (DamageSource, float/Float). Method names are deliberately ignored.
 */
public final class DamageMethodProbe {
    private static final int MAX_METHODS = 24;

    private DamageMethodProbe() {}

    public static StepResult tryDamageMethods(DamageContext context, DamageProbeResult result, String prefix) {
        LivingEntity victim = context.victim();
        DamageSource source = context.source();
        if (victim == null || source == null) {
            result.add(prefix + " skipped: victim/source=null");
            return StepResult.noProgress();
        }

        List<Method> candidates = findCandidates(victim.getClass(), source);
        result.add(prefix + " candidates=" + candidates.size());
        if (candidates.isEmpty()) return StepResult.noProgress();

        float bestDealt = 0.0F;
        boolean progress = false;
        int tried = 0;
        for (Method method : candidates) {
            if (tried++ >= MAX_METHODS) break;
            DamageSnapshot before = DamageSnapshot.of(victim);
            float input = Math.max(DamageConstants.DAMAGE_EPS, context.remainingOrAmount(result));
            try {
                GuardStateSupport.clearDamageGuards(victim, result, prefix);
                method.setAccessible(true);
                Object returned = method.invoke(victim, source, method.getParameterTypes()[1] == Float.class ? Float.valueOf(input) : input);
                DamageSnapshot after = DamageSnapshot.of(victim);
                float dealt = Math.max(0.0F, before.health() - after.health())
                        + Math.max(0.0F, before.absorption() - after.absorption());
                boolean authoritative = dealt > DamageConstants.DAMAGE_EPS
                        || before.alive() != after.alive()
                        || before.removed() != after.removed()
                        || after.deathTime() > before.deathTime()
                        || result.killConfirmed();
                result.add(prefix + " invoke: " + describe(method)
                        + ", return=" + returned
                        + ", input=" + input
                        + ", health " + before.health() + " -> " + after.health()
                        + ", absorption " + before.absorption() + " -> " + after.absorption()
                        + ", deathTime " + before.deathTime() + " -> " + after.deathTime()
                        + ", alive " + before.alive() + " -> " + after.alive()
                        + ", removed " + before.removed() + " -> " + after.removed()
                        + ", authoritative=" + authoritative);
                if (authoritative) {
                    progress = true;
                    bestDealt = Math.max(bestDealt, dealt);
                    if (result.killConfirmed() || result.reachedExpectedDamage()) break;
                }
            } catch (Throwable e) {
                result.add(prefix + " error: " + describe(method) + ", "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        result.add(prefix + " end: tried=" + Math.min(tried, MAX_METHODS)
                + ", progress=" + progress + ", bestDealt=" + bestDealt);
        return progress ? new StepResult(true, bestDealt) : StepResult.noProgress();
    }

    private static List<Method> findCandidates(Class<?> start, DamageSource source) {
        List<Method> methods = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int depth = 0;
        for (Class<?> cls = start; cls != null && cls != Object.class; cls = cls.getSuperclass(), depth++) {
            for (Method method : cls.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers)
                        || method.isBridge() || method.isSynthetic()) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 2 || !params[0].isInstance(source)) continue;
                if (params[1] != float.class && params[1] != Float.class) continue;
                String signature = method.getName() + java.util.Arrays.toString(params);
                if (seen.add(signature)) methods.add(method);
            }
        }
        methods.sort(Comparator
                .comparingInt((Method m) -> hierarchyDistance(start, m.getDeclaringClass()))
                .thenComparingInt(m -> returnRank(m.getReturnType()))
                .thenComparing(Method::getName));
        return methods;
    }

    private static int hierarchyDistance(Class<?> start, Class<?> declaring) {
        int distance = 0;
        for (Class<?> cls = start; cls != null; cls = cls.getSuperclass(), distance++) {
            if (cls == declaring) return distance;
        }
        return Integer.MAX_VALUE;
    }

    private static int returnRank(Class<?> type) {
        if (type == void.class) return 0;
        if (type == boolean.class || type == Boolean.class) return 1;
        if (Number.class.isAssignableFrom(type) || type.isPrimitive()) return 2;
        return 3;
    }

    private static String describe(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }
}
