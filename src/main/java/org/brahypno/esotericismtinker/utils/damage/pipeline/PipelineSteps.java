package org.brahypno.esotericismtinker.utils.damage.pipeline;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageConstants;
import org.brahypno.esotericismtinker.utils.damage.DamageContext;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;
import org.brahypno.esotericismtinker.utils.damage.linked.LinkedTargetFinder;
import org.brahypno.esotericismtinker.utils.damage.linked.LinkedTargetKillSupport;
import org.brahypno.esotericismtinker.utils.damage.method.GuardStateSupport;
import org.brahypno.esotericismtinker.utils.damage.method.MethodHealthProbe;
import org.brahypno.esotericismtinker.utils.damage.method.MixinMethodInspector;
import org.brahypno.esotericismtinker.utils.damage.profile.DamageProfile;
import org.brahypno.esotericismtinker.utils.damage.profile.DamageProfileCache;
import org.brahypno.esotericismtinker.utils.damage.profile.ProbeDirective;
import org.brahypno.esotericismtinker.utils.damage.profile.ProbeStepId;
import org.brahypno.esotericismtinker.utils.damage.profile.ProfileGuidedExplorer;
import org.brahypno.esotericismtinker.utils.damage.reflect.DamageMethodInvoker;
import org.brahypno.esotericismtinker.utils.damage.reflect.MethodInvokeResult;
import org.brahypno.esotericismtinker.utils.damage.scan.DataCandidate;
import org.brahypno.esotericismtinker.utils.damage.scan.EntityDataMove;
import org.brahypno.esotericismtinker.utils.damage.scan.EntityDataScanner;
import org.brahypno.esotericismtinker.utils.damage.scan.FieldCandidate;
import org.brahypno.esotericismtinker.utils.damage.scan.FieldMove;
import org.brahypno.esotericismtinker.utils.damage.scan.FieldScanner;
import org.brahypno.esotericismtinker.utils.damage.state.DamageSnapshot;
import org.brahypno.esotericismtinker.utils.damage.state.NbtMutationProbe;
import org.brahypno.esotericismtinker.utils.damage.state.NbtStateDiff;
import org.brahypno.esotericismtinker.utils.damage.step.KillPathKind;
import org.brahypno.esotericismtinker.utils.damage.step.StepResult;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class PipelineSteps {
  private PipelineSteps() {}

  static void profileHeader(DamageContext context, DamageProbeResult result) {
    ProfileGuidedExplorer.addProfileHeader(context, result);
  }

  static StepResult tryLearnedHealthFieldKill(DamageContext context, DamageProbeResult result) {
    return ProfileGuidedExplorer.tryLearnedHealthFieldKill(context, result);
  }

  static StepResult tryLearnedCapFieldBypass(DamageContext context, DamageProbeResult result) {
    return ProfileGuidedExplorer.tryLearnedCapFieldBypass(context, result);
  }

  static int exploreHealthBackingFields(DamageContext context, DamageProbeResult result) {
    return ProfileGuidedExplorer.exploreHealthBackingFields(context, result);
  }

  static int exploreCapSource(DamageContext context, DamageProbeResult result) {
    int hits = ProfileGuidedExplorer.exploreCapSource(context, result);
    MixinMethodInspector.inspect(context.victim(), result);
    return hits;
  }

  static StepResult nbtMutation(DamageContext context, DamageProbeResult result, String prefix) {
    return NbtMutationProbe.tryNbtMutation(context, result, prefix);
  }

  static StepResult basicHandler(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) {
      result.add("basic_handler skipped: victim=null");
      return StepResult.noProgress();
    }
    if (ProfileGuidedExplorer.shouldSkip(context, result, ProbeStepId.BASIC_HANDLER)) return StepResult.noProgress();

    DamageSnapshot before = DamageSnapshot.of(victim);
    NbtStateDiff.Snapshot beforeNbt = NbtStateDiff.capture(victim, result, "basic_handler_before");
    clearInvulnerability(context.entity(), victim);
    GuardStateSupport.clearDamageGuards(victim, result, "basic_handler");

    boolean returned = false;
    try {
      returned = MediumDamagePipeline.damageHandler(context.entity(), context.source(), context.amount());
    } catch (Throwable e) {
      result.add("basic_handler error: " + e.getClass().getSimpleName());
    }

    DamageSnapshot afterBasic = DamageSnapshot.of(victim);
    NbtStateDiff.Snapshot afterBasicNbt = NbtStateDiff.capture(victim, result, "basic_handler_after_basic");
    if (returned) result.markPipelineEntered("basic_handler returned true");
    result.recordDamageLikeChange(before, afterBasic);
    float basicDealt = positiveDelta(before.health(), afterBasic.health()) + positiveDelta(before.absorption(), afterBasic.absorption());
    if (basicDealt > DamageConstants.DAMAGE_EPS || afterBasic.authoritativeChangeFrom(before)) {
      NbtStateDiff.logDiff("basic_handler", beforeNbt, afterBasicNbt, basicDealt, result);
    }
    result.add("basic_handler: returned=" + returned
        + ", health " + before.health() + " -> " + afterBasic.health()
        + ", absorption " + before.absorption() + " -> " + afterBasic.absorption());
    handleServerDeath(victim, context.source(), result, "basic_handler");

    boolean progress = afterBasic.authoritativeChangeFrom(before) || result.killConfirmed();
    DamageSnapshot after = afterBasic;

    if (!progress && context.options().mediumActuallyHurtFallback()) {
      result.add("basic_handler: living hurt path failed; trying actuallyHurt fallback inside BASIC_HANDLER");
      MethodInvokeResult invokeResult = DamageMethodInvoker.invokeActuallyHurt(victim, context.source(), context.remainingOrAmount(result));
      for (String line : invokeResult.lines()) result.add("basic_handler_actually_hurt: " + line);
      DamageSnapshot afterActual = DamageSnapshot.of(victim);
      NbtStateDiff.Snapshot afterActualNbt = NbtStateDiff.capture(victim, result, "basic_handler_after_actual");
      if (invokeResult.invoked()) result.markPipelineEntered("basic_handler actuallyHurt invoked");
      result.recordDamageLikeChange(afterBasic, afterActual);
      float actualDealt = positiveDelta(afterBasic.health(), afterActual.health()) + positiveDelta(afterBasic.absorption(), afterActual.absorption());
      if (actualDealt > DamageConstants.DAMAGE_EPS || afterActual.authoritativeChangeFrom(afterBasic)) {
        NbtStateDiff.logDiff("basic_handler_actually_hurt", afterBasicNbt, afterActualNbt, actualDealt, result);
      }
      handleServerDeath(victim, context.source(), result, "basic_handler_actually_hurt");
      progress = afterActual.authoritativeChangeFrom(afterBasic) || result.killConfirmed();
      after = afterActual;
    }

    float dealt = positiveDelta(before.health(), after.health()) + positiveDelta(before.absorption(), after.absorption());
    if (progress && dealt > DamageConstants.DAMAGE_EPS) result.markKillPath(KillPathKind.DAMAGE_PIPELINE, "basic handler hit chain");
    return ProfileGuidedExplorer.record(context, ProbeStepId.BASIC_HANDLER, new StepResult(progress, dealt, progress ? KillPathKind.DAMAGE_PIPELINE : KillPathKind.NONE), before, after, result);
  }

  static StepResult rawSetHealth(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    if (ProfileGuidedExplorer.shouldSkip(context, result, ProbeStepId.RAW_SET_HEALTH)) return StepResult.noProgress();

    clearInvulnerability(victim, victim);
    GuardStateSupport.clearDamageGuards(victim, result, "raw_set_health");
    DamageSnapshot before = DamageSnapshot.of(victim);
    float requested = context.remainingOrAmount(result);
    float target = Math.max(0.0F, before.health() - requested);

    try {
      victim.setHealth(target);
    } catch (Throwable e) {
      result.add("raw_set_health error: " + e.getClass().getSimpleName());
      DamageSnapshot afterError = DamageSnapshot.of(victim);
      ProfileGuidedExplorer.record(context, ProbeStepId.RAW_SET_HEALTH, StepResult.noProgress(), before, afterError, result);
      return StepResult.noProgress();
    }

    DamageSnapshot afterRaw = DamageSnapshot.of(victim);
    result.recordDamageLikeChange(before, afterRaw);
    result.add("raw_set_health: health " + before.health() + " -> " + afterRaw.health() + ", target=" + target);
    handleServerDeath(victim, context.source(), result, "raw_set_health");

    float rawDealt = positiveDelta(before.health(), afterRaw.health());
    boolean rawAuthoritative = afterRaw.authoritativeChangeFrom(before) || result.killConfirmed();
    boolean shouldFallback = !result.killConfirmed()
        && context.options().setHealthFallbackMode().shouldFallback(requested, rawDealt, rawAuthoritative);

    DamageSnapshot after = afterRaw;
    float totalDealt = rawDealt;
    boolean progress = rawAuthoritative;

    if (shouldFallback) {
      result.add("raw_set_health fallback: mode=" + context.options().setHealthFallbackMode()
          + ", requested=" + requested
          + ", rawDealt=" + rawDealt
          + ", trying true/special LivingEntity#setHealth");
      MethodInvokeResult invokeResult = DamageMethodInvoker.invokeLivingEntitySetHealthSpecial(victim, target);
      for (String line : invokeResult.lines()) result.add("raw_set_health_true_fallback: " + line);
      DamageSnapshot afterSpecial = DamageSnapshot.of(victim);
      result.recordDamageLikeChange(afterRaw, afterSpecial);
      handleServerDeath(victim, context.source(), result, "raw_set_health_true_fallback");
      float specialDealt = positiveDelta(afterRaw.health(), afterSpecial.health());
      totalDealt += specialDealt;
      progress |= afterSpecial.authoritativeChangeFrom(afterRaw) || result.killConfirmed();
      after = afterSpecial;
      result.add("raw_set_health fallback result: health " + afterRaw.health() + " -> " + afterSpecial.health()
          + ", specialDealt=" + specialDealt
          + ", progress=" + progress);
    }

    if (progress && totalDealt > DamageConstants.DAMAGE_EPS) result.markKillPath(KillPathKind.SURFACE_HEALTH, "raw setHealth equivalent");
    return ProfileGuidedExplorer.record(context, ProbeStepId.RAW_SET_HEALTH, new StepResult(progress, totalDealt, progress ? KillPathKind.SURFACE_HEALTH : KillPathKind.NONE), before, after, result);
  }

  static StepResult superSetHealth(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    if (ProfileGuidedExplorer.shouldSkip(context, result, ProbeStepId.SUPER_SET_HEALTH)) return StepResult.noProgress();

    clearInvulnerability(victim, victim);
    GuardStateSupport.clearDamageGuards(victim, result, "final_super_set_health");
    DamageSnapshot before = DamageSnapshot.of(victim);
    float target = Math.max(0.0F, before.health() - context.remainingOrAmount(result));
    MethodInvokeResult invokeResult = DamageMethodInvoker.invokeLivingEntitySetHealthSpecial(victim, target);
    for (String line : invokeResult.lines()) result.add("final_super_set_health: " + line);
    DamageSnapshot after = DamageSnapshot.of(victim);
    result.recordDamageLikeChange(before, after);
    handleServerDeath(victim, context.source(), result, "final_super_set_health");
    float dealt = positiveDelta(before.health(), after.health());
    boolean progress = after.authoritativeChangeFrom(before) || result.killConfirmed() || dealt > DamageConstants.DAMAGE_EPS;
    if (progress && dealt > DamageConstants.DAMAGE_EPS) result.markKillPath(KillPathKind.SURFACE_HEALTH, "super setHealth equivalent");
    return ProfileGuidedExplorer.record(context, ProbeStepId.SUPER_SET_HEALTH, new StepResult(progress, dealt, progress ? KillPathKind.SURFACE_HEALTH : KillPathKind.NONE), before, after, result);
  }

  static StepResult entityDataUnsafe(DamageContext context, DamageProbeResult result, boolean verified) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    ProbeStepId id = verified ? ProbeStepId.ENTITY_DATA_ABSOLUTE : ProbeStepId.ENTITY_DATA_UNSAFE;
    if (ProfileGuidedExplorer.shouldSkip(context, result, id)) return StepResult.noProgress();

    List<DataCandidate> candidates = EntityDataScanner.collect(victim, verified, verified ? Integer.MIN_VALUE : DamageConstants.MIN_ENTITY_DATA_SCORE);
    result.add((verified ? "absolute_entity_data_verified" : "entity_data")
        + " candidates=" + candidates.size()
        + ", input=" + context.remainingOrAmount(result));

    int tried = 0;
    for (DataCandidate candidate : candidates) {
      if (verified && ++tried > context.options().maxAbsoluteDataTries()) break;

      Object oldValue = readEntityDataValue(victim, candidate, result, verified);
      DamageSnapshot before = DamageSnapshot.of(victim);
      EntityDataMove move = EntityDataScanner.move(victim, candidate, context.remainingOrAmount(result), result);
      if (!move.moved()) {
        if (verified) rollbackEntityData(victim, candidate, oldValue, result);
        continue;
      }

      DamageSnapshot after = DamageSnapshot.of(victim);
      boolean authoritative = after.authoritativeChangeFrom(before) || result.killConfirmed();
      result.add((verified ? "absolute_entity_data_verified" : "entity_data") + " check: candidate=" + candidate.name()
          + ", movedDealt=" + move.dealtEquivalent()
          + ", health " + before.health() + " -> " + after.health()
          + ", absorption " + before.absorption() + " -> " + after.absorption()
          + ", deathTime " + before.deathTime() + " -> " + after.deathTime()
          + ", alive " + before.alive() + " -> " + after.alive()
          + ", removed " + before.removed() + " -> " + after.removed()
          + ", authoritative=" + authoritative);

      if (verified && !authoritative) {
        rollbackEntityData(victim, candidate, oldValue, result);
        continue;
      }

      result.recordDamageLikeChange(before, after);
      if (!verified && authoritative && move.dealtEquivalent() > 0.0F)
        result.recordSyntheticDamage(move.dealtEquivalent(), "entity_data " + candidate.name());
      if (authoritative && move.dealtEquivalent() > 0.0F) result.markKillPath(KillPathKind.SURFACE_HEALTH, "entity data health-equivalent move " + candidate.name());
      handleServerDeath(victim, context.source(), result, verified ? "absolute_entity_data_verified" : "entity_data");
      return ProfileGuidedExplorer.record(
          context,
          id,
          new StepResult(authoritative, authoritative ? move.dealtEquivalent() : 0.0F, authoritative ? KillPathKind.SURFACE_HEALTH : KillPathKind.NONE),
          before,
          after,
          result
      );
    }

    DamageSnapshot snapshot = DamageSnapshot.of(victim);
    ProfileGuidedExplorer.record(context, id, StepResult.noProgress(), snapshot, snapshot, result);
    return StepResult.noProgress();
  }

  static StepResult privateFieldUnsafe(DamageContext context, DamageProbeResult result, boolean verified) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    ProbeStepId id = verified ? ProbeStepId.PRIVATE_FIELD_ABSOLUTE : ProbeStepId.PRIVATE_FIELD_UNSAFE;
    if (ProfileGuidedExplorer.shouldSkip(context, result, id)) return StepResult.noProgress();

    List<FieldCandidate> candidates = FieldScanner.collect(victim, verified, verified ? Integer.MIN_VALUE : DamageConstants.MIN_FIELD_SCORE);
    result.add((verified ? "absolute_private_field_verified" : "private_field")
        + " candidates=" + candidates.size()
        + ", input=" + context.remainingOrAmount(result));

    int tried = 0;
    for (FieldCandidate candidate : candidates) {
      if (verified && ++tried > context.options().maxAbsoluteFieldTries()) break;

      Object oldValue = readFieldValue(victim, candidate, result, verified);
      DamageSnapshot before = DamageSnapshot.of(victim);
      FieldMove move = FieldScanner.move(victim, candidate, context.remainingOrAmount(result), result);
      if (!move.moved()) {
        if (verified) rollbackField(victim, candidate, oldValue, result);
        continue;
      }

      DamageSnapshot after = DamageSnapshot.of(victim);
      boolean authoritative = after.authoritativeChangeFrom(before) || result.killConfirmed();
      result.add((verified ? "absolute_private_field_verified" : "private_field") + " check: candidate="
          + candidate.field().getDeclaringClass().getSimpleName() + "#" + candidate.name()
          + ", movedDealt=" + move.dealtEquivalent()
          + ", health " + before.health() + " -> " + after.health()
          + ", absorption " + before.absorption() + " -> " + after.absorption()
          + ", deathTime " + before.deathTime() + " -> " + after.deathTime()
          + ", alive " + before.alive() + " -> " + after.alive()
          + ", removed " + before.removed() + " -> " + after.removed()
          + ", authoritative=" + authoritative);

      if (verified && !authoritative) {
        rollbackField(victim, candidate, oldValue, result);
        continue;
      }

      result.recordDamageLikeChange(before, after);
      if (!verified && authoritative && move.dealtEquivalent() > 0.0F)
        result.recordSyntheticDamage(move.dealtEquivalent(), "private_field " + candidate.name());
      if (authoritative && move.dealtEquivalent() > 0.0F) result.markKillPath(KillPathKind.SURFACE_HEALTH, "private field health-equivalent move " + candidate.name());
      handleServerDeath(victim, context.source(), result, verified ? "absolute_private_field_verified" : "private_field");
      return ProfileGuidedExplorer.record(
          context,
          id,
          new StepResult(authoritative, authoritative ? move.dealtEquivalent() : 0.0F, authoritative ? KillPathKind.SURFACE_HEALTH : KillPathKind.NONE),
          before,
          after,
          result
      );
    }

    DamageSnapshot snapshot = DamageSnapshot.of(victim);
    ProfileGuidedExplorer.record(context, id, StepResult.noProgress(), snapshot, snapshot, result);
    return StepResult.noProgress();
  }

  static StepResult clearAllCooldowns(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    clearInvulnerability(context.entity(), victim);
    GuardStateSupport.clearDamageGuards(victim, result, "final_clear_all_cooldowns");
    result.markSupportChanged("final_clear_all_cooldowns");
    result.add("final_clear_all_cooldowns: entityInvul=" + (context.entity() == null ? "null" : context.entity().invulnerableTime)
        + ", victimInvul=" + victim.invulnerableTime
        + ", hurtTime=" + victim.hurtTime
        + ", hurtDuration=" + victim.hurtDuration);
    return StepResult.support(true);
  }

  static StepResult numericDefenseSurgery(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    boolean changed = false;
    Class<?> cls = victim.getClass();
    while (cls != null && cls != Object.class) {
      for (Field field : cls.getDeclaredFields()) {
        try {
          if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;
          if (!FieldScanner.isNumericField(field)) continue;
          String name = field.getName().toUpperCase(Locale.ROOT);
          field.setAccessible(true);
          Object before = field.get(victim);
          Object after = null;
          if (containsAny(name, "DAMAGECAP", "DAMAGE_CAP", "MAXDAMAGE", "MAX_DAMAGE", "DAMAGE_LIMIT", "DAMAGELIMIT", "MAXHURT", "MAX_HURT"))
            after = FieldScanner.highValue(before);
          else if (containsAny(name, "COOLDOWN", "INVUL", "INVINCIBLE", "IMMUNE", "HURTTIME", "HURT_TIME", "DAMAGE_TICK", "LAST_DAMAGE_TICK"))
            after = FieldScanner.zeroValue(before);
          else if (containsAny(name, "SHIELD", "BARRIER", "GUARD"))
            after = FieldScanner.zeroValue(before);
          if (after == null || Objects.equals(before, after)) continue;
          field.set(victim, after);
          result.add("final_cap_surgery " + field.getDeclaringClass().getSimpleName() + "#" + field.getName() + ": " + before + " -> " + after);
          result.markSupportChanged("numeric defense field changed " + field.getName());
          changed = true;
        } catch (Throwable e) {
          result.add("final_cap_surgery field error: " + field.getName() + ", error=" + e.getClass().getSimpleName());
        }
      }
      cls = cls.getSuperclass();
    }
    return new StepResult(changed, 0.0F, changed ? KillPathKind.CAP_CLEAR : KillPathKind.NONE);
  }

  static StepResult booleanDefenseSurgery(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    boolean changed = false;
    Class<?> cls = victim.getClass();
    while (cls != null && cls != Object.class) {
      for (Field field : cls.getDeclaredFields()) {
        try {
          if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;
          Class<?> type = field.getType();
          if (type != boolean.class && type != Boolean.class) continue;
          String name = field.getName().toUpperCase(Locale.ROOT);
          if (!containsAny(name, "INVUL", "INVINCIBLE", "IMMUNE", "IMMUNITY", "SHIELDED", "SHIELDING", "BARRIER", "LOCK", "LOCKED", "PHASELOCK", "PHASE_LOCK", "DEATHLESS", "DEATH_IMMUNE", "UNKILLABLE", "PROTECTED", "PROTECTION")) continue;
          field.setAccessible(true);
          Object before = field.get(victim);
          if (!(before instanceof Boolean b) || !b) continue;
          field.set(victim, false);
          Object after = field.get(victim);
          result.add("final_boolean_defense_surgery " + field.getDeclaringClass().getSimpleName() + "#" + field.getName() + ": " + before + " -> " + after);
          result.markSupportChanged("boolean defense field changed " + field.getName());
          changed = true;
        } catch (Throwable e) {
          result.add("final_boolean_defense_surgery field error: " + field.getName() + ", error=" + e.getClass().getSimpleName());
        }
      }
      cls = cls.getSuperclass();
    }
    return new StepResult(changed, 0.0F, changed ? KillPathKind.CAP_CLEAR : KillPathKind.NONE);
  }

  static StepResult linkedTargetProbe(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    if (ProfileGuidedExplorer.shouldSkip(context, result, ProbeStepId.LINKED_TARGET)) return StepResult.noProgress();
    List<LivingEntity> targets = LinkedTargetFinder.find(context.entity(), victim, context.source(), result::add);
    boolean foundValid = !targets.isEmpty();
    result.add("linked_target_probe: foundValid=" + foundValid + ", validTargets=" + targets.size());
    DamageSnapshot snapshot = DamageSnapshot.of(victim);
    ProfileGuidedExplorer.record(context, ProbeStepId.LINKED_TARGET, StepResult.noProgress(), snapshot, snapshot, result);
    return foundValid ? new StepResult(true, 0.0F) : StepResult.noProgress();
  }

  static StepResult killLinkedTargets(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    List<LivingEntity> targets = LinkedTargetFinder.find(context.entity(), victim, context.source(), result::add);
    boolean victimConfirmed = LinkedTargetKillSupport.tryKillLinkedTargets(context.entity(), victim, context.source(), targets, target -> {
      try {
        DamageSnapshot before = DamageSnapshot.of(target);
        target.setHealth(0.0F);
        DamageSnapshot after = DamageSnapshot.of(target);
        result.add("kill_linked_target_inner: " + target.getClass().getSimpleName()
            + ", health " + before.health() + " -> " + after.health()
            + ", alive " + before.alive() + " -> " + after.alive()
            + ", removed " + before.removed() + " -> " + after.removed());
        return target.isRemoved() || (!target.isAlive() && target.getHealth() <= DamageConstants.DAMAGE_EPS);
      } catch (Throwable e) {
        result.add("kill_linked_target_inner error: " + e.getClass().getSimpleName());
        return false;
      }
    }, ignored -> result.killConfirmed(), result::add);
    return new StepResult(victimConfirmed, 0.0F);
  }

  static StepResult hardSetHealthZero(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    if (ProfileGuidedExplorer.shouldSkip(context, result, ProbeStepId.HARD_SET_HEALTH_ZERO)) return StepResult.noProgress();
    GuardStateSupport.clearDamageGuards(victim, result, "hard_set_health_zero");
    StepResult nbt = nbtMutation(context, result, "hard_set_health_zero_nbt_preflight");
    if (result.killConfirmed()) return ProfileGuidedExplorer.record(context, ProbeStepId.HARD_SET_HEALTH_ZERO, nbt, DamageSnapshot.of(victim), DamageSnapshot.of(victim), result);

    DamageSnapshot before = DamageSnapshot.of(victim);
    try {
      victim.setHealth(0.0F);
    } catch (Throwable e) {
      result.add("hard_set_health_zero error: " + e.getClass().getSimpleName());
      DamageSnapshot afterError = DamageSnapshot.of(victim);
      ProfileGuidedExplorer.record(context, ProbeStepId.HARD_SET_HEALTH_ZERO, StepResult.noProgress(), before, afterError, result);
      return StepResult.noProgress();
    }
    DamageSnapshot after = DamageSnapshot.of(victim);
    result.recordDamageLikeChange(before, after);
    float dealt = positiveDelta(before.health(), after.health());
    result.add("hard_set_health_zero: " + before.health() + " -> " + after.health());
    ProfileGuidedExplorer.observePossibleCap(context, context.remainingOrAmount(result), dealt, result);
    handleServerDeath(victim, context.source(), result, "hard_set_health_zero");
    boolean killed = result.killConfirmed() || (!after.alive() && after.health() <= DamageConstants.DAMAGE_EPS) || after.health() <= DamageConstants.DAMAGE_EPS;
    if (dealt > DamageConstants.DAMAGE_EPS && !killed) {
      result.add("hard_set_health_zero_cap_limited_partial: requested=" + context.remainingOrAmount(result)
          + ", dealt=" + dealt
          + ", health " + before.health() + " -> " + after.health()
          + ", not terminal success");
    }
    if (killed || dealt > DamageConstants.DAMAGE_EPS) result.markKillPath(KillPathKind.SURFACE_HEALTH, "hard setHealth zero equivalent");
    return ProfileGuidedExplorer.record(context, ProbeStepId.HARD_SET_HEALTH_ZERO, new StepResult(killed, dealt, killed ? KillPathKind.SURFACE_HEALTH : KillPathKind.NONE), before, after, result);
  }

  static StepResult forceDeathNow(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    if (ProfileGuidedExplorer.shouldSkip(context, result, ProbeStepId.FORCE_DIE)) return StepResult.noProgress();
    DamageSnapshot before = DamageSnapshot.of(victim);
    result.add("force_death_now skipped: die(source) is side-effect-only unless remove policy is used; health="
        + victim.getHealth() + ", isAlive=" + victim.isAlive() + ", removed=" + victim.isRemoved() + ", deathTime=" + victim.deathTime);
    return ProfileGuidedExplorer.record(context, ProbeStepId.FORCE_DIE, StepResult.noProgress(), before, before, result);
  }

  static StepResult forceRemove(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();
    result.add("force_remove before: health=" + victim.getHealth() + ", isAlive=" + victim.isAlive() + ", removed=" + victim.isRemoved());
    try {
      victim.remove(Entity.RemovalReason.KILLED);
    } catch (Throwable e) {
      result.add("force_remove error: " + e.getClass().getSimpleName());
    }
    result.add("force_remove after: health=" + victim.getHealth() + ", isAlive=" + victim.isAlive() + ", removed=" + victim.isRemoved());
    if (result.killConfirmed()) {
      result.markDeathHandled("force_remove confirmed");
      result.serverDead("force_remove confirmed");
    }
    if (result.killConfirmed()) result.markKillPath(KillPathKind.REMOVE, "force remove");
    return new StepResult(result.killConfirmed(), 0.0F, result.killConfirmed() ? KillPathKind.REMOVE : KillPathKind.NONE);
  }

  static StepResult methodHealthBacking(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) {
      result.add("generic_method_health skipped: victim=null");
      return StepResult.noProgress();
    }
    DamageProfile profile = DamageProfileCache.profileFor(victim);
    boolean externalSuspect = profile.nextDirective() == ProbeDirective.SUSPECT_EXTERNAL_CAP
        || profile.suspectExternalCap()
        || profile.suspectBytecodeOrEventCap();
    if (!externalSuspect && ProfileGuidedExplorer.shouldSkip(context, result, ProbeStepId.METHOD_HEALTH_BACKING))
      return StepResult.noProgress();

    DamageSnapshot before = DamageSnapshot.of(victim);
    StepResult nbt = nbtMutation(context, result, "generic_method_health_nbt_preflight");
    if (result.killConfirmed() || nbt.progress()) {
      DamageSnapshot afterNbt = DamageSnapshot.of(victim);
      return ProfileGuidedExplorer.record(context, ProbeStepId.METHOD_HEALTH_BACKING, nbt, before, afterNbt, result);
    }

    StepResult step = MethodHealthProbe.tryMethodHealthBacking(context, result);
    DamageSnapshot after = DamageSnapshot.of(victim);
    return ProfileGuidedExplorer.record(context, ProbeStepId.METHOD_HEALTH_BACKING, step, before, after, result);
  }

  private static Object readFieldValue(LivingEntity victim, FieldCandidate candidate, DamageProbeResult result, boolean verified) {
    if (!verified) return null;
    try {
      Field field = candidate.field();
      field.setAccessible(true);
      return field.get(victim);
    } catch (Throwable e) {
      result.add("absolute_private_field_verified read_old error: "
          + candidate.field().getDeclaringClass().getSimpleName() + "#" + candidate.name()
          + ", error=" + e.getClass().getSimpleName());
      return null;
    }
  }

  private static void rollbackField(LivingEntity victim, FieldCandidate candidate, Object oldValue, DamageProbeResult result) {
    if (oldValue == null) return;
    try {
      Field field = candidate.field();
      field.setAccessible(true);
      Object current = field.get(victim);
      field.set(victim, oldValue);
      result.add("absolute_private_field_verified rollback: "
          + field.getDeclaringClass().getSimpleName() + "#" + candidate.name()
          + ", " + current + " -> " + oldValue);
    } catch (Throwable e) {
      result.add("absolute_private_field_verified rollback error: "
          + candidate.field().getDeclaringClass().getSimpleName() + "#" + candidate.name()
          + ", error=" + e.getClass().getSimpleName());
    }
  }

  private static Object readEntityDataValue(LivingEntity victim, DataCandidate candidate, DamageProbeResult result, boolean verified) {
    if (!verified) return null;
    try {
      return victim.getEntityData().get(candidate.accessor());
    } catch (Throwable e) {
      result.add("absolute_entity_data_verified read_old error: "
          + candidate.name()
          + ", error=" + e.getClass().getSimpleName());
      return null;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void rollbackEntityData(LivingEntity victim, DataCandidate candidate, Object oldValue, DamageProbeResult result) {
    if (oldValue == null) return;
    try {
      Object current = victim.getEntityData().get(candidate.accessor());
      victim.getEntityData().set((EntityDataAccessor) candidate.accessor(), oldValue);
      result.add("absolute_entity_data_verified rollback: "
          + candidate.name()
          + ", " + current + " -> " + oldValue);
    } catch (Throwable e) {
      result.add("absolute_entity_data_verified rollback error: "
          + candidate.name()
          + ", error=" + e.getClass().getSimpleName());
    }
  }

  private static void clearInvulnerability(Entity entity, LivingEntity victim) {
    if (entity != null) entity.invulnerableTime = 0;
    if (victim != null) {
      victim.invulnerableTime = 0;
      victim.hurtTime = 0;
      victim.hurtDuration = 0;
    }
  }

  private static void handleServerDeath(LivingEntity victim, DamageSource source, DamageProbeResult result, String reason) {
    if (victim == null || !result.killConfirmed()) return;
    result.markDeathHandled(reason + " confirmed");
    result.serverDead(reason + ", health=" + victim.getHealth()
        + ", isAlive=" + victim.isAlive()
        + ", removed=" + victim.isRemoved()
        + ", deathTime=" + victim.deathTime);
  }

  private static float positiveDelta(float before, float after) {
    return Math.max(0.0F, before - after);
  }

  private static boolean containsAny(String s, String... keys) {
    for (String key : keys) if (s.contains(key)) return true;
    return false;
  }
}
