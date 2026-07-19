package org.brahypno.esotericismtinker.utils.damage.pipeline;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageConstants;
import org.brahypno.esotericismtinker.utils.damage.DamageContext;
import org.brahypno.esotericismtinker.utils.damage.DamageOptions;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;
import org.brahypno.esotericismtinker.utils.damage.FinalKillPolicy;
import org.brahypno.esotericismtinker.utils.damage.profile.DamageProfile;
import org.brahypno.esotericismtinker.utils.damage.profile.DamageProfileCache;
import org.brahypno.esotericismtinker.utils.damage.profile.ProbeDirective;
import org.brahypno.esotericismtinker.utils.damage.profile.ProbeStepId;
import org.brahypno.esotericismtinker.utils.damage.step.StepResult;
import org.jetbrains.annotations.Nullable;

public final class FinalDamagePipeline {
  private FinalDamagePipeline() {}

  public static DamageProbeResult apply(@Nullable Entity entity, DamageSource source, float amount, DamageOptions options) {
    DamageProbeResult result = MediumDamagePipeline.apply(entity, source, amount, options);
    DamageContext mediumContext = result.context();
    LivingEntity victim = mediumContext.victim();
    result.addHeader("final begin");
    if (entity == null || source == null || amount <= 0.0F) return result;
    if (victim == null || victim.level().isClientSide) return result;

    PipelineSteps.profileHeader(mediumContext, result);
    boolean lethalRequested = amount + DamageConstants.DAMAGE_EPS >= Math.max(0.0F, mediumContext.initialHealth());
    result.markFinalRequested(lethalRequested);
    result.add("final decision: initialHealth=" + mediumContext.initialHealth()
        + ", currentHealth=" + victim.getHealth()
        + ", amount=" + amount
        + ", lethalRequested=" + lethalRequested
        + ", mediumSuccess=" + result.success()
        + ", mediumInstantEffective=" + result.instantEffective()
        + ", mediumKillConfirmed=" + result.killConfirmed()
        + ", killPathKind=" + result.killPathKind());

    if (lethalRequested) {
      if (result.killConfirmed()) {
        finalizeIfSurfaceHealth(mediumContext, result, "post_health_zero_finalizer_medium");
        return result.success("final: medium kill confirmed");
      }
      result.clearSuccess("final lethal escalation required; medium did not kill");
      return finalLethalEscalation(entity, source, amount, options, result);
    }

    if (result.instantEffective()) return result.success("final: medium instant effective");
    result.clearSuccess("final non-lethal escalation required; medium had no instant effect");
    return finalNonLethalEscalation(entity, source, amount, options, result);
  }

  private static DamageProbeResult finalNonLethalEscalation(Entity entity, DamageSource source, float amount, DamageOptions options, DamageProbeResult result) {
    DamageContext context = result.context();
    result.addHeader("final non-lethal escalation begin");
    PipelineSteps.clearAllCooldowns(context, result);
    PipelineSteps.numericDefenseSurgery(context, result);
    PipelineSteps.booleanDefenseSurgery(context, result);
    PipelineSteps.linkedTargetProbe(context, result);

    StepResult damageMethod = PipelineSteps.damageMethodProbe(context, result, "final_non_lethal_damage_method");
    if (result.killConfirmed() || result.instantEffective()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_non_lethal_damage_method");
      return result.success("final_non_lethal_damage_method");
    }
    StepResult methodHealth = PipelineSteps.methodHealthBacking(context, result);
    if (result.killConfirmed()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_non_lethal_method_health");
      return result.success("final_lethal_generic_method_health_backing");
    }
    if (methodHealth.progress()) result.add("final_lethal_generic_method_health_backing made progress; continuing terminal checks");

    PipelineSteps.superSetHealth(context, result);
    if (result.instantEffective()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_non_lethal_super_set_health");
      return result.success("final_non_lethal_super_set_health");
    }

    PipelineSteps.entityDataUnsafe(context, result, true);
    if (result.instantEffective()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_non_lethal_entity_data_verified");
      return result.success("final_non_lethal_entity_data_verified");
    }

    PipelineSteps.privateFieldUnsafe(context, result, true);
    if (result.instantEffective()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_non_lethal_private_field_verified");
      return result.success("final_non_lethal_private_field_verified");
    }

    return result.fail("final non-lethal escalation failed");
  }

