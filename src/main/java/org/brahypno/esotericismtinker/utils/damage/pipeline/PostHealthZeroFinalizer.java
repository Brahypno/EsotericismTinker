package org.brahypno.esotericismtinker.utils.damage.pipeline;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageContext;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;
import org.brahypno.esotericismtinker.utils.damage.state.DamageSnapshot;
import org.brahypno.esotericismtinker.utils.damage.step.KillPathKind;
import org.brahypno.esotericismtinker.utils.damage.step.StepResult;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Post-zero death finalizer for paths that are semantically equivalent to
 * setHealth(0).
 * <p>
 * It is not used as an independent damage method and is not applied to
 * authority-state kills.
 */
final class PostHealthZeroFinalizer {
    private PostHealthZeroFinalizer() {}

    static StepResult finalizeSurfaceDeath(
            DamageContext context,
            DamageProbeResult result,
            String prefix
    ) {
        LivingEntity victim = context.victim();
        DamageSource source = context.source();

        if (victim == null || source == null){
            return StepResult.noProgress();
        }

        if (!result.needsSurfaceDeathFinalizer()){
            result.add(prefix
                       + " skipped: killPathKind=" + result.killPathKind()
                       + ", killConfirmed=" + result.killConfirmed()
                       + ", deathFinalized=" + result.deathFinalized());

            return StepResult.noProgress();
        }

        DamageSnapshot before = DamageSnapshot.of(victim);
        int beforeItems = nearbyItemCount(victim);

        result.add(prefix
                   + " begin: health=" + victim.getHealth()
                   + ", isAlive=" + victim.isAlive()
                   + ", removed=" + victim.isRemoved()
                   + ", deathTime=" + victim.deathTime
                   + ", nearbyItems=" + beforeItems);

        /*
         * Restore immediately before die(). Vanilla and Forge death processing
         * inspect lastHurtByPlayer and lastHurtByPlayerTime while constructing
         * the loot context and firing death-related hooks.
         */
        context.restoreAttackAttribution();

        boolean dieCalled = callDie(
                victim,
                source,
                result,
                prefix
        );

        int afterDieItems = nearbyItemCount(victim);

        result.add(prefix
                   + " after_die: called=" + dieCalled
                   + ", health=" + victim.getHealth()
                   + ", isAlive=" + victim.isAlive()
                   + ", removed=" + victim.isRemoved()
                   + ", deathTime=" + victim.deathTime
                   + ", nearbyItems=" + afterDieItems);

        boolean dropCalled = false;

        if (afterDieItems <= beforeItems){
            /*
             * die() or a modded death callback may alter attacker attribution,
             * so restore it again before invoking a direct loot fallback.
             */
            context.restoreAttackAttribution();

            dropCalled = invokeDropFallback(
                    victim,
                    source,
                    result,
                    prefix
            );
        }

        DamageSnapshot after = DamageSnapshot.of(victim);
        int afterItems = nearbyItemCount(victim);

        boolean changed =
                after.deathTime() != before.deathTime()
                || after.removed() != before.removed()
                || after.alive() != before.alive()
                || afterItems > beforeItems
                || dieCalled
                || dropCalled;

        result.add(prefix
                   + " end: dieCalled=" + dieCalled
                   + ", dropFallbackCalled=" + dropCalled
                   + ", health=" + victim.getHealth()
                   + ", isAlive=" + victim.isAlive()
                   + ", removed=" + victim.isRemoved()
                   + ", deathTime=" + victim.deathTime
                   + ", nearbyItems=" + afterItems
                   + ", changed=" + changed);

        if (changed){
            result.markDeathFinalized(prefix);
            result.markKillPath(
                    KillPathKind.DEATH_FINALIZER,
                    prefix
            );

            if (result.killConfirmed()){
                result.serverDead(prefix
                                  + " confirmed, health=" + victim.getHealth()
                                  + ", isAlive=" + victim.isAlive()
                                  + ", removed=" + victim.isRemoved()
                                  + ", deathTime=" + victim.deathTime);
            }
        }

        return new StepResult(
                changed,
                0.0F,
                changed
                ? KillPathKind.DEATH_FINALIZER
                : KillPathKind.NONE
        );
    }

    private static boolean callDie(
            LivingEntity victim,
            DamageSource source,
            DamageProbeResult result,
            String prefix
    ) {
        try {
            victim.die(source);
            return true;
        }
        catch (Throwable throwable) {
            result.add(prefix
                       + " die error: "
                       + throwable.getClass().getSimpleName());

            return false;
        }
    }

    private static boolean invokeDropFallback(
            LivingEntity victim,
            DamageSource source,
            DamageProbeResult result,
            String prefix
    ) {
        for (Method method : dropMethods(victim.getClass())) {
            try {
                Object[] args = argsFor(method, source);

                if (args == null){
                    continue;
                }

                if (!method.trySetAccessible()){
                    result.add(prefix
                               + " drop_fallback inaccessible: "
                               + method.getDeclaringClass().getSimpleName()
                               + "#"
                               + method.getName());

                    continue;
                }

                method.invoke(victim, args);

                result.add(prefix
                           + " drop_fallback invoked: "
                           + method.getDeclaringClass().getSimpleName()
                           + "#"
                           + method.getName());

                return true;
            }
            catch (Throwable throwable) {
                result.add(prefix
                           + " drop_fallback error: "
                           + method.getDeclaringClass().getSimpleName()
                           + "#"
                           + method.getName()
                           + ", error="
                           + throwable.getClass().getSimpleName());
            }
        }

        result.add(prefix
                   + " drop_fallback skipped: no compatible drop method");

        return false;
    }

