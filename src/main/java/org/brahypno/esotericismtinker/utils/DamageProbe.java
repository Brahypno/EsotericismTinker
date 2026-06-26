package org.brahypno.esotericismtinker.utils;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.LootHelper.LootResolver;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public final class DamageProbe {
    private static final int MAX_RETRY_TOTAL = 12;
    private static final int MAX_ABSOLUTE_FIELD_TRIES = 32;
    private static final int MAX_ABSOLUTE_DATA_TRIES = 24;
    private static final int MAX_LINKED_DEPTH = 2;
    private static final int MAX_LINKED_TARGETS = 8;

    private static final int MIN_ENTITY_DATA_SCORE = 55;
    private static final int MIN_FIELD_SCORE = 60;

    private static final float DAMAGE_EPS = 0.01F;
    private static final float DAMAGE_TOLERANCE = 0.999F;

    private DamageProbe() {}

    public static boolean damageHandler(@Nullable Entity entity, DamageSource damageSource, float damageAmount) {
        if (entity == null)
            return false;

        LivingEntity victim = ETHelper.getLivingTarget(entity);
        if (victim != null && !victim.level().isClientSide){
            return MethodHandler.invokeLivingHurt(victim, damageSource, damageAmount);
        }

        return entity.hurt(damageSource, damageAmount);
    }

    /**
     * Medium strength damage.
     * <p>
     * This is the old finalDamageMethod semantics:
     * - direct hurt
     * - Dreamtinker damage handler
     * - raw setHealth
     * - unsafe EntityData
     * - unsafe private fields
     * - retry the useful step
     * <p>
     * Use this for normal powerful tools.
     */
    public static Result mediumDamageMethod(@Nullable Entity entity, DamageSource source, float amount) {
        LivingEntity victim = ETHelper.getLivingTarget(entity);
        Result result = new Result(entity, victim, source, amount, ForceLevel.MEDIUM);

        if (entity == null || source == null || amount <= 0.0F)
            return result.fail("invalid args");
        if (victim == null)
            return result.fail("no living target");
        if (victim.level().isClientSide)
            return result.fail("client side");
        if (!victim.isAlive())
            return result.serverDead("victim already not alive, health=" + victim.getHealth());

        result.addHeader("medium begin");

        List<PlanStep> steps = buildMediumSteps(entity, victim, source);
        List<RetryCandidate> candidates = new ArrayList<>();

        for (PlanStep step : steps) {
            if (result.reachedExpectedDamage())
                return result.success(step.name());

            StepResult attempt = runStepOnce(step, result);
            if (attempt.progress() && step.kind() == StepKind.DAMAGE){
                candidates.add(new RetryCandidate(step, attempt.dealt()));
            }

            if (result.reachedExpectedDamage())
                return result.success(step.name());
        }

        if (result.reachedExpectedDamage()){
            return result.success("first_pass_accumulated");
        }

        if (candidates.isEmpty()){
            return result.fail("all medium strategies failed");
        }

        candidates.sort(Comparator.comparingInt(c -> c.estimatedOps(result.remainingAmount())));
        result.add("medium retry_candidates=" + candidates.stream()
                                                          .map(c -> c.step().name()
                                                                    + "/dealt=" + c.dealtPerOp()
                                                                    + "/estimatedOps=" + c.estimatedOps(result.remainingAmount()))
                                                          .toList());

        for (RetryCandidate candidate : candidates) {
            int limit = Math.min(MAX_RETRY_TOTAL, Math.max(1, candidate.estimatedOps(result.remainingAmount()) + 2));
            result.add("medium retry_select: " + candidate.step().name() + ", limit=" + limit);

            for (int i = 0; i < limit && !result.reachedExpectedDamage(); ++i) {
                StepResult attempt = runStepOnce(candidate.step(), result);
                result.add("medium retry " + candidate.step().name() + " #" + i
                           + ": progress=" + attempt.progress()
                           + ", dealt=" + attempt.dealt()
                           + ", remaining=" + result.remainingAmount());

                if (!attempt.progress())
                    break;
            }

            if (result.reachedExpectedDamage()){
                return result.success("medium_retry_" + candidate.step().name());
            }
        }

        return result.fail("all medium strategies exhausted");
    }

    /**
     * True final damage.
     * <p>
     * Semantics:
     * - Always calls medium first.
     * - Non-lethal damage only requires instant effect in this tick.
     * - Lethal damage must kill/remove the target in this tick.
     * <p>
     * Use this only for the strongest tools/effects.
     */
    public static Result finalDamageMethod(@Nullable Entity entity, DamageSource source, float amount) {
        Result result = mediumDamageMethod(entity, source, amount);
        LivingEntity victim = result.victim;

        result.addHeader("final begin");

        if (entity == null || source == null || amount <= 0.0F){
            result.add("final abort: invalid args");
            return result;
        }

        if (victim == null){
            result.add("final abort: no living target");
            return result;
        }

        if (victim.level().isClientSide){
            result.add("final abort: client side");
            return result;
        }

        boolean lethalRequested = amount + DAMAGE_EPS >= Math.max(0.0F, result.initialHealth());
        result.markFinalRequested(lethalRequested);

        result.add("final decision: initialHealth=" + result.initialHealth()
                   + ", currentHealth=" + victim.getHealth()
                   + ", amount=" + amount
                   + ", lethalRequested=" + lethalRequested
                   + ", mediumSuccess=" + result.success()
                   + ", mediumInstantEffective=" + result.instantEffective()
                   + ", mediumKillConfirmed=" + result.killConfirmed());

        if (lethalRequested){
            if (result.killConfirmed()){
                return result.success("final: medium kill confirmed");
            }

            result.clearSuccess("final lethal escalation required; medium did not kill");
            return finalLethalEscalation(entity, victim, source, amount, result);
        }

        if (result.instantEffective()){
            return result.success("final: medium instant effective");
        }

        result.clearSuccess("final non-lethal escalation required; medium had no instant effect");
        return finalNonLethalEscalation(entity, victim, source, amount, result);
    }

    /**
     * Compatibility default: medium strength.
     * <p>
     * Old callers will not automatically become absolute execution tools.
     */
    public static boolean damageBoolean(@Nullable Entity entity, DamageSource source, float amount) {
        return mediumDamageMethod(entity, source, amount).success();
    }

    /**
     * Explicit true final boolean entry.
     */
    public static boolean finalDamageBoolean(@Nullable Entity entity, DamageSource source, float amount) {
        return finalDamageMethod(entity, source, amount).success();
    }

    private static Result finalNonLethalEscalation(Entity entity, LivingEntity victim, DamageSource source, float amount, Result result) {
        result.addHeader("final non-lethal escalation begin");

        for (PlanStep step : buildFinalSupportSteps(entity, victim, source)) {
            runStepOnce(step, result);

            if (result.instantEffective()){
                return result.success("final_non_lethal_support_" + step.name());
            }
        }

        result.add("final non-lethal: retry medium damage path after support steps");

        for (PlanStep step : buildMediumSteps(entity, victim, source)) {
            runStepOnce(step, result);

            if (result.instantEffective()){
                return result.success("final_non_lethal_medium_after_support_" + step.name());
            }
        }

        result.add("final non-lethal: trying absolute entity data and field write");

        for (PlanStep step : buildFinalDamageSteps(entity, victim, source)) {
            runStepOnce(step, result);

            if (result.instantEffective()){
                return result.success("final_non_lethal_absolute_" + step.name());
            }
        }

        return result.fail("final non-lethal escalation failed");
    }

    private static Result finalLethalEscalation(Entity entity, LivingEntity victim, DamageSource source, float amount, Result result) {
        result.addHeader("final lethal escalation begin");
        result.add("final lethal initial state: health=" + victim.getHealth()
                   + ", isAlive=" + victim.isAlive()
                   + ", removed=" + victim.isRemoved()
                   + ", deathTime=" + victim.deathTime
                   + ", amount=" + amount);

        for (PlanStep step : buildFinalSupportSteps(entity, victim, source)) {
            runStepOnce(step, result);

            if (result.killConfirmed()){
                return result.success("final_lethal_support_" + step.name());
            }
        }

        result.add("final lethal: retry medium damage path after support steps");

        for (PlanStep step : buildMediumSteps(entity, victim, source)) {
            runStepOnce(step, result);

            if (result.killConfirmed()){
                return result.success("final_lethal_medium_after_support_" + step.name());
            }
        }

        result.add("final lethal: trying absolute data/field path");

        for (PlanStep step : buildFinalDamageSteps(entity, victim, source)) {
            runStepOnce(step, result);

            if (result.killConfirmed()){
                return result.success("final_lethal_absolute_" + step.name());
            }
        }

        hardSetHealthZero(victim, source, result);

        if (result.killConfirmed()){
            return result.success("final_lethal_hard_set_health_zero");
        }

        forceDeathNow(victim, source, result);

        if (result.killConfirmed()){
            return result.success("final_lethal_force_death_now");
        }

        tryKillLinkedTargets(entity, source, result);

        if (result.killConfirmed()){
            return result.success("final_lethal_linked_target");
        }

        forceRemove(victim, result);

        if (result.killConfirmed()){
            return result.success("final_lethal_force_remove");
        }

        result.add("final lethal final state: health=" + victim.getHealth()
                   + ", isAlive=" + victim.isAlive()
                   + ", removed=" + victim.isRemoved()
                   + ", deathTime=" + victim.deathTime);

        return result.fail("final lethal escalation failed");
    }

    private static List<PlanStep> buildMediumSteps(Entity entity, LivingEntity victim, DamageSource source) {
        List<PlanStep> steps = new ArrayList<>();

        if (entity != victim){
            steps.add(damageStep("direct_entity_hurt", (amount, result) ->
                    tryDirectEntityHurt(entity, source, amount, result)));
        }

        steps.add(damageStep("dreamtinker_damage_handler", (amount, result) ->
                tryDreamtinkerDamageHandler(entity, victim, source, amount, result)));

        steps.add(damageStep("raw_set_health", (amount, result) ->
                tryRawSetHealth(victim, source, amount, result)));

        steps.add(damageStep("entity_data_unsafe", (amount, result) ->
                tryEntityDataUnsafe(victim, source, amount, result)));

        steps.add(damageStep("private_field_unsafe", (amount, result) ->
                tryPrivateFieldUnsafe(victim, source, amount, result)));

        return steps;
    }

    private static List<PlanStep> buildFinalSupportSteps(Entity entity, LivingEntity victim, DamageSource source) {
        List<PlanStep> steps = new ArrayList<>();

        steps.add(supportStep("final_clear_all_cooldowns", (amount, result) ->
                tryClearAllCooldowns(entity, victim, result)));

        steps.add(supportStep("final_cap_surgery", (amount, result) ->
                tryCapSurgery(victim, result)));

        steps.add(supportStep("final_linked_target_probe", (amount, result) ->
                tryLinkedTargets(entity, source, amount, result)));

        return steps;
    }

    private static List<PlanStep> buildFinalDamageSteps(Entity entity, LivingEntity victim, DamageSource source) {
        List<PlanStep> steps = new ArrayList<>();

        steps.add(damageStep("final_entity_data_absolute", (amount, result) ->
                tryEntityDataAbsolute(victim, source, amount, result)));

        steps.add(damageStep("final_private_field_absolute", (amount, result) ->
                tryPrivateFieldAbsolute(victim, source, amount, result)));

        return steps;
    }

    private static StepResult runStepOnce(PlanStep step, Result result) {
        float amount = result.remainingAmountForStep();
        if (amount <= DAMAGE_EPS && step.kind() == StepKind.DAMAGE)
            return StepResult.noProgress();

        float before = result.totalDealt();
        boolean rawProgress;

        try {
            rawProgress = step.runner().run(amount, result);
        }
        catch (Throwable e) {
            result.add(step.name() + " fatal error: " + e.getClass().getSimpleName());
            return StepResult.noProgress();
        }

        float dealt = result.totalDealt() - before;
        boolean progress = step.kind() == StepKind.SUPPORT
                           ? rawProgress
                           : rawProgress && dealt > DAMAGE_EPS;

        result.add(step.name() + " once: kind=" + step.kind()
                   + ", input=" + amount
                   + ", rawProgress=" + rawProgress
                   + ", dealt=" + dealt
                   + ", totalDealt=" + result.totalDealt()
                   + ", remaining=" + result.remainingAmount()
                   + ", instantEffective=" + result.instantEffective()
                   + ", killConfirmed=" + result.killConfirmed());

        return new StepResult(progress, Math.max(0.0F, dealt));
    }

    private static boolean tryDirectEntityHurt(Entity entity, DamageSource source, float amount, Result result) {
        if (amount <= DAMAGE_EPS)
            return false;

        clearInvulnerability(entity, result.victim);

        float beforeHealth = result.health();
        float beforeAbsorption = result.absorption();

        boolean damaged;
        try {
            damaged = entity.hurt(source, amount);
        }
        catch (Throwable e) {
            result.add("direct_entity_hurt error: " + e.getClass().getSimpleName());
            return false;
        }

        float afterHealth = result.health();
        float afterAbsorption = result.absorption();

        if (damaged)
            result.markPipelineEntered("direct_entity_hurt returned true");

        result.recordDamageLikeChange(beforeHealth, afterHealth, beforeAbsorption, afterAbsorption);
        result.add("direct_entity_hurt: returned=" + damaged
                   + ", health " + beforeHealth + " -> " + afterHealth
                   + ", absorption " + beforeAbsorption + " -> " + afterAbsorption);

        result.recordFieldChanges("direct_entity_hurt");
        handleServerDeath(result.victim, source, result, "direct_entity_hurt", false);

        return damaged || beforeHealth > afterHealth || beforeAbsorption > afterAbsorption || result.serverDead();
    }

    private static boolean tryDreamtinkerDamageHandler(Entity entity, LivingEntity victim, DamageSource source, float amount, Result result) {
        if (amount <= DAMAGE_EPS)
            return false;

        clearInvulnerability(entity, victim);

        float beforeHealth = victim.getHealth();
        float beforeAbsorption = victim.getAbsorptionAmount();

        boolean damaged;
        try {
            damaged = damageHandler(entity, source, amount);
        }
        catch (Throwable e) {
            result.add("dreamtinker_damage_handler error: " + e.getClass().getSimpleName());
            return false;
        }

        float afterHealth = victim.getHealth();
        float afterAbsorption = victim.getAbsorptionAmount();

        if (damaged)
            result.markPipelineEntered("dreamtinker_damage_handler returned true");

        result.recordDamageLikeChange(beforeHealth, afterHealth, beforeAbsorption, afterAbsorption);
        result.add("dreamtinker_damage_handler: returned=" + damaged
                   + ", health " + beforeHealth + " -> " + afterHealth
                   + ", absorption " + beforeAbsorption + " -> " + afterAbsorption);

        result.recordFieldChanges("dreamtinker_damage_handler");
        handleServerDeath(victim, source, result, "dreamtinker_damage_handler", false);

        return damaged || beforeHealth > afterHealth || beforeAbsorption > afterAbsorption || result.serverDead();
    }

    private static boolean tryRawSetHealth(LivingEntity victim, DamageSource source, float amount, Result result) {
        if (amount <= DAMAGE_EPS)
            return false;

        clearInvulnerability(victim, victim);

        float before = victim.getHealth();
        if (before <= 0.0F)
            return false;

        try {
            victim.setHealth(Math.max(0.0F, before - amount));

            float after = victim.getHealth();

            result.recordHealthChange(before, after);
            result.add("raw_set_health: health " + before + " -> " + after);

            if (before > after)
                result.markStateChanged("raw_set_health health changed");

            result.recordFieldChanges("raw_set_health");
            handleServerDeath(victim, source, result, "raw_set_health", true);

            return before > after || result.serverDead();
        }
        catch (Throwable e) {
            result.add("raw_set_health error: " + e.getClass().getSimpleName());
            return false;
        }
    }

    private static boolean tryEntityDataUnsafe(LivingEntity victim, DamageSource source, float amount, Result result) {
        if (amount <= DAMAGE_EPS)
            return false;

        List<DataCandidate> candidates = collectEntityDataCandidates(victim, false, MIN_ENTITY_DATA_SCORE);
        result.add("entity_data candidates=" + candidates.size() + ", input=" + amount);

        for (DataCandidate candidate : candidates) {
            EntityDataMove move = tryMoveEntityData(victim, candidate, amount, result);
            if (!move.moved())
                continue;

            result.recordSyntheticDamage(move.dealtEquivalent());
            result.markStateChanged("entity_data moved " + candidate.name());

            handleServerDeath(victim, source, result, "entity_data", true);
            return true;
        }

        return false;
    }

    private static boolean tryPrivateFieldUnsafe(LivingEntity victim, DamageSource source, float amount, Result result) {
        if (amount <= DAMAGE_EPS)
            return false;

        List<FieldCandidate> candidates = collectFieldCandidates(victim, result, false, MIN_FIELD_SCORE);
        result.add("private_field candidates=" + candidates.size() + ", input=" + amount);

        for (FieldCandidate candidate : candidates) {
            FieldMove move = tryMoveField(victim, candidate, amount, result);
            if (!move.moved())
                continue;

            result.recordSyntheticDamage(move.dealtEquivalent());
            result.markStateChanged("private_field moved " + candidate.name());

            handleServerDeath(victim, source, result, "private_field", true);
            return true;
        }

        return false;
    }

    private static boolean tryEntityDataAbsolute(LivingEntity victim, DamageSource source, float amount, Result result) {
        if (amount <= DAMAGE_EPS)
            return false;

        List<DataCandidate> candidates = collectEntityDataCandidates(victim, true, Integer.MIN_VALUE);
        result.add("absolute_entity_data candidates=" + candidates.size() + ", input=" + amount);

        int tried = 0;
        for (DataCandidate candidate : candidates) {
            if (++tried > MAX_ABSOLUTE_DATA_TRIES)
                break;

            EntityDataMove move = tryMoveEntityData(victim, candidate, amount, result);
            if (!move.moved())
                continue;

            result.recordSyntheticDamage(move.dealtEquivalent());
            result.markStateChanged("absolute_entity_data moved " + candidate.name());

            handleServerDeath(victim, source, result, "absolute_entity_data", true);
            return true;
        }

        return false;
    }

    private static boolean tryPrivateFieldAbsolute(LivingEntity victim, DamageSource source, float amount, Result result) {
        if (amount <= DAMAGE_EPS)
            return false;

        List<FieldCandidate> candidates = collectFieldCandidates(victim, result, true, Integer.MIN_VALUE);
        result.add("absolute_private_field candidates=" + candidates.size() + ", input=" + amount);

        int tried = 0;
        for (FieldCandidate candidate : candidates) {
            if (++tried > MAX_ABSOLUTE_FIELD_TRIES)
                break;

            FieldMove move = tryMoveField(victim, candidate, amount, result);
            if (!move.moved())
                continue;

            result.recordSyntheticDamage(move.dealtEquivalent());
            result.markStateChanged("absolute_private_field moved " + candidate.name());

            handleServerDeath(victim, source, result, "absolute_private_field", true);
            return true;
        }

        return false;
    }

    private static boolean tryClearAllCooldowns(Entity entity, LivingEntity victim, Result result) {
        boolean changed = false;

        try {
            if (entity != null){
                entity.invulnerableTime = 0;
                changed = true;
            }

            if (victim != null){
                victim.invulnerableTime = 0;
                victim.hurtTime = 0;
                victim.hurtDuration = 0;
                changed = true;
            }

            result.add("final_clear_all_cooldowns: entityInvul="
                       + (entity == null ? "null" : entity.invulnerableTime)
                       + ", victimInvul=" + (victim == null ? "null" : victim.invulnerableTime)
                       + ", hurtTime=" + (victim == null ? "null" : victim.hurtTime)
                       + ", hurtDuration=" + (victim == null ? "null" : victim.hurtDuration));
        }
        catch (Throwable e) {
            result.add("final_clear_all_cooldowns error: " + e.getClass().getSimpleName());
        }

        return changed;
    }

    private static boolean tryCapSurgery(LivingEntity victim, Result result) {
        boolean changed = false;

        Class<?> cls = victim.getClass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                try {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                        continue;
                    if (!isNumericField(field))
                        continue;

                    field.setAccessible(true);

                    String name = field.getName().toUpperCase(Locale.ROOT);
                    Object before = field.get(victim);
                    Object after = null;

                    if (containsAny(name, "DAMAGECAP", "DAMAGE_CAP", "MAXDAMAGE", "MAX_DAMAGE", "DAMAGE_LIMIT", "DAMAGELIMIT")){
                        after = highValue(before);
                    }else if (containsAny(name, "COOLDOWN", "INVUL", "INVINCIBLE", "IMMUNE", "HURTTIME", "HURT_TIME", "DAMAGE_TICK", "LAST_DAMAGE_TICK")){
                        after = zeroValue(before);
                    }else if (containsAny(name, "SHIELD", "BARRIER", "GUARD")){
                        after = zeroValue(before);
                    }

                    if (after == null)
                        continue;
                    if (Objects.equals(before, after))
                        continue;

                    field.set(victim, after);

                    result.add("final_cap_surgery "
                               + field.getDeclaringClass().getSimpleName()
                               + "#" + field.getName()
                               + ": " + before + " -> " + after);

                    changed = true;
                }
                catch (Throwable e) {
                    result.add("final_cap_surgery field error: "
                               + field.getName()
                               + ", error=" + e.getClass().getSimpleName());
                }
            }

            cls = cls.getSuperclass();
        }

        return changed;
    }

    private static boolean tryLinkedTargets(Entity entity, DamageSource source, float amount, Result result) {
        List<LivingEntity> linkedTargets = findLinkedLivingTargets(entity, result);
        result.add("final_linked_target_probe: found=" + linkedTargets.size());

        boolean changed = false;
        for (LivingEntity linked : linkedTargets) {
            if (linked == result.victim)
                continue;
            if (!linked.isAlive() || linked.level().isClientSide)
                continue;

            result.add("final_linked_target_probe target=" + describeEntity(linked)
                       + ", health=" + linked.getHealth());

            Result linkedResult = mediumDamageMethod(linked, source, amount);
            result.add("final_linked_target_probe result=" + linkedResult.compactDebugText());

            if (linkedResult.instantEffective()){
                result.markStateChanged("linked target medium effective " + linked.getType());
                changed = true;
            }

            if (linkedResult.serverDead() || linkedResult.killConfirmed()){
                result.serverDead("linked target died " + linked.getType());
                changed = true;
            }
        }

        return changed;
    }

    private static void tryKillLinkedTargets(Entity entity, DamageSource source, Result result) {
        List<LivingEntity> linkedTargets = findLinkedLivingTargets(entity, result);
        result.add("final_kill_linked_targets: found=" + linkedTargets.size());

        for (LivingEntity linked : linkedTargets) {
            if (linked == result.victim)
                continue;
            if (!linked.isAlive() || linked.level().isClientSide)
                continue;

            result.add("final_kill_linked_targets target=" + describeEntity(linked)
                       + ", health=" + linked.getHealth());

            hardSetHealthZero(linked, source, result);
            forceDeathNow(linked, source, result);
            forceRemove(linked, result);
        }
    }

    private static List<LivingEntity> findLinkedLivingTargets(Entity entity, Result result) {
        List<LivingEntity> found = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        collectLinkedLivingTargets(entity, found, visited, result, 0);

        LinkedHashMap<UUID, LivingEntity> unique = new LinkedHashMap<>();
        for (LivingEntity living : found) {
            if (living != null)
                unique.put(living.getUUID(), living);
            if (unique.size() >= MAX_LINKED_TARGETS)
                break;
        }

        return new ArrayList<>(unique.values());
    }

    private static void collectLinkedLivingTargets(Object object, List<LivingEntity> found, Set<Object> visited, Result result, int depth) {
        if (object == null || depth > MAX_LINKED_DEPTH || !visited.add(object))
            return;

        if (object instanceof LivingEntity living){
            found.add(living);
        }

        Class<?> cls = object.getClass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                try {
                    if (Modifier.isStatic(field.getModifiers()))
                        continue;

                    String name = field.getName().toUpperCase(Locale.ROOT);
                    if (!containsAny(name, "OWNER", "PARENT", "HOST", "CORE", "BOSS", "MAIN", "MASTER", "CONTROLLER", "TARGET")){
                        continue;
                    }

                    field.setAccessible(true);
                    Object value = field.get(object);

                    if (value instanceof LivingEntity linked){
                        found.add(linked);
                        result.add("linked_target field "
                                   + field.getDeclaringClass().getSimpleName()
                                   + "#" + field.getName()
                                   + " -> " + describeEntity(linked));
                    }else {
                        collectLinkedLivingTargets(value, found, visited, result, depth + 1);
                    }
                }
                catch (Throwable ignored) {}
            }

            cls = cls.getSuperclass();
        }
    }

    private static void hardSetHealthZero(LivingEntity victim, DamageSource source, Result result) {
        try {
            clearInvulnerability(victim, victim);

            float before = victim.getHealth();
            victim.setHealth(0.0F);
            float after = victim.getHealth();

            result.recordHealthChange(before, after);
            result.markStateChanged("hard_set_health_zero");
            result.add("hard_set_health_zero: " + before + " -> " + after);

            writeVanillaHealthData(victim, 0.0F, result);
            handleServerDeath(victim, source, result, "hard_set_health_zero", true);
        }
        catch (Throwable e) {
            result.add("hard_set_health_zero error: " + e.getClass().getSimpleName());
        }
    }

    private static void forceDeathNow(LivingEntity victim, DamageSource source, Result result) {
        try {
            victim.invulnerableTime = 0;
            victim.hurtTime = 0;
            victim.hurtDuration = 0;

            result.add("force_death_now before: health=" + victim.getHealth()
                       + ", isAlive=" + victim.isAlive()
                       + ", removed=" + victim.isRemoved()
                       + ", deathTime=" + victim.deathTime);

            victim.die(source);
            result.markDeathHandled("force_death_now die(source)");

            LootResolver.dropAllDeathLootVanilla(victim, source);

            result.add("force_death_now after_die: health=" + victim.getHealth()
                       + ", isAlive=" + victim.isAlive()
                       + ", removed=" + victim.isRemoved()
                       + ", deathTime=" + victim.deathTime);
        }
        catch (Throwable e) {
            result.add("force_death_now error: " + e.getClass().getSimpleName());
        }
    }

    private static void forceRemove(LivingEntity victim, Result result) {
        try {
            result.add("force_remove before: health=" + victim.getHealth()
                       + ", isAlive=" + victim.isAlive()
                       + ", removed=" + victim.isRemoved());

            victim.setRemoved(Entity.RemovalReason.KILLED);
            result.markDeathHandled("force_remove setRemoved(KILLED)");

            result.add("force_remove after: health=" + victim.getHealth()
                       + ", isAlive=" + victim.isAlive()
                       + ", removed=" + victim.isRemoved());
        }
        catch (Throwable e) {
            result.add("force_remove error: " + e.getClass().getSimpleName());
        }
    }

    private static void handleServerDeath(LivingEntity victim, DamageSource source, Result result, String reason, boolean prepareLootFallback) {
        if (victim == null)
            return;
        if (victim.getHealth() > 0.0F)
            return;

        result.serverDead(reason
                          + ", health=" + victim.getHealth()
                          + ", isAlive=" + victim.isAlive()
                          + ", removed=" + victim.isRemoved()
                          + ", deathTime=" + victim.deathTime);

        syncVanillaHealthZero(victim, result);

        try {
            victim.invulnerableTime = 0;
            victim.hurtTime = 0;
            victim.hurtDuration = 0;
            result.add("death_sync flags: invulnerableTime=0, hurtTime=0, hurtDuration=0, deathTime=" + victim.deathTime);
        }
        catch (Throwable e) {
            result.add("death_sync flags error: " + e.getClass().getSimpleName());
        }

        if (prepareLootFallback){
            result.prepareLootFallback(victim, source, reason);
        }

        try {
            victim.die(source);
            result.markDeathHandled(reason + ", die(source)");

            LootResolver.dropAllDeathLootVanilla(victim, source);

            result.add("after_die: health=" + victim.getHealth()
                       + ", isAlive=" + victim.isAlive()
                       + ", removed=" + victim.isRemoved()
                       + ", deathTime=" + victim.deathTime);
        }
        catch (Throwable e) {
            result.add("die(source) error: " + e.getClass().getSimpleName());
        }
    }

    private static void syncVanillaHealthZero(LivingEntity victim, Result result) {
        writeVanillaHealthData(victim, 0.0F, result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean writeVanillaHealthData(LivingEntity victim, float value, Result result) {
        try {
            EntityDataAccessor healthAccessor = findLivingHealthAccessor(victim, result);
            if (healthAccessor == null){
                result.add("health_data_write: failed to find LivingEntity health accessor");
                return false;
            }

            Object before = victim.getEntityData().get(healthAccessor);
            victim.getEntityData().set(healthAccessor, value);
            Object after = victim.getEntityData().get(healthAccessor);

            result.add("health_data_write: vanilla health data " + before + " -> " + after);
            result.markStateChanged("vanilla health data write");

            return true;
        }
        catch (Throwable e) {
            result.add("health_data_write error: " + e.getClass().getSimpleName());
            return false;
        }
    }

    private static void clearInvulnerability(Entity entity, LivingEntity victim) {
        if (entity != null)
            entity.invulnerableTime = 0;

        if (victim != null){
            victim.invulnerableTime = 0;
            victim.hurtTime = 0;
            victim.hurtDuration = 0;
        }
    }

    private static List<DataCandidate> collectEntityDataCandidates(LivingEntity victim, boolean absolute, int minScore) {
        List<DataCandidate> result = new ArrayList<>();

        Class<?> cls = victim.getClass();
        while (cls != null && cls != Object.class) {
            if (!absolute && (cls == LivingEntity.class || cls == Entity.class))
                break;

            collectEntityDataFromClass(victim, cls, result, minScore, absolute);
            cls = cls.getSuperclass();
        }

        result.sort(Comparator.comparingInt(DataCandidate::score).reversed());
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static void collectEntityDataFromClass(LivingEntity victim, Class<?> cls, List<DataCandidate> result, int minScore, boolean absolute) {
        for (Field field : cls.getDeclaredFields()) {
            try {
                if (!Modifier.isStatic(field.getModifiers()))
                    continue;
                if (!EntityDataAccessor.class.isAssignableFrom(field.getType()))
                    continue;

                field.setAccessible(true);

                EntityDataAccessor accessor = (EntityDataAccessor) field.get(null);
                Object value = getData(victim, accessor);

                if (!(value instanceof Float) && !(value instanceof Integer))
                    continue;

                DataMode mode = guessMode(field.getName());
                int score = scoreName(field.getName(), value, mode) + 10;

                if (absolute || score >= minScore){
                    result.add(new DataCandidate(field.getName(), accessor, value, score, mode));
                }
            }
            catch (Throwable ignored) {}
        }
    }

    private static List<FieldCandidate> collectFieldCandidates(LivingEntity victim, Result probe, boolean absolute, int minScore) {
        List<FieldCandidate> result = new ArrayList<>();

        Class<?> cls = victim.getClass();
        while (cls != null && cls != Object.class) {
            if (!absolute && (cls == LivingEntity.class || cls == Entity.class))
                break;

            collectFieldsFromClass(victim, cls, probe, result, minScore, absolute);
            cls = cls.getSuperclass();
        }

        result.sort(Comparator.comparingInt(FieldCandidate::score).reversed());
        return result;
    }

    private static void collectFieldsFromClass(LivingEntity victim, Class<?> cls, Result probe, List<FieldCandidate> result, int minScore, boolean absolute) {
        for (Field field : cls.getDeclaredFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                    continue;
                if (!isNumericField(field))
                    continue;

                field.setAccessible(true);

                Object value = field.get(victim);
                DataMode mode = guessMode(field.getName());
                int score = scoreName(field.getName(), value, mode)
                            + probe.scoreObservedField(field)
                            + scoreGetHealthDependency(victim, field, mode);

                if (absolute || score >= minScore){
                    result.add(new FieldCandidate(field, field.getName(), value, score, mode));
                }
            }
            catch (Throwable ignored) {}
        }
    }

    private static int scoreGetHealthDependency(LivingEntity victim, Field field, DataMode mode) {
        try {
            Object beforeValue = field.get(victim);
            float beforeHealth = victim.getHealth();

            Object nudged = nudgeValue(beforeValue, mode == DataMode.REMAINING ? -1.0F : 1.0F);
            if (nudged == null)
                return 0;

            field.set(victim, nudged);
            float afterHealth = victim.getHealth();
            field.set(victim, beforeValue);

            if (Math.abs(afterHealth - beforeHealth) > DAMAGE_EPS)
                return 45;
        }
        catch (Throwable ignored) {}

        return 0;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static EntityDataMove tryMoveEntityData(LivingEntity victim, DataCandidate candidate, float amount, Result result) {
        try {
            Object before = getData(victim, candidate.accessor());
            Object after = nextValue(before, amount, candidate.mode());

            if (after == null)
                return EntityDataMove.failed();

            victim.getEntityData().set((EntityDataAccessor) candidate.accessor(), after);

            Object confirmed = getData(victim, candidate.accessor());

            boolean moved = movedAsDamage(before, confirmed, candidate.mode());
            float dealt = moved ? dealtEquivalent(before, confirmed, candidate.mode()) : 0.0F;

            result.add("entity_data " + candidate.mode()
                       + " " + candidate.name()
                       + ": " + before + " -> " + confirmed
                       + ", score=" + candidate.score()
                       + ", dealt=" + dealt);

            return new EntityDataMove(moved, dealt);
        }
        catch (Throwable e) {
            result.add("entity_data " + candidate.name() + " error: " + e.getClass().getSimpleName());
            return EntityDataMove.failed();
        }
    }

    private static FieldMove tryMoveField(LivingEntity victim, FieldCandidate candidate, float amount, Result result) {
        try {
            Object before = candidate.field().get(victim);
            Object after = nextValue(before, amount, candidate.mode());

            if (after == null)
                return FieldMove.failed();

            candidate.field().set(victim, after);

            Object confirmed = candidate.field().get(victim);

            boolean moved = movedAsDamage(before, confirmed, candidate.mode());
            float dealt = moved ? dealtEquivalent(before, confirmed, candidate.mode()) : 0.0F;

            result.add("private_field " + candidate.mode()
                       + " " + candidate.field().getDeclaringClass().getSimpleName()
                       + "#" + candidate.name()
                       + ": " + before + " -> " + confirmed
                       + ", score=" + candidate.score()
                       + ", dealt=" + dealt);

            return new FieldMove(moved, dealt);
        }
        catch (Throwable e) {
            result.add("private_field " + candidate.name() + " error: " + e.getClass().getSimpleName());
            return FieldMove.failed();
        }
    }

    @SuppressWarnings("rawtypes")
    private static Object getData(LivingEntity victim, EntityDataAccessor accessor) {
        try {
            return victim.getEntityData().get(accessor);
        }
        catch (Throwable e) {
            return null;
        }
    }

    private static Object nextValue(Object before, float amount, DataMode mode) {
        if (before instanceof Float f){
            return Float.isFinite(f)
                   ? (mode == DataMode.REMAINING ? Math.max(0.0F, f - amount) : Math.min(100000.0F, f + amount))
                   : null;
        }

        if (before instanceof Double d){
            return Double.isFinite(d)
                   ? (mode == DataMode.REMAINING ? Math.max(0.0D, d - amount) : Math.min(100000.0D, d + amount))
                   : null;
        }

        if (before instanceof Integer i){
            int delta = Math.max(1, (int) Math.ceil(amount));
            return mode == DataMode.REMAINING ? Math.max(0, i - delta) : Math.min(100000, i + delta);
        }

        if (before instanceof Long l){
            long delta = Math.max(1L, (long) Math.ceil(amount));
            return mode == DataMode.REMAINING ? Math.max(0L, l - delta) : Math.min(100000L, l + delta);
        }

        return null;
    }

    private static Object nudgeValue(Object before, float delta) {
        if (before instanceof Float f)
            return Float.isFinite(f) ? f + delta : null;
        if (before instanceof Double d)
            return Double.isFinite(d) ? d + delta : null;
        if (before instanceof Integer i)
            return i + (delta < 0 ? -1 : 1);
        if (before instanceof Long l)
            return l + (delta < 0 ? -1L : 1L);

        return null;
    }

    private static Object highValue(Object before) {
        if (before instanceof Float)
            return 1000000.0F;
        if (before instanceof Double)
            return 1000000.0D;
        if (before instanceof Integer)
            return 1000000;
        if (before instanceof Long)
            return 1000000L;

        return null;
    }

    private static Object zeroValue(Object before) {
        if (before instanceof Float)
            return 0.0F;
        if (before instanceof Double)
            return 0.0D;
        if (before instanceof Integer)
            return 0;
        if (before instanceof Long)
            return 0L;

        return null;
    }

    private static boolean movedAsDamage(Object before, Object after, DataMode mode) {
        double b = asDouble(before);
        double a = asDouble(after);

        if (!Double.isFinite(b) || !Double.isFinite(a))
            return false;

        return mode == DataMode.REMAINING ? a < b : a > b;
    }

    private static float dealtEquivalent(Object before, Object after, DataMode mode) {
        double b = asDouble(before);
        double a = asDouble(after);

        if (!Double.isFinite(b) || !Double.isFinite(a))
            return 0.0F;

        return (float) Math.max(0.0D, mode == DataMode.REMAINING ? b - a : a - b);
    }

    private static double asDouble(Object value) {
        if (value instanceof Number n)
            return n.doubleValue();
        return Double.NaN;
    }

    private static boolean isNumericField(Field field) {
        Class<?> type = field.getType();

        return type == float.class
               || type == Float.class
               || type == double.class
               || type == Double.class
               || type == int.class
               || type == Integer.class
               || type == long.class
               || type == Long.class;
    }

    private static DataMode guessMode(String rawName) {
        String name = rawName.toUpperCase(Locale.ROOT);

        if (containsAny(name,
                        "TOTALDAMAGETAKEN", "TOTAL_DAMAGE_TAKEN",
                        "DAMAGETAKEN", "DAMAGE_TAKEN",
                        "TAKENDAMAGE", "TAKEN_DAMAGE",
                        "HURT_TAKEN", "WOUND_TAKEN", "INJURY_TAKEN",
                        "DAMAGE", "HURT", "WOUND", "INJURY", "DMG")){
            return DataMode.TAKEN;
        }

        return DataMode.REMAINING;
    }

    private static int scoreName(String rawName, Object value, DataMode mode) {
        String name = rawName.toUpperCase(Locale.ROOT);
        int score = value instanceof Float || value instanceof Double ? 20 : 12;

        if (mode == DataMode.TAKEN && containsAny(name,
                                                  "TOTALDAMAGETAKEN", "TOTAL_DAMAGE_TAKEN",
                                                  "DAMAGETAKEN", "DAMAGE_TAKEN",
                                                  "TAKENDAMAGE", "TAKEN_DAMAGE")){
            score += 120;
        }

        if (mode == DataMode.REMAINING && containsAny(name, "HEALTH", "HP", "LIFE", "VITAL")){
            score += 75;
        }

        if (mode == DataMode.REMAINING && containsAny(name, "SHIELD", "BARRIER", "CORE", "PART")){
            score += 35;
        }

        if (mode == DataMode.TAKEN && containsAny(name, "HURT_TAKEN", "WOUND_TAKEN", "INJURY_TAKEN")){
            score += 75;
        }

        if (mode == DataMode.TAKEN && containsAny(name, "DAMAGE", "HURT", "WOUND", "INJURY", "DMG")){
            score += 35;
        }

        if (containsAny(name,
                        "ANIM", "TIMER", "TIME", "POSE", "FLAGS", "FLAG",
                        "STATE", "MODE", "VARIANT", "TYPE", "ID",
                        "PROGRESS", "LAST", "CLIENT", "TARGET", "ATTACK")){
            score -= 60;
        }

        if (containsAny(name, "COOLDOWN", "LOCK")){
            score -= 40;
        }

        if (containsAny(name, "MAX", "CAP", "LIMIT", "MULTIPLIER")){
            score -= 35;
        }

        double d = asDouble(value);
        if (Double.isFinite(d) && d >= 0.0D && d <= 100000.0D)
            score += 8;
        else
            score -= 30;

        return score;
    }

    private static boolean containsAny(String s, String... keys) {
        for (String key : keys) {
            if (s.contains(key))
                return true;
        }

        return false;
    }

    @SuppressWarnings("rawtypes")
    private static EntityDataAccessor findLivingHealthAccessor(LivingEntity victim, Result result) {
        EntityDataAccessor byName = findLivingHealthAccessorByName(victim, result);
        if (byName != null)
            return byName;

        return findLivingHealthAccessorByFloatType(victim, result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static EntityDataAccessor findLivingHealthAccessorByName(LivingEntity victim, Result result) {
        Class<LivingEntity> cls = LivingEntity.class;

        for (Field field : cls.getDeclaredFields()) {
            try {
                if (!Modifier.isStatic(field.getModifiers()))
                    continue;
                if (!EntityDataAccessor.class.isAssignableFrom(field.getType()))
                    continue;

                String name = field.getName();
                if (!name.equals("DATA_HEALTH_ID") && !name.equals("f_20961_"))
                    continue;

                field.setAccessible(true);

                EntityDataAccessor accessor = (EntityDataAccessor) field.get(null);
                Object value = victim.getEntityData().get(accessor);

                if (value instanceof Float){
                    result.add("health_accessor: found by name=" + name + ", value=" + value);
                    return accessor;
                }

                result.add("health_accessor: named accessor " + name + " had non-float value=" + value);
            }
            catch (Throwable e) {
                result.add("health_accessor: named accessor error field="
                           + field.getName()
                           + ", error=" + e.getClass().getSimpleName());
            }
        }

        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static EntityDataAccessor findLivingHealthAccessorByFloatType(LivingEntity victim, Result result) {
        Class<LivingEntity> cls = LivingEntity.class;

        for (Field field : cls.getDeclaredFields()) {
            try {
                if (!Modifier.isStatic(field.getModifiers()))
                    continue;
                if (!EntityDataAccessor.class.isAssignableFrom(field.getType()))
                    continue;

                field.setAccessible(true);

                EntityDataAccessor accessor = (EntityDataAccessor) field.get(null);
                Object value;

                try {
                    value = victim.getEntityData().get(accessor);
                }
                catch (Throwable ignored) {
                    continue;
                }

                if (value instanceof Float f && Float.isFinite(f)){
                    result.add("health_accessor: fallback float EntityDataAccessor field=" + field.getName() + ", value=" + f);
                    return accessor;
                }
            }
            catch (Throwable e) {
                result.add("health_accessor: fallback accessor error field="
                           + field.getName()
                           + ", error=" + e.getClass().getSimpleName());
            }
        }

        return null;
    }

    private static String describeEntity(Entity entity) {
        if (entity == null)
            return "null";

        return entity.getClass().getName()
               + ", type=" + entity.getType()
               + ", uuid=" + entity.getUUID()
               + ", removed=" + entity.isRemoved();
    }

    private static PlanStep damageStep(String name, StepRunner runner) {
        return new PlanStep(name, StepKind.DAMAGE, runner);
    }

    private static PlanStep supportStep(String name, StepRunner runner) {
        return new PlanStep(name, StepKind.SUPPORT, runner);
    }

    private enum ForceLevel {
        MEDIUM,
        FINAL
    }

    private enum StepKind {
        DAMAGE,
        SUPPORT
    }

    private enum DataMode {
        REMAINING,
        TAKEN
    }

    @FunctionalInterface
    private interface StepRunner {
        boolean run(float amount, Result result);
    }

    private record PlanStep(String name, StepKind kind, StepRunner runner) {}

    private record StepResult(boolean progress, float dealt) {
        private static StepResult noProgress() {
            return new StepResult(false, 0.0F);
        }
    }

    private record RetryCandidate(PlanStep step, float dealtPerOp) {
        private int estimatedOps(float remaining) {
            if (dealtPerOp <= DAMAGE_EPS)
                return Integer.MAX_VALUE;
            return Math.max(1, (int) Math.ceil(remaining / dealtPerOp));
        }
    }

    private record DataCandidate(String name, EntityDataAccessor<?> accessor, Object value, int score, DataMode mode) {}

    private record FieldCandidate(Field field, String name, Object value, int score, DataMode mode) {}

    private record EntityDataMove(boolean moved, float dealtEquivalent) {
        private static EntityDataMove failed() {
            return new EntityDataMove(false, 0.0F);
        }
    }

    private record FieldMove(boolean moved, float dealtEquivalent) {
        private static FieldMove failed() {
            return new FieldMove(false, 0.0F);
        }
    }

    public static final class Result {
        private final Entity entity;
        private final LivingEntity victim;
        private final DamageSource source;
        private final float amount;
        private final ForceLevel forceLevel;
        private final float initialHealth;

        private final List<String> lines = new ArrayList<>();
        private final Map<String, FieldSnapshot> fieldSnapshot = new HashMap<>();
        private final Map<String, Integer> observedFieldScores = new HashMap<>();

        private boolean success;
        private boolean serverDead;
        private boolean deathHandled;
        private boolean lootFallbackPrepared;

        private boolean finalRequested;
        private boolean lethalRequested;
        private boolean pipelineEntered;
        private boolean stateChanged;

        private LivingEntity lootFallbackEntity;
        private DamageSource lootFallbackSource;
        private String lootFallbackReason = "";

        private String strategy = "none";
        private float totalDealt;

        private Result(@Nullable Entity entity, @Nullable LivingEntity victim, DamageSource source, float amount, ForceLevel forceLevel) {
            this.entity = entity;
            this.victim = victim;
            this.source = source;
            this.amount = amount;
            this.forceLevel = forceLevel;
            this.initialHealth = victim == null ? 0.0F : victim.getHealth();

            add("created: forceLevel=" + forceLevel
                + ", entity=" + describeEntity(entity)
                + ", victim=" + describeEntity(victim)
                + ", amount=" + amount
                + ", initialHealth=" + initialHealth);

            snapshotFields();
        }

        private static String fieldKey(Field field) {
            return field.getDeclaringClass().getName() + "#" + field.getName();
        }

        private void addHeader(String name) {
            add("========== " + name + " ==========");
        }

        private Result success(String strategy) {
            this.success = true;
            this.strategy = strategy;
            add("success: " + strategy);
            return this;
        }

        private void clearSuccess(String reason) {
            if (success){
                add("clear_success: " + reason + ", previousStrategy=" + strategy);
            }

            this.success = false;
            this.strategy = "none";
        }

        private Result fail(String reason) {
            add("failed: " + reason);
            return this;
        }

        private Result serverDead(String reason) {
            this.serverDead = true;
            add("server_dead: " + reason);
            return this;
        }

        private void markDeathHandled(String reason) {
            this.serverDead = true;
            this.deathHandled = true;
            add("death_handled: " + reason);
        }

        private void prepareLootFallback(LivingEntity victim, DamageSource source, String reason) {
            if (lootFallbackPrepared)
                return;

            this.lootFallbackPrepared = true;
            this.lootFallbackEntity = victim;
            this.lootFallbackSource = source;
            this.lootFallbackReason = reason;

            add("loot_fallback_prepared: entity=" + victim.getType() + ", reason=" + reason);
        }

        private void markFinalRequested(boolean lethalRequested) {
            this.finalRequested = true;
            this.lethalRequested = lethalRequested;
            add("final_requested: lethal=" + lethalRequested);
        }

        private void markPipelineEntered(String reason) {
            this.pipelineEntered = true;
            add("pipeline_entered: " + reason);
        }

        private void markStateChanged(String reason) {
            this.stateChanged = true;
            add("state_changed: " + reason);
        }

        private void add(String line) {
            lines.add(line);
        }

        private float health() {
            return victim == null ? -1.0F : victim.getHealth();
        }

        private float absorption() {
            return victim == null ? 0.0F : victim.getAbsorptionAmount();
        }

        private void recordHealthChange(float before, float after) {
            if (before > after){
                float dealt = before - after;
                totalDealt += dealt;
                markStateChanged("health changed " + before + " -> " + after + ", dealt=" + dealt);
            }
        }

        private void recordDamageLikeChange(float beforeHealth, float afterHealth, float beforeAbsorption, float afterAbsorption) {
            recordHealthChange(beforeHealth, afterHealth);

            if (beforeAbsorption > afterAbsorption){
                float dealt = beforeAbsorption - afterAbsorption;
                totalDealt += dealt;
                markStateChanged("absorption changed " + beforeAbsorption + " -> " + afterAbsorption + ", dealt=" + dealt);
            }
        }

        private void recordSyntheticDamage(float amount) {
            if (amount > 0.0F){
                totalDealt += amount;
                markStateChanged("synthetic damage=" + amount);
            }
        }

        private void snapshotFields() {
            fieldSnapshot.clear();

            if (victim == null)
                return;

            Class<?> cls = victim.getClass();
            while (cls != null && cls != LivingEntity.class && cls != Entity.class && cls != Object.class) {
                for (Field field : cls.getDeclaredFields()) {
                    snapshotField(field);
                }

                cls = cls.getSuperclass();
            }
        }

        private void snapshotField(Field field) {
            try {
                if (Modifier.isStatic(field.getModifiers()))
                    return;
                if (!isNumericField(field))
                    return;

                field.setAccessible(true);

                Object value = field.get(victim);
                fieldSnapshot.put(fieldKey(field), new FieldSnapshot(field, value));
            }
            catch (Throwable ignored) {}
        }

        private void recordFieldChanges(String sourceName) {
            if (victim == null)
                return;

            float health = health();

            for (FieldSnapshot snapshot : fieldSnapshot.values()) {
                try {
                    Object now = snapshot.field().get(victim);

                    double before = asDouble(snapshot.value());
                    double after = asDouble(now);

                    if (!Double.isFinite(before) || !Double.isFinite(after))
                        continue;
                    if (Math.abs(after - before) <= DAMAGE_EPS)
                        continue;

                    int score = after > before ? 30 : 20;
                    observedFieldScores.merge(fieldKey(snapshot.field()), score, Integer::sum);

                    markStateChanged("observed field changed by " + sourceName + ": " + snapshot.field().getName());
                    add("observed_field_change by " + sourceName
                        + ": " + snapshot.field().getDeclaringClass().getSimpleName()
                        + "#" + snapshot.field().getName()
                        + " " + before + " -> " + after
                        + ", healthNow=" + health);
                }
                catch (Throwable ignored) {}
            }

            snapshotFields();
        }

        private int scoreObservedField(Field field) {
            return observedFieldScores.getOrDefault(fieldKey(field), 0);
        }

        public boolean reachedExpectedDamage() {
            return serverDead
                   || totalDealt + DAMAGE_EPS >= amount * DAMAGE_TOLERANCE
                   || victim != null && !victim.isAlive();
        }

        public boolean instantEffective() {
            return serverDead()
                   || totalDealt() > DAMAGE_EPS
                   || pipelineEntered
                   || stateChanged
                   || victim != null && (!victim.isAlive() || victim.isRemoved());
        }

        public boolean killConfirmed() {
            return victim == null
                   || victim.isRemoved()
                   || !victim.isAlive()
                   || victim.getHealth() <= DAMAGE_EPS
                   || serverDead();
        }

        public float remainingAmountForStep() {
            if (killConfirmed())
                return 0.0F;
            return remainingAmount();
        }

        public float remainingAmount() {
            return reachedExpectedDamage() ? 0.0F : Math.max(0.0F, amount - totalDealt);
        }

        public float initialHealth() {
            return initialHealth;
        }

        public float totalDealt() {
            return totalDealt;
        }

        public boolean success() {
            return success;
        }

        public boolean serverDead() {
            return serverDead;
        }

        public boolean deathHandled() {
            return deathHandled;
        }

        public boolean lootFallbackPrepared() {
            return lootFallbackPrepared;
        }

        public LivingEntity lootFallbackEntity() {
            return lootFallbackEntity;
        }

        public DamageSource lootFallbackSource() {
            return lootFallbackSource;
        }

        public String lootFallbackReason() {
            return lootFallbackReason;
        }

        public DamageSource source() {
            return source;
        }

        public String strategy() {
            return strategy;
        }

        public List<String> lines() {
            return List.copyOf(lines);
        }

        public String compactDebugText() {
            return "DamageProbe.Result{"
                   + "forceLevel=" + forceLevel
                   + ", amount=" + amount
                   + ", initialHealth=" + initialHealth
                   + ", currentHealth=" + health()
                   + ", totalDealt=" + totalDealt
                   + ", remaining=" + remainingAmount()
                   + ", success=" + success
                   + ", serverDead=" + serverDead
                   + ", deathHandled=" + deathHandled
                   + ", finalRequested=" + finalRequested
                   + ", lethalRequested=" + lethalRequested
                   + ", pipelineEntered=" + pipelineEntered
                   + ", stateChanged=" + stateChanged
                   + ", instantEffective=" + instantEffective()
                   + ", killConfirmed=" + killConfirmed()
                   + ", strategy=" + strategy
                   + "}";
        }

        public String debugText() {
            String entityName = entity == null ? "null" : String.valueOf(entity.getType());
            String victimName = victim == null ? "null" : String.valueOf(victim.getType());

            return "DamageProbe{"
                   + "entity=" + entityName
                   + ", victim=" + victimName
                   + ", forceLevel=" + forceLevel
                   + ", amount=" + amount
                   + ", initialHealth=" + initialHealth
                   + ", currentHealth=" + health()
                   + ", totalDealt=" + totalDealt
                   + ", remaining=" + remainingAmount()
                   + ", success=" + success
                   + ", serverDead=" + serverDead
                   + ", deathHandled=" + deathHandled
                   + ", finalRequested=" + finalRequested
                   + ", lethalRequested=" + lethalRequested
                   + ", pipelineEntered=" + pipelineEntered
                   + ", stateChanged=" + stateChanged
                   + ", instantEffective=" + instantEffective()
                   + ", killConfirmed=" + killConfirmed()
                   + ", lootFallbackPrepared=" + lootFallbackPrepared
                   + ", lootFallbackReason=" + lootFallbackReason
                   + ", strategy=" + strategy
                   + ", steps=" + lines
                   + "}";
        }

        private record FieldSnapshot(Field field, Object value) {}
    }
}