  private static DamageProbeResult finalLethalEscalation(Entity entity, DamageSource source, float amount, DamageOptions options, DamageProbeResult result) {
    DamageContext context = result.context();
    LivingEntity victim = context.victim();
    DamageProfile profile = DamageProfileCache.profileFor(victim);
    profile.beginAttempt();

    result.addHeader("final lethal escalation begin");
    result.add("final lethal initial state: health=" + victim.getHealth()
        + ", isAlive=" + victim.isAlive()
        + ", removed=" + victim.isRemoved()
        + ", deathTime=" + victim.deathTime
        + ", amount=" + amount
        + ", directive=" + profile.nextDirective()
        + ", killPathKind=" + result.killPathKind());

    PipelineSteps.clearAllCooldowns(context, result);
    PipelineSteps.numericDefenseSurgery(context, result);
    PipelineSteps.booleanDefenseSurgery(context, result);

    StepResult damageMethod = PipelineSteps.damageMethodProbe(context, result, "final_lethal_damage_method");
    if (damageMethod.progress() && result.killConfirmed()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_damage_method");
      return result.success("final_lethal_damage_method");
    }
    StepResult methodHealth = PipelineSteps.methodHealthBacking(context, result);
    if (methodHealth.progress() && result.killConfirmed()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_method_health");
      return result.success("final_lethal_generic_method_health_backing");
    }
    if (methodHealth.progress()) result.add("final_lethal_generic_method_health_backing made progress; continuing terminal checks");

    PipelineSteps.linkedTargetProbe(context, result);

    if (tryProfileGuidedOpening(context, result, profile)) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_profile_guided_opening");
      return result.success("final_lethal_profile_guided_opening");
    }

    PipelineSteps.superSetHealth(context, result);
    if (result.killConfirmed()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_super_set_health");
      return result.success("final_lethal_super_set_health");
    }

