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
import java.util.List;
import java.util.Locale;

/**
 * Post-zero death finalizer for paths that are semantically equivalent to setHealth(0).
 * It is not used as an independent damage method and is not applied to authority-state kills.
 */
final class PostHealthZeroFinalizer {
  private PostHealthZeroFinalizer() {}

  static StepResult finalizeSurfaceDeath(DamageContext context, DamageProbeResult result, String prefix) {
    LivingEntity victim = context.victim();
    DamageSource source = context.source();
    if (victim == null || source == null) return StepResult.noProgress();
    if (!result.needsSurfaceDeathFinalizer()) {
      result.add(prefix + " skipped: killPathKind=" + result.killPathKind()
          + ", killConfirmed=" + result.killConfirmed()
          + ", deathFinalized=" + result.deathFinalized());
      return StepResult.noProgress();
    }

    DamageSnapshot before = DamageSnapshot.of(victim);
    int beforeItems = nearbyItemCount(victim);
    result.add(prefix + " begin: health=" + victim.getHealth()
        + ", isAlive=" + victim.isAlive()
        + ", removed=" + victim.isRemoved()
        + ", deathTime=" + victim.deathTime
        + ", nearbyItems=" + beforeItems);

    boolean dieCalled = callDie(victim, source, result, prefix);
    DamageSnapshot afterDie = DamageSnapshot.of(victim);
    int afterDieItems = nearbyItemCount(victim);
    result.add(prefix + " after_die: called=" + dieCalled
        + ", health=" + victim.getHealth()
        + ", isAlive=" + victim.isAlive()
        + ", removed=" + victim.isRemoved()
        + ", deathTime=" + victim.deathTime
        + ", nearbyItems=" + afterDieItems);

    boolean dropCalled = false;
    if (afterDieItems <= beforeItems) {
      dropCalled = invokeDropFallback(victim, source, result, prefix);
    }

    DamageSnapshot after = DamageSnapshot.of(victim);
    int afterItems = nearbyItemCount(victim);
    boolean changed = after.deathTime() != before.deathTime()
        || after.removed() != before.removed()
        || after.alive() != before.alive()
        || afterItems > beforeItems
        || dieCalled
        || dropCalled;
    result.add(prefix + " end: dieCalled=" + dieCalled
        + ", dropFallbackCalled=" + dropCalled
        + ", health=" + victim.getHealth()
        + ", isAlive=" + victim.isAlive()
        + ", removed=" + victim.isRemoved()
        + ", deathTime=" + victim.deathTime
        + ", nearbyItems=" + afterItems
        + ", changed=" + changed);

    if (changed) {
      result.markDeathFinalized(prefix);
      result.markKillPath(KillPathKind.DEATH_FINALIZER, prefix);
      if (result.killConfirmed()) result.serverDead(prefix + " confirmed, health=" + victim.getHealth()
          + ", isAlive=" + victim.isAlive()
          + ", removed=" + victim.isRemoved()
          + ", deathTime=" + victim.deathTime);
    }
    return new StepResult(changed, 0.0F, changed ? KillPathKind.DEATH_FINALIZER : KillPathKind.NONE);
  }

  private static boolean callDie(LivingEntity victim, DamageSource source, DamageProbeResult result, String prefix) {
    try {
      victim.die(source);
      return true;
    } catch (Throwable e) {
      result.add(prefix + " die error: " + e.getClass().getSimpleName());
      return false;
    }
  }

  private static boolean invokeDropFallback(LivingEntity victim, DamageSource source, DamageProbeResult result, String prefix) {
    for (Method method : dropMethods(victim.getClass())) {
      try {
        method.setAccessible(true);
        Object[] args = argsFor(method, source);
        if (args == null) continue;
        method.invoke(victim, args);
        result.add(prefix + " drop_fallback invoked: "
            + method.getDeclaringClass().getSimpleName() + "#" + method.getName());
        return true;
      } catch (Throwable e) {
        result.add(prefix + " drop_fallback error: "
            + method.getDeclaringClass().getSimpleName() + "#" + method.getName()
            + ", error=" + e.getClass().getSimpleName());
      }
    }
    result.add(prefix + " drop_fallback skipped: no compatible drop method");
    return false;
  }

  private static List<Method> dropMethods(Class<?> start) {
    java.util.ArrayList<Method> methods = new java.util.ArrayList<>();
    Class<?> cls = start;
    while (cls != null && cls != Object.class) {
      for (Method method : cls.getDeclaredMethods()) {
        if (Modifier.isStatic(method.getModifiers())) continue;
        String lower = method.getName().toLowerCase(Locale.ROOT);
        if (!lower.contains("drop")) continue;
        if (!lower.contains("death") && !lower.contains("loot")) continue;
        if (argsFor(method, null) == null) continue;
        methods.add(method);
      }
      cls = cls.getSuperclass();
    }
    methods.sort((a, b) -> scoreDropMethod(b) - scoreDropMethod(a));
    return methods;
  }

  private static int scoreDropMethod(Method method) {
    String lower = method.getName().toLowerCase(Locale.ROOT);
    int score = 0;
    if (lower.contains("all")) score += 20;
    if (lower.contains("death")) score += 20;
    if (lower.contains("loot")) score += 10;
    return score - method.getParameterCount();
  }

  private static Object[] argsFor(Method method, DamageSource source) {
    Class<?>[] params = method.getParameterTypes();
    if (params.length == 1 && DamageSource.class.isAssignableFrom(params[0])) return new Object[]{source};
    if (params.length == 2 && DamageSource.class.isAssignableFrom(params[0]) && params[1] == boolean.class) return new Object[]{source, true};
    if (params.length == 3 && DamageSource.class.isAssignableFrom(params[0]) && params[1] == int.class && params[2] == boolean.class) return new Object[]{source, 0, true};
    return null;
  }

  private static int nearbyItemCount(LivingEntity victim) {
    try {
      return victim.level().getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(12.0D)).size();
    } catch (Throwable ignored) {
      return -1;
    }
  }
}