    private static List<Method> dropMethods(Class<?> start) {
        List<Method> methods = new ArrayList<>();

        Class<?> current = start;

        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (!isDropCandidate(method)){
                    continue;
                }

                methods.add(method);
            }

            current = current.getSuperclass();
        }

        methods.sort(
                Comparator.comparingInt(
                        PostHealthZeroFinalizer::scoreDropMethod
                ).reversed()
        );

        return methods;
    }

    private static boolean isDropCandidate(Method method) {
        int modifiers = method.getModifiers();

        if (Modifier.isStatic(modifiers)
            || Modifier.isAbstract(modifiers)
            || method.isBridge()
            || method.isSynthetic()){
            return false;
        }

        /*
         * Death-loot methods are side-effect methods. Reject non-void methods
         * to avoid accidentally invoking helpers such as getLootTable(),
         * shouldDropLoot() or methods returning generated collections.
         */
        if (method.getReturnType() != void.class){
            return false;
        }

        String lower = method.getName().toLowerCase(Locale.ROOT);

        boolean explicitKnownName =
                lower.equals("dropalldeathloot")
                || lower.equals("dropdeathloot")
                || lower.equals("dropcustomdeathloot")
                || lower.equals("dropfromloottable");

        boolean semanticName =
                lower.startsWith("drop")
                && (lower.contains("death")
                    || lower.contains("loot"));

        if (!explicitKnownName && !semanticName){
            return false;
        }

        return supportsDropSignature(method);
    }

    private static boolean supportsDropSignature(Method method) {
        Class<?>[] params = method.getParameterTypes();

        if (params.length == 1){
            return acceptsDamageSource(params[0]);
        }

        if (params.length == 2){
            return acceptsDamageSource(params[0])
                   && params[1] == boolean.class;
        }

        if (params.length == 3){
            return acceptsDamageSource(params[0])
                   && params[1] == int.class
                   && params[2] == boolean.class;
        }

        return false;
    }

    private static int scoreDropMethod(Method method) {
        String lower = method.getName().toLowerCase(Locale.ROOT);
        int score = 0;

        /*
         * Prefer the canonical complete-death-loot method first.
         */
        switch (lower) {
            case "dropalldeathloot" -> score += 1000;
            case "dropdeathloot" -> score += 800;
            case "dropcustomdeathloot" -> score += 600;
            case "dropfromloottable" -> score += 400;
        }

        if (lower.contains("all")){
            score += 80;
        }

        if (lower.contains("death")){
            score += 60;
        }

        if (lower.contains("loot")){
            score += 40;
        }

        /*
         * A DamageSource-exact signature is safer than a broad Object or
         * interface parameter.
         */
        Class<?> firstParam = method.getParameterTypes()[0];

        if (firstParam == DamageSource.class){
            score += 50;
        }else if (firstParam.isAssignableFrom(DamageSource.class)){
            score += 20;
        }

        /*
         * Complete methods with source, looting level and player-kill flag are
         * preferred over less descriptive overloads when names score equally.
         */
        score += method.getParameterCount() * 5;

        /*
         * Prefer implementations nearer to the concrete entity class.
         */
        score += declaringDepth(method.getDeclaringClass());

        return score;
    }

    private static int declaringDepth(Class<?> type) {
        int depth = 0;
        Class<?> current = type;

        while (current != null && current != Object.class) {
            depth++;
            current = current.getSuperclass();
        }

        return depth;
    }

    private static Object[] argsFor(
            Method method,
            DamageSource source
    ) {
        Class<?>[] params = method.getParameterTypes();

        if (params.length == 1
            && acceptsArgument(params[0], source)){
            return new Object[]{source};
        }

        if (params.length == 2
            && acceptsArgument(params[0], source)
            && params[1] == boolean.class){
            return new Object[]{
                    source,
                    true
            };
        }

        if (params.length == 3
            && acceptsArgument(params[0], source)
            && params[1] == int.class
            && params[2] == boolean.class){
            return new Object[]{
                    source,
                    0,
                    true
            };
        }

        return null;
    }

    /**
     * Checks whether a parameter type can receive a DamageSource.
     * <p>
     * The previous implementation used:
     * <p>
     * DamageSource.class.isAssignableFrom(parameterType)
     * <p>
     * That direction answers whether parameterType is DamageSource or a
     * subclass of it. It may therefore accept an overly narrow subclass that
     * cannot receive the actual base DamageSource instance.
     * <p>
     * The parameter must instead be DamageSource itself or a supertype capable
     * of receiving one.
     */
    private static boolean acceptsDamageSource(Class<?> parameterType) {
        return parameterType.isAssignableFrom(DamageSource.class);
    }

    private static boolean acceptsArgument(
            Class<?> parameterType,
            Object argument
    ) {
        return parameterType.isInstance(argument);
    }

    private static int nearbyItemCount(LivingEntity victim) {
        try {
            return victim.level()
                         .getEntitiesOfClass(
                                 ItemEntity.class,
                                 victim.getBoundingBox().inflate(12.0D)
                         )
                         .size();
        }
        catch (Throwable ignored) {
            return -1;
        }
    }
}