    if (tryProfileExplorationBeforeClamp(context, result, profile)) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_profile_exploration");
      return result.success("final_lethal_profile_exploration");
    }

    PipelineSteps.hardSetHealthZero(context, result);
    if (result.killConfirmed()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_hard_set_health_zero");
      return result.success("final_lethal_hard_set_health_zero");
    }

    if (profile.nextDirective() == ProbeDirective.EXPLORE_CAP_SOURCE) {
      PipelineSteps.exploreCapSource(context, result);
      StepResult capBypass = PipelineSteps.tryLearnedCapFieldBypass(context, result);
      if (capBypass.progress() && result.killConfirmed()) {
        finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_learned_cap_after_explore");
        return result.success("final_lethal_learned_cap_field_after_explore");
      }
    }

    if (profile.learnedCapField() != null) {
      StepResult capBypass = PipelineSteps.tryLearnedCapFieldBypass(context, result);
      if (capBypass.progress() && result.killConfirmed()) {
        finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_learned_cap");
        return result.success("final_lethal_learned_cap_field");
      }
    }

    PipelineSteps.forceDeathNow(context, result);
    if (result.killConfirmed()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_force_death_now");
      return result.success("final_lethal_force_death_now");
    }

    StepResult linkedKill = PipelineSteps.killLinkedTargets(context, result);
    if (linkedKill.progress() && result.killConfirmed()) {
      finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_linked_target");
      return result.success("final_lethal_linked_target");
    }

    if (shouldTryAbsoluteAfterProfile(profile)) {
      result.add("final lethal: profile-guided paths failed; trying absolute verified as last non-remove probe");
      PipelineSteps.entityDataUnsafe(context, result, true);
      if (result.killConfirmed()) {
        finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_entity_data_verified");
        return result.success("final_lethal_entity_data_verified");
      }
      PipelineSteps.privateFieldUnsafe(context, result, true);
      if (result.killConfirmed()) {
        finalizeIfSurfaceHealth(context, result, "post_health_zero_finalizer_private_field_verified");
        return result.success("final_lethal_private_field_verified");
      }
    } else {
      result.add("profile_skip_absolute_tail: directive=" + profile.nextDirective() + ", summary=" + profile.summary());
    }

    if (options.killPolicy() == FinalKillPolicy.REMOVE_AS_LAST_RESORT) {
      PipelineSteps.forceRemove(context, result);
      if (result.killConfirmed()) return result.success("final_lethal_force_remove");
    }

    profile.recordFailedLethal();
    if (profile.capSourceExplored() && profile.learnedCapField() == null && profile.learnedHealthField() == null && profile.allKnownTerminalPathsFailed()) {
      profile.markSuspectExternalCap("all known terminal paths failed after health/cap exploration");
    }

    result.add("final lethal final state: health=" + victim.getHealth()
        + ", isAlive=" + victim.isAlive()
        + ", removed=" + victim.isRemoved()
        + ", deathTime=" + victim.deathTime
        + ", profile=" + profile.summary());
    return result.fail("final lethal escalation failed");
  }

  private static void finalizeIfSurfaceHealth(DamageContext context, DamageProbeResult result, String prefix) {
    if (!result.needsSurfaceDeathFinalizer()) return;
    PostHealthZeroFinalizer.finalizeSurfaceDeath(context, result, prefix);
  }

  private static boolean shouldTryMethodHealthBacking(DamageProfile profile) {
    ProbeDirective directive = profile.nextDirective();
    return directive == ProbeDirective.SUSPECT_EXTERNAL_CAP
        || profile.suspectExternalCap()
        || profile.suspectBytecodeOrEventCap()
        || profile.capSourceExplored() && profile.learnedHealthField() == null && profile.learnedCapField() == null;
  }

  private static boolean tryProfileGuidedOpening(DamageContext context, DamageProbeResult result, DamageProfile profile) {
    ProbeDirective directive = profile.nextDirective();
    if (directive == ProbeDirective.TRY_LEARNED_HEALTH_FIELD) {
      StepResult learned = PipelineSteps.tryLearnedHealthFieldKill(context, result);
      if (learned.progress() && result.killConfirmed()) return true;
    }
    if (directive == ProbeDirective.TRY_LEARNED_CAP_FIELD) {
      StepResult cap = PipelineSteps.tryLearnedCapFieldBypass(context, result);
      return cap.progress() && result.killConfirmed();
    }
    return false;
  }

  private static boolean tryProfileExplorationBeforeClamp(DamageContext context, DamageProbeResult result, DamageProfile profile) {
    ProbeDirective directive = profile.nextDirective();
    if (directive == ProbeDirective.EXPLORE_HEALTH_BACKING) {
      PipelineSteps.exploreHealthBackingFields(context, result);
      StepResult learned = PipelineSteps.tryLearnedHealthFieldKill(context, result);
      if (learned.progress() && result.killConfirmed()) return true;
    }
    if (profile.nextDirective() == ProbeDirective.EXPLORE_CAP_SOURCE) {
      PipelineSteps.exploreCapSource(context, result);
      StepResult cap = PipelineSteps.tryLearnedCapFieldBypass(context, result);
      if (cap.progress() && result.killConfirmed()) return true;
    }
    if (profile.learnedCapField() != null) {
      StepResult cap = PipelineSteps.tryLearnedCapFieldBypass(context, result);
      return cap.progress() && result.killConfirmed();
    }
    return false;
  }

  private static boolean shouldTryAbsoluteAfterProfile(DamageProfile profile) {
    if (profile.suspectExternalCap() || profile.suspectBytecodeOrEventCap()) return false;
    if (profile.nextDirective() == ProbeDirective.EXPLORE_HEALTH_BACKING || profile.nextDirective() == ProbeDirective.EXPLORE_CAP_SOURCE) return false;
    return !profile.step(ProbeStepId.ENTITY_DATA_ABSOLUTE).shouldSkip()
        || !profile.step(ProbeStepId.PRIVATE_FIELD_ABSOLUTE).shouldSkip();
  }
}
