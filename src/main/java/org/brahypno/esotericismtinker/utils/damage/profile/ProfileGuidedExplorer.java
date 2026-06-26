package org.brahypno.esotericismtinker.utils.damage.profile;

import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageConstants;
import org.brahypno.esotericismtinker.utils.damage.DamageContext;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;
import org.brahypno.esotericismtinker.utils.damage.scan.FieldScanner;
import org.brahypno.esotericismtinker.utils.damage.state.DamageSnapshot;
import org.brahypno.esotericismtinker.utils.damage.step.KillPathKind;
import org.brahypno.esotericismtinker.utils.damage.step.StepResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public final class ProfileGuidedExplorer {
    private ProfileGuidedExplorer() {}

    public static void addProfileHeader(DamageContext context, DamageProbeResult result) {
        DamageProfile profile = DamageProfileCache.profileFor(context.victim());
        result.add("profile_summary: " + profile.summary());
        result.add("profile_step SUPER_SET_HEALTH: " + profile.step(ProbeStepId.SUPER_SET_HEALTH).summary());
        result.add("profile_step ENTITY_DATA_ABSOLUTE: " + profile.step(ProbeStepId.ENTITY_DATA_ABSOLUTE).summary());
        result.add("profile_step PRIVATE_FIELD_ABSOLUTE: " + profile.step(ProbeStepId.PRIVATE_FIELD_ABSOLUTE).summary());
        result.add("profile_step HARD_SET_HEALTH_ZERO: " + profile.step(ProbeStepId.HARD_SET_HEALTH_ZERO).summary());
    }

    public static boolean shouldSkip(DamageContext context, DamageProbeResult result, ProbeStepId id) {
        DamageProfile profile = DamageProfileCache.profileFor(context.victim());
        if (!profile.shouldSkip(id))
            return false;
        result.add("profile_skip: " + id + " for " + profile.key() + ", memory=" + profile.step(id).summary());
        return true;
    }

    public static StepResult record(DamageContext context, ProbeStepId id, StepResult stepResult, DamageSnapshot before, DamageSnapshot after, DamageProbeResult result) {
        boolean authoritative = after.authoritativeChangeFrom(before) || result.killConfirmed();
        float dealt = Math.max(0.0F, before.health() - after.health()) + Math.max(0.0F, before.absorption() - after.absorption());
        boolean profileSuccess = authoritative || dealt > DamageConstants.DAMAGE_EPS;
        DamageProfile profile = DamageProfileCache.profileFor(context.victim());
        profile.recordStep(id, profileSuccess, authoritative, dealt);
        if (!profileSuccess && isCoreLethalProbe(id))
            profile.observeZeroGate(id.name());
        result.add("profile_record: " + id
                   + ", stepProgress=" + stepResult.progress()
                   + ", profileSuccess=" + profileSuccess
                   + ", authoritative=" + authoritative
                   + ", dealt=" + dealt
                   + ", memory=" + profile.step(id).summary());
        return stepResult;
    }

    public static void observePossibleCap(DamageContext context, float requested, float dealt, DamageProbeResult result) {
        DamageProfile profile = DamageProfileCache.profileFor(context.victim());
        profile.observePossibleCap(requested, dealt);
        if (profile.stableDamageCap()){
            result.add(
                    "profile_cap_stable: observedCap=" + profile.observedCap() + ", hits=" + profile.capHits() + ", nextDirective=" + profile.nextDirective());
        }else if (dealt <= DamageConstants.DAMAGE_EPS && requested > DamageConstants.DAMAGE_EPS){
            result.add("profile_zero_gate_observed: requested=" + requested + ", zeroGateHits=" + profile.zeroGateHits() + ", nextDirective=" +
                       profile.nextDirective());
        }else if (dealt > DamageConstants.DAMAGE_EPS && dealt + DamageConstants.DAMAGE_TOLERANCE < requested){
            result.add("profile_cap_observed: requested=" + requested + ", dealt=" + dealt + ", observedCap=" + profile.observedCap() + ", hits=" +
                       profile.capHits());
        }
    }

    public static StepResult tryLearnedHealthFieldKill(DamageContext context, DamageProbeResult result) {
        DamageProfile profile = DamageProfileCache.profileFor(context.victim());
        Field field = profile.learnedHealthField();
        if (field == null)
            return StepResult.noProgress();
        LivingEntity victim = context.victim();
        DamageSnapshot before = DamageSnapshot.of(victim);
        try {
            field.setAccessible(true);
            Object beforeValue = field.get(victim);
            Object zeroValue = zeroLike(beforeValue);
            if (zeroValue == null)
                return StepResult.noProgress();
            field.set(victim, zeroValue);
            DamageSnapshot after = DamageSnapshot.of(victim);
            result.recordDamageLikeChange(before, after);
            boolean authoritative = after.authoritativeChangeFrom(before) || result.killConfirmed();
            float dealt = Math.max(0.0F, before.health() - after.health());
            result.add("learned_health_field_kill: " + describeField(field)
                       + ", value " + beforeValue + " -> " + zeroValue
                       + ", health " + before.health() + " -> " + after.health()
                       + ", authoritative=" + authoritative);
            profile.recordStep(ProbeStepId.LEARNED_HEALTH_FIELD, authoritative, authoritative, dealt);
            profile.recordLearnedHealthFieldResult(authoritative, field, "kill validation");
            if (authoritative)
                result.markKillPath(KillPathKind.SURFACE_HEALTH, "learned health field " + describeField(field));
            if (!authoritative)
                field.set(victim, beforeValue);
            return new StepResult(authoritative, dealt, authoritative ? KillPathKind.SURFACE_HEALTH : KillPathKind.NONE);
        }
        catch (Throwable e) {
            result.add("learned_health_field_kill error: " + e.getClass().getSimpleName());
            profile.recordStep(ProbeStepId.LEARNED_HEALTH_FIELD, false, false, 0.0F);
            profile.recordLearnedHealthFieldResult(false, field, e.getClass().getSimpleName());
            return StepResult.noProgress();
        }
    }

    public static StepResult tryLearnedCapFieldBypass(DamageContext context, DamageProbeResult result) {
        DamageProfile profile = DamageProfileCache.profileFor(context.victim());
        Field field = profile.learnedCapField();
        if (field == null)
            return StepResult.noProgress();
        LivingEntity victim = context.victim();
        Object beforeValue = null;
        try {
            field.setAccessible(true);
            beforeValue = field.get(victim);
            Object high = highLike(beforeValue);
            if (high == null)
                return StepResult.noProgress();

            DamageSnapshot beforeBypass = DamageSnapshot.of(victim);
            field.set(victim, high);
            result.add("learned_cap_field_bypass: " + describeField(field) + ", value " + beforeValue + " -> " + high);
            victim.invulnerableTime = 0;
            victim.hurtTime = 0;
            victim.hurtDuration = 0;
            victim.setHealth(0.0F);
            DamageSnapshot after = DamageSnapshot.of(victim);
            float dealt = Math.max(0.0F, beforeBypass.health() - after.health());
            boolean authoritative = after.authoritativeChangeFrom(beforeBypass) || result.killConfirmed();
            boolean bypassed = authoritative && bypassedObservedCap(profile, context.remainingOrAmount(result), dealt, result.killConfirmed());
            result.recordDamageLikeChange(beforeBypass, after);
            result.add("learned_cap_field_bypass result: health " + beforeBypass.health() + " -> " + after.health()
                       + ", dealt=" + dealt
                       + ", authoritative=" + authoritative
                       + ", bypassedCap=" + bypassed);
            profile.recordStep(ProbeStepId.LEARNED_CAP_FIELD, bypassed, bypassed, dealt);
            profile.recordLearnedCapFieldResult(bypassed, field, bypassed ? "bypass validation" : "cap-limited only");
            if (!bypassed){
                field.set(victim, beforeValue);
                profile.requestCapSourceRecheck("learned cap field did not bypass observed cap");
            }
            if (bypassed)
                result.markKillPath(KillPathKind.SURFACE_HEALTH, "learned cap field setHealth bypass " + describeField(field));
            return new StepResult(bypassed, dealt, bypassed ? KillPathKind.SURFACE_HEALTH : KillPathKind.NONE);
        }
        catch (Throwable e) {
            try {
                if (beforeValue != null)
                    field.set(victim, beforeValue);
            }
            catch (Throwable ignored) {}
            result.add("learned_cap_field_bypass error: " + e.getClass().getSimpleName());
            profile.recordStep(ProbeStepId.LEARNED_CAP_FIELD, false, false, 0.0F);
            profile.recordLearnedCapFieldResult(false, field, e.getClass().getSimpleName());
            return StepResult.noProgress();
        }
    }

    public static int exploreHealthBackingFields(DamageContext context, DamageProbeResult result) {
        LivingEntity victim = context.victim();
        DamageProfile profile = DamageProfileCache.profileFor(victim);
        if (profile.healthFieldExplored()){
            result.add("health_backing_exploration skip: already explored");
            return 0;
        }
        result.add("========== health backing exploration begin ==========");
        int hits = 0;
        Class<?> cls = victim.getClass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                try {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                        continue;
                    if (!FieldScanner.isNumericField(field) || profile.isRejectedHealthField(field))
                        continue;
                    field.setAccessible(true);
                    Object beforeValue = field.get(victim);
                    Object testValue = minusOneLike(beforeValue);
                    if (testValue == null)
                        continue;
                    float beforeHealth = victim.getHealth();
                    field.set(victim, testValue);
                    float afterHealth = victim.getHealth();
                    field.set(victim, beforeValue);
                    float restoredHealth = victim.getHealth();
                    boolean hit = Math.abs(beforeHealth - afterHealth) > 0.05F;
                    if (!hit)
                        continue;
                    hits++;
                    result.add("health_backing_candidate: " + describeField(field)
                               + ", value " + beforeValue + " -> " + testValue
                               + ", getHealth " + beforeHealth + " -> " + afterHealth
                               + ", restoredHealth=" + restoredHealth);
                    if (profile.learnedHealthField() == null)
                        profile.learnHealthField(field);
                }
                catch (Throwable e) {
                    result.add("health_backing_candidate error: " + describeField(field) + ", error=" + e.getClass().getSimpleName());
                }
            }
            cls = cls.getSuperclass();
        }
        profile.markHealthFieldExplored();
        profile.recordStep(ProbeStepId.HEALTH_BACKING_EXPLORATION, hits > 0, hits > 0, 0.0F);
        if (hits == 0)
            profile.addNote("health backing exploration found no direct numeric backing field");
        result.add("health_backing_exploration end: hits=" + hits + ", nextDirective=" + profile.nextDirective());
        return hits;
    }

    public static int exploreCapSource(DamageContext context, DamageProbeResult result) {
        LivingEntity victim = context.victim();
        DamageProfile profile = DamageProfileCache.profileFor(victim);
        if (profile.capSourceExplored()){
            result.add("cap_source_exploration skip: already explored");
            return 0;
        }
        result.add("========== cap source exploration begin ==========");
        result.add("cap_source profile=" + profile.summary());
        int fieldHits = inspectCapValueFields(context, victim, profile, result);
        int methodHits = inspectDamageRelevantMethods(victim, result);
        profile.markCapSourceExplored();
        profile.recordStep(ProbeStepId.CAP_SOURCE_EXPLORATION, fieldHits > 0 || methodHits > 0, false, 0.0F);
        if (fieldHits == 0 && methodHits == 0)
            profile.markSuspectBytecodeOrEventCap("no value-based cap/health candidate found");
        result.add("cap_source_exploration end: fieldHits=" + fieldHits + ", methodHits=" + methodHits + ", nextDirective=" + profile.nextDirective());
        return fieldHits + methodHits;
    }

    private static int inspectCapValueFields(DamageContext context, LivingEntity victim, DamageProfile profile, DamageProbeResult result) {
        int hits = 0;
        Class<?> cls = victim.getClass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                try {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                        continue;
                    if (!FieldScanner.isNumericField(field) || profile.isRejectedCapField(field))
                        continue;
                    field.setAccessible(true);
                    Object value = field.get(victim);
                    if (!looksLikeProbeRelevantValue(value, profile, victim))
                        continue;
                    result.add("cap_value_candidate: " + describeField(field)
                               + ", type=" + field.getType().getSimpleName()
                               + ", value=" + value
                               + ", observedCap=" + profile.observedCap()
                               + ", zeroGateHits=" + profile.zeroGateHits());
                    if (validateCapFieldCandidate(context, victim, field, profile, result))
                        hits++;
                }
                catch (Throwable e) {
                    result.add("cap_value_candidate error: " + describeField(field) + ", error=" + e.getClass().getSimpleName());
                }
            }
            cls = cls.getSuperclass();
        }
        return hits;
    }

    private static boolean validateCapFieldCandidate(DamageContext context, LivingEntity victim, Field field, DamageProfile profile, DamageProbeResult result) {
        Object oldValue = null;
        try {
            oldValue = field.get(victim);
            Object high = highLike(oldValue);
            if (high == null || Objects.equals(oldValue, high))
                return false;
            DamageSnapshot before = DamageSnapshot.of(victim);
            field.set(victim, high);
            victim.invulnerableTime = 0;
            victim.hurtTime = 0;
            victim.hurtDuration = 0;
            victim.setHealth(0.0F);
            DamageSnapshot after = DamageSnapshot.of(victim);
            boolean authoritative = after.authoritativeChangeFrom(before) || after.deathTime() > before.deathTime() || result.killConfirmed();
            float dealt = Math.max(0.0F, before.health() - after.health());
            boolean bypassed = authoritative && bypassedObservedCap(profile, context.remainingOrAmount(result), dealt, result.killConfirmed());
            result.add("cap_candidate_validation: " + describeField(field)
                       + ", value " + oldValue + " -> " + high
                       + ", health " + before.health() + " -> " + after.health()
                       + ", dealt=" + dealt
                       + ", authoritative=" + authoritative
                       + ", bypassedCap=" + bypassed);
            if (bypassed){
                profile.learnCapField(field);
                profile.recordStep(ProbeStepId.LEARNED_CAP_FIELD, true, true, dealt);
                return true;
            }
            field.set(victim, oldValue);
            profile.rejectCapField(field, authoritative ? "cap-limited only" : "candidate validation failed");
            return false;
        }
        catch (Throwable e) {
            try {
                if (oldValue != null)
                    field.set(victim, oldValue);
            }
            catch (Throwable ignored) {}
            profile.rejectCapField(field, e.getClass().getSimpleName());
            result.add("cap_candidate_validation error: " + describeField(field) + ", error=" + e.getClass().getSimpleName());
            return false;
        }
    }

    private static int inspectDamageRelevantMethods(LivingEntity victim, DamageProbeResult result) {
        int hits = 0;
        Class<?> cls = victim.getClass();
        while (cls != null && cls != Object.class) {
            for (Method method : cls.getDeclaredMethods()) {
                String upper = method.getName().toUpperCase(Locale.ROOT);
                if (!containsAny(upper, "HEALTH", "DAMAGE", "HURT", "DEATH", "DIE", "TICK", "INVUL", "PHASE", "COOLDOWN"))
                    continue;
                result.add("damage_method_candidate: " + method.getDeclaringClass().getSimpleName() + "#" + method.getName()
                           + ", synthetic=" + method.isSynthetic()
                           + ", bridge=" + method.isBridge()
                           + ", return=" + method.getReturnType().getSimpleName()
                           + ", params=" + Arrays.toString(method.getParameterTypes()));
                hits++;
            }
            cls = cls.getSuperclass();
        }
        return hits;
    }

    private static boolean looksLikeProbeRelevantValue(Object value, DamageProfile profile, LivingEntity victim) {
        if (!(value instanceof Number number))
            return false;
        double d = number.doubleValue();
        if (!Double.isFinite(d))
            return false;
        if (profile.observedCap() > 0.01F && Math.abs(d - profile.observedCap()) <= Math.max(0.25D, profile.observedCap() * 0.05D))
            return true;
        if (profile.stableZeroGate() && (Math.abs(d - victim.getHealth()) <= 0.25D || Math.abs(d - victim.getMaxHealth()) <= 0.25D))
            return true;
        return Math.abs(d - 20.0D) <= 0.25D
               || Math.abs(d - 40.0D) <= 0.25D
               || Math.abs(d - 0.85D) <= 0.01D
               || Math.abs(d - 0.5D) <= 0.01D
               || Math.abs(d - 0.05D) <= 0.01D;
    }

    private static boolean bypassedObservedCap(DamageProfile profile, float requested, float dealt, boolean killConfirmed) {
        if (killConfirmed)
            return true;
        if (dealt <= DamageConstants.DAMAGE_EPS)
            return false;
        if (!profile.stableDamageCap())
            return dealt + DamageConstants.DAMAGE_TOLERANCE >= requested;
        float tolerance = Math.max(0.5F, profile.observedCap() * 0.10F);
        return dealt > profile.observedCap() + tolerance;
    }

    private static boolean isCoreLethalProbe(ProbeStepId id) {
        return id == ProbeStepId.RAW_SET_HEALTH
               || id == ProbeStepId.SUPER_SET_HEALTH
               || id == ProbeStepId.HARD_SET_HEALTH_ZERO
               || id == ProbeStepId.FORCE_DIE
               || id == ProbeStepId.ENTITY_DATA_ABSOLUTE
               || id == ProbeStepId.PRIVATE_FIELD_ABSOLUTE;
    }

    private static Object minusOneLike(Object value) {
        if (value instanceof Float f)
            return f - 1.0F;
        if (value instanceof Double d)
            return d - 1.0D;
        if (value instanceof Integer i)
            return i - 1;
        if (value instanceof Long l)
            return l - 1L;
        if (value instanceof Short s)
            return (short) (s - 1);
        if (value instanceof Byte b)
            return (byte) (b - 1);
        return null;
    }

    private static Object zeroLike(Object value) {
        if (value instanceof Float)
            return 0.0F;
        if (value instanceof Double)
            return 0.0D;
        if (value instanceof Integer)
            return 0;
        if (value instanceof Long)
            return 0L;
        if (value instanceof Short)
            return (short) 0;
        if (value instanceof Byte)
            return (byte) 0;
        return null;
    }

    private static Object highLike(Object value) {
        if (value instanceof Float)
            return 1000000.0F;
        if (value instanceof Double)
            return 1000000.0D;
        if (value instanceof Integer)
            return 1000000;
        if (value instanceof Long)
            return 1000000L;
        if (value instanceof Short)
            return Short.MAX_VALUE;
        if (value instanceof Byte)
            return Byte.MAX_VALUE;
        return null;
    }

    private static String describeField(Field field) {
        return field == null ? "none" : field.getDeclaringClass().getSimpleName() + "#" + field.getName();
    }

    private static boolean containsAny(String s, String... keys) {
        for (String key : keys)
            if (s.contains(key))
                return true;
        return false;
    }
}
