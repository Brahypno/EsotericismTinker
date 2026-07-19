package org.brahypno.esotericismtinker.utils.damage.method;

import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageConstants;
import org.brahypno.esotericismtinker.utils.damage.DamageContext;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;
import org.brahypno.esotericismtinker.utils.damage.state.DamageSnapshot;
import org.brahypno.esotericismtinker.utils.damage.state.NbtStateDiff;
import org.brahypno.esotericismtinker.utils.damage.step.StepResult;
import org.brahypno.esotericismtinker.utils.damage.profile.DamageProfile;
import org.brahypno.esotericismtinker.utils.damage.profile.DamageProfileCache;
import org.brahypno.esotericismtinker.utils.damage.profile.ProfileGuidedExplorer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Generic method-level state probe.
 *
 * Important semantics:
 * - No target-specific class names or mod-specific method names.
 * - Side effects are diagnostic only. They do not mean damage progress.
 * - Non-health getter movement, such as animation/progress/last-hurt state, does not mean damage progress.
 * - This class must not call die(source), remove(), or death-like setters.
 */
public final class MethodHealthProbe {
  private static final int MAX_PAIRS = 18;
  private static final int MAX_TARGETS_PER_PAIR = 7;
  private static final int MAX_FIELD_SNAPSHOTS = 768;
  private static final int MAX_OBJECT_DEPTH = 2;

  private MethodHealthProbe() {}

  public static StepResult tryMethodHealthBacking(DamageContext context, DamageProbeResult result) {
    LivingEntity victim = context.victim();
    if (victim == null) {
      result.add("generic_method_health skipped: victim=null");
      return StepResult.noProgress();
    }

    result.add("========== generic method health backing probe begin ==========");
    result.add("generic_method_health note: side effects are diagnostic only; progress requires visible/authoritative movement or strong health-like getter movement");

    List<Getter> getters = findFloatGetters(victim.getClass(), result);
    List<Setter> setters = findFloatSetters(victim.getClass(), result);
    result.add("generic_method_health getters=" + getters.size() + ", setters=" + setters.size());

    if (getters.isEmpty() || setters.isEmpty()) {
      result.add("generic_method_health skipped: no float getter/setter pair");
      return StepResult.noProgress();
    }

    List<Pair> pairs = pair(getters, setters);
    MethodBytecodeProbe.inspectPairs(victim.getClass(), pairs, result, MAX_PAIRS);
    return tryPairs(context, victim, pairs, result);
  }

  private static StepResult tryPairs(DamageContext context, LivingEntity victim, List<Pair> pairs, DamageProbeResult result) {
    boolean anyProgress = false;
    boolean anyExternalSideEffect = false;
    float bestDealt = 0.0F;
    int tried = 0;

    for (Pair pair : pairs) {
      if (tried++ >= MAX_PAIRS) break;
      PairResult pairResult = tryPair(context, victim, pair, result);
      anyProgress |= pairResult.step().progress();
      anyExternalSideEffect |= pairResult.externalSideEffect();
      bestDealt = Math.max(bestDealt, pairResult.step().dealt());

      if (result.killConfirmed()) {
        result.add("generic_method_health confirmed kill with " + pair.describe());
        return new StepResult(true, bestDealt);
      }
    }

    result.add("generic_method_health end: tried=" + tried
        + ", progress=" + anyProgress
        + ", externalSideEffect=" + anyExternalSideEffect
        + ", bestDealt=" + bestDealt);
    return anyProgress ? new StepResult(true, bestDealt) : StepResult.noProgress();
  }

  private static PairResult tryPair(DamageContext context, LivingEntity victim, Pair pair, DamageProbeResult result) {
    try {
      Float getterBefore = invokeFloatGetter(victim, pair.getter().method());
      List<Float> targets = targetsFor(getterBefore, victim.getHealth());
      float bestDealt = 0.0F;
      boolean anyProgress = false;
      boolean anyExternalSideEffect = false;
      int tries = 0;

      result.add("generic_method_health_pair_begin: " + pair.describe()
          + ", getterBefore=" + getterBefore
          + ", targets=" + targets);

      for (float target : targets) {
        if (tries++ >= MAX_TARGETS_PER_PAIR) break;
        TargetResult targetResult = tryPairTarget(context, victim, pair, target, result);
        anyProgress |= targetResult.step().progress();
        anyExternalSideEffect |= targetResult.externalSideEffect();
        bestDealt = Math.max(bestDealt, targetResult.step().dealt());
        if (result.killConfirmed()) return new PairResult(new StepResult(true, bestDealt), anyExternalSideEffect);
        if (shouldStopAfterCapLimitedPulse(context, targetResult)) {
          result.add("generic_method_health_pair_stop: cap-limited pulse observed; stop repeating targets for "
              + pair.describe()
              + ", dealt=" + targetResult.step().dealt());
          break;
        }
      }

      result.add("generic_method_health_pair_end: " + pair.describe()
          + ", triedTargets=" + tries
          + ", progress=" + anyProgress
          + ", externalSideEffect=" + anyExternalSideEffect
          + ", bestDealt=" + bestDealt);
      return new PairResult(anyProgress ? new StepResult(true, bestDealt) : StepResult.noProgress(), anyExternalSideEffect);
    } catch (Throwable e) {
      result.add("generic_method_health_pair error: " + pair.describe()
          + ", error=" + e.getClass().getSimpleName()
          + ": " + e.getMessage());
      return new PairResult(StepResult.noProgress(), false);
    }
  }

  private static TargetResult tryPairTarget(DamageContext context, LivingEntity victim, Pair pair, float target, DamageProbeResult result) {
    FieldSnapshot beforeFields = FieldSnapshot.capture(victim, MAX_FIELD_SNAPSHOTS, MAX_OBJECT_DEPTH);
    DamageSnapshot before = DamageSnapshot.of(victim);
    NbtStateDiff.Snapshot beforeNbt = NbtStateDiff.capture(victim, result, "generic_method_health_before");
    Float getterBefore = invokeFloatGetterSafe(victim, pair.getter().method(), result, "before");

    GuardStateSupport.clearDamageGuards(victim, result, "generic_method_health");
    applyFloatSetterSafe(victim, pair.setter().method(), target, result);

    FieldSnapshot afterSetFields = FieldSnapshot.capture(victim, MAX_FIELD_SNAPSHOTS, MAX_OBJECT_DEPTH);
    List<FieldDiff> setterDiffs = beforeFields.diff(afterSetFields);
    Float getterAfterSet = invokeFloatGetterSafe(victim, pair.getter().method(), result, "after_set");

    // Only try a non-side-effect setHealth nudge; never call die(source) here.
    softHealthNudge(victim, result);

    DamageSnapshot after = DamageSnapshot.of(victim);
    NbtStateDiff.Snapshot afterNbt = NbtStateDiff.capture(victim, result, "generic_method_health_after");
    FieldSnapshot afterNudgeFields = FieldSnapshot.capture(victim, MAX_FIELD_SNAPSHOTS, MAX_OBJECT_DEPTH);
    List<FieldDiff> nudgeDiffs = afterSetFields.diff(afterNudgeFields);
    Float getterAfterNudge = invokeFloatGetterSafe(victim, pair.getter().method(), result, "after_nudge");

    float dealt = Math.max(0.0F, before.health() - after.health())
        + Math.max(0.0F, before.absorption() - after.absorption());
    boolean getterMoved = moved(getterBefore, getterAfterSet) || moved(getterBefore, getterAfterNudge);
    boolean visibleMoved = dealt > DamageConstants.DAMAGE_EPS;
    boolean authoritative = authoritativeChange(before, after) || result.killConfirmed();
    boolean healthLikePair = isStrongHealthLike(pair.getter().method()) || isStrongHealthLike(pair.setter().method());
    boolean targetApplied = Math.abs(after.health() - target) <= 0.5F;
    boolean damagePulse = visibleMoved && !targetApplied;
    boolean externalSideEffect = hasSuspiciousExternalDiff(setterDiffs, target) || hasSuspiciousExternalDiff(nudgeDiffs, target);
    boolean methodHealthProgress = healthLikePair && getterMoved && targetApplied;
    boolean progress = authoritative || visibleMoved || methodHealthProgress;

    if (visibleMoved) {
      ProfileGuidedExplorer.observePossibleCap(context, context.remainingOrAmount(result), dealt, result);
      NbtStateDiff.logDiff("generic_method_health" + (damagePulse ? "_damage_pulse" : "_target_write"), beforeNbt, afterNbt, dealt, result);
      if (damagePulse) result.add("generic_method_health_damage_pulse: targetNotApplied, target=" + target + ", actualDealt=" + dealt + ", beforeHealth=" + before.health() + ", afterHealth=" + after.health());
    }

    logTargetResult(pair, target, getterBefore, getterAfterSet, getterAfterNudge, before, after,
        setterDiffs, nudgeDiffs, externalSideEffect, healthLikePair, methodHealthProgress, authoritative, progress, result);

    return new TargetResult(progress ? new StepResult(true, dealt) : StepResult.noProgress(), externalSideEffect, damagePulse);
  }

  private static void softHealthNudge(LivingEntity victim, DamageProbeResult result) {
    try {
      victim.invulnerableTime = 0;
      victim.hurtTime = 0;
      victim.hurtDuration = 0;
      victim.setHealth(0.0F);
    } catch (Throwable e) {
      result.add("generic_method_health soft nudge error: " + e.getClass().getSimpleName());
    }
  }

  private static void logTargetResult(
      Pair pair,
      float target,
      Float getterBefore,
      Float getterAfterSet,
      Float getterAfterNudge,
      DamageSnapshot before,
      DamageSnapshot after,
      List<FieldDiff> setterDiffs,
      List<FieldDiff> nudgeDiffs,
      boolean externalSideEffect,
      boolean healthLikePair,
      boolean methodHealthProgress,
      boolean authoritative,
      boolean progress,
      DamageProbeResult result
  ) {
    result.add("generic_method_health_roundtrip: " + pair.describe()
        + ", target=" + target
        + ", getter " + getterBefore + " -> " + getterAfterSet + " -> " + getterAfterNudge
        + ", visibleHealth " + before.health() + " -> " + after.health()
        + ", deathTime " + before.deathTime() + " -> " + after.deathTime()
        + ", alive " + before.alive() + " -> " + after.alive()
        + ", removed " + before.removed() + " -> " + after.removed()
        + ", setterDiffs=" + setterDiffs.size()
        + ", nudgeDiffs=" + nudgeDiffs.size()
        + ", externalSideEffect=" + externalSideEffect
        + ", healthLikePair=" + healthLikePair
        + ", methodHealthProgress=" + methodHealthProgress
        + ", authoritative=" + authoritative
        + ", progress=" + progress);
    logDiffs("generic_method_health_setter_diff", setterDiffs, result);
    logDiffs("generic_method_health_nudge_diff", nudgeDiffs, result);
  }

  private static void logDiffs(String prefix, List<FieldDiff> diffs, DamageProbeResult result) {
    int shown = 0;
    for (FieldDiff diff : diffs) {
      if (shown++ >= 16) {
        result.add(prefix + ": ... more=" + (diffs.size() - shown + 1));
        break;
      }
      result.add(prefix + ": " + diff);
    }
  }

  private static List<Float> targetsFor(Float getterBefore, float visibleHealth) {
    float base = getterBefore != null && Float.isFinite(getterBefore) ? getterBefore : visibleHealth;
    List<Float> targets = new ArrayList<>();
    addTarget(targets, base - 1.0F);
    addTarget(targets, base - 20.0F);
    addTarget(targets, base * 0.5F);
    addTarget(targets, 1.0F);
    addTarget(targets, 0.5F);
    addTarget(targets, 0.0F);
    addTarget(targets, base + 20.0F);
    return targets;
  }

  private static void addTarget(List<Float> targets, float value) {
    if (!Float.isFinite(value)) return;
    float clamped = Math.max(0.0F, value);
    for (float target : targets)
      if (Math.abs(target - clamped) <= DamageConstants.DAMAGE_EPS) return;
    targets.add(clamped);
  }

  private static boolean moved(Float before, Float after) {
    return before != null && after != null && Math.abs(after - before) > DamageConstants.DAMAGE_EPS;
  }

  private static boolean isStrongHealthLike(Method method) {
    String lower = normalize(method.getName());
    if (isKnownNonHealthFloatState(lower)) return false;
    return lower.contains("health")
        || lower.contains("currenthp")
        || lower.contains("currenthealth")
        || lower.contains("hpvalue")
        || lower.contains("life")
        || lower.contains("vital");
  }

  private static boolean isKnownNonHealthFloatState(String lower) {
    return lower.contains("lasthurt")
        || lower.contains("lastdamage")
        || lower.contains("lasttaken")
        || lower.contains("shoot")
        || lower.contains("indicator")
        || lower.contains("progress")
        || lower.contains("jump")
        || lower.contains("power")
        || lower.contains("speed")
        || lower.contains("scale")
        || lower.contains("animation")
        || lower.contains("rotation")
        || lower.contains("look")
        || lower.contains("swing")
        || lower.contains("sound")
        || lower.contains("armor")
        || lower.contains("absorb");
  }

  private static boolean hasSuspiciousExternalDiff(List<FieldDiff> diffs, float target) {
    for (FieldDiff diff : diffs) {
      String path = diff.path().toLowerCase(Locale.ROOT);
      if (path.contains("deathtime") || path.contains("last")) continue;
      if (path.contains("health") || path.contains("hp") || path.contains("life") || path.contains("hurt") || path.contains("damage")) return true;
      if (diff.newValue() instanceof Number number && Math.abs(number.floatValue() - target) <= 0.25F) return true;
    }
    return false;
  }

  private static boolean authoritativeChange(DamageSnapshot before, DamageSnapshot after) {
    return after.health() + DamageConstants.DAMAGE_EPS < before.health()
        || after.absorption() + DamageConstants.DAMAGE_EPS < before.absorption()
        || (after.deathTime() > before.deathTime() && after.health() <= DamageConstants.DAMAGE_EPS)
        || before.alive() != after.alive()
        || before.removed() != after.removed();
  }

  private static List<Getter> findFloatGetters(Class<?> start, DamageProbeResult result) {
    List<Getter> list = new ArrayList<>();
    scanHierarchy(start, method -> {
      if (method.getParameterCount() != 0) return;
      Class<?> type = method.getReturnType();
      if (type != float.class && type != Float.class) return;
      int score = getterScore(method);
      if (score <= 0) return;
      method.setAccessible(true);
      list.add(new Getter(method, score, tokens(method.getName())));
      result.add("generic_method_health_getter_candidate: " + describe(method) + ", score=" + score);
    });
    list.sort(Comparator.comparingInt(Getter::score).reversed());
    return list;
  }

  private static List<Setter> findFloatSetters(Class<?> start, DamageProbeResult result) {
    List<Setter> list = new ArrayList<>();
    scanHierarchy(start, method -> {
      if (method.getParameterCount() != 1 || method.getReturnType() != void.class) return;
      Class<?> type = method.getParameterTypes()[0];
      if (type != float.class && type != Float.class) return;
      int score = setterScore(method);
      if (score <= 0) return;
      method.setAccessible(true);
      list.add(new Setter(method, score, tokens(method.getName())));
      result.add("generic_method_health_setter_candidate: " + describe(method) + ", score=" + score);
    });
    list.sort(Comparator.comparingInt(Setter::score).reversed());
    return list;
  }

  private static List<Pair> pair(List<Getter> getters, List<Setter> setters) {
    List<Pair> pairs = new ArrayList<>();
    Set<Method> pairedSetters = new HashSet<>();
    for (Getter getter : getters) {
      for (Setter setter : setters) {
        if (!isReasonablePair(getter, setter)) continue;
        int score = getter.score() + setter.score()
            + tokenOverlapScore(getter.tokens(), setter.tokens());
        pairs.add(new Pair(getter, setter, score));
        pairedSetters.add(setter.method());
      }
    }
    // A setter without a semantically matching getter is not discarded. Probe it once using the
    // highest-scoring getter as an observation channel, instead of pairing it with every getter.
    Getter fallbackGetter = getters.isEmpty() ? null : getters.get(0);
    if (fallbackGetter != null) {
      for (Setter setter : setters) {
        if (pairedSetters.contains(setter.method())) continue;
        int score = fallbackGetter.score() + setter.score() - 24;
        pairs.add(new Pair(fallbackGetter, setter, score));
      }
    }
    pairs.sort(Comparator.comparingInt(Pair::score).reversed());
    return pairs;
  }

  private static boolean isReasonablePair(Getter getter, Setter setter) {
    String getterProperty = propertyKey(getter.method());
    String setterProperty = propertyKey(setter.method());
    if (!getterProperty.isBlank() && getterProperty.equals(setterProperty)) return true;
    int meaningfulOverlap = 0;
    int getterMeaningful = 0;
    int setterMeaningful = 0;
    for (String token : getter.tokens()) {
      if (isWeakToken(token)) continue;
      getterMeaningful++;
      if (setter.tokens().contains(token)) meaningfulOverlap++;
    }
    for (String token : setter.tokens()) if (!isWeakToken(token)) setterMeaningful++;
    int smaller = Math.min(getterMeaningful, setterMeaningful);
    return meaningfulOverlap > 0 && smaller > 0 && meaningfulOverlap * 2 >= smaller;
  }

  private static String propertyKey(Method method) {
    String name = method.getName();
    int mixinSeparator = name.lastIndexOf('$');
    if (mixinSeparator >= 0 && mixinSeparator + 1 < name.length()) {
      name = name.substring(mixinSeparator + 1);
    }
    if (name.startsWith("get") || name.startsWith("set")) name = name.substring(3);
    else if (name.startsWith("is")) name = name.substring(2);
    return normalize(name);
  }

  private static int getterScore(Method method) {
    String lower = normalize(method.getName());
    int score = stateNameScore(lower);
    if (lower.contains("get")) score += 12;
    if (lower.contains("current")) score += 6;
    if (lower.contains("max")) score -= 40;
    if (isKnownNonHealthFloatState(lower)) score -= 80;
    return Math.max(0, score);
  }

  private static int setterScore(Method method) {
    String lower = normalize(method.getName());
    int score = stateNameScore(lower);
    if (lower.contains("set")) score += 12;
    if (lower.contains("reset")) score -= 20;
    if (lower.contains("max")) score -= 40;
    if (isKnownNonHealthFloatState(lower)) score -= 80;
    return Math.max(0, score);
  }

  private static int stateNameScore(String lower) {
    int score = 0;
    if (lower.contains("health")) score += 60;
    if (lower.contains("hp")) score += 25;
    if (lower.contains("life")) score += 25;
    if (lower.contains("vital")) score += 20;
    if (lower.contains("damage")) score += 12;
    if (lower.contains("hurt")) score += 8;
    if (lower.contains("absorb")) score += 8;
    if (lower.contains("shield")) score -= 10;
    if (lower.contains("sound")) score -= 20;
    if (lower.contains("armor")) score -= 25;
    if (lower.contains("speed")) score -= 30;
    if (lower.contains("scale")) score -= 30;
    return score;
  }

  private static int tokenOverlapScore(Set<String> left, Set<String> right) {
    int score = 0;
    for (String token : left) {
      if (isWeakToken(token)) continue;
      if (right.contains(token)) score += 12;
    }
    return score;
  }

  private static boolean isWeakToken(String token) {
    return token.length() < 3 || token.equals("get") || token.equals("set") || token.equals("is") || token.equals("has") || token.equals("the") || token.equals("value");
  }

  private static void clearGenericGuardIntState(LivingEntity victim, DamageProbeResult result) {
    scanHierarchy(victim.getClass(), method -> {
      try {
        if (method.getParameterCount() != 1 || method.getReturnType() != void.class) return;
        Class<?> type = method.getParameterTypes()[0];
        if (type != int.class && type != Integer.class) return;
        String lower = normalize(method.getName());
        if (!looksLikeClearableGuardIntSetter(lower)) return;
        method.setAccessible(true);
        method.invoke(victim, 0);
        result.add("generic_method_health_clear_guard_int_state: " + describe(method) + " -> 0");
      } catch (Throwable e) {
        result.add("generic_method_health_clear_guard_int_state error: " + describe(method) + ", error=" + e.getClass().getSimpleName());
      }
    });
  }

  private static boolean looksLikeClearableGuardIntSetter(String lower) {
    return lower.contains("set") && (
        lower.contains("cooldown")
            || lower.contains("invul")
            || lower.contains("immune")
            || lower.contains("hurt")
            || lower.contains("shield")
            || lower.contains("block")
    ) && !(lower.contains("death") || lower.contains("dead") || lower.contains("dying"));
  }

  private static Float invokeFloatGetterSafe(LivingEntity victim, Method method, DamageProbeResult result, String phase) {
    try {
      return invokeFloatGetter(victim, method);
    } catch (Throwable e) {
      result.add("generic_method_health_getter_error: " + describe(method) + ", phase=" + phase + ", error=" + e.getClass().getSimpleName());
      return null;
    }
  }

  private static Float invokeFloatGetter(LivingEntity victim, Method method) throws ReflectiveOperationException {
    Object value = method.invoke(victim);
    if (value instanceof Number number) return number.floatValue();
    return null;
  }

  private static void applyFloatSetterSafe(LivingEntity victim, Method method, float value, DamageProbeResult result) {
    try {
      method.invoke(victim, value);
    } catch (Throwable e) {
      result.add("generic_method_health_setter_error: " + describe(method) + ", target=" + value + ", error=" + e.getClass().getSimpleName());
    }
  }

  private static void scanHierarchy(Class<?> start, MethodConsumer consumer) {
    Class<?> cls = start;
    while (cls != null && cls != Object.class) {
      for (Method method : cls.getDeclaredMethods()) {
        if (Modifier.isStatic(method.getModifiers())) continue;
        consumer.accept(method);
      }
      cls = cls.getSuperclass();
    }
  }

  private static Set<String> tokens(String name) {
    String spaced = name.replace('$', ' ').replace('_', ' ')
        .replaceAll("([a-z])([A-Z])", "$1 $2")
        .replaceAll("[^A-Za-z0-9]+", " ");
    String[] parts = spaced.toLowerCase(Locale.ROOT).split(" +");
    Set<String> tokens = new HashSet<>();
    for (String part : parts) if (!part.isBlank()) tokens.add(part);
    return tokens;
  }

  private static String normalize(String name) {
    return name.toLowerCase(Locale.ROOT).replace("$", "").replace("_", "");
  }

  static String describe(Method method) {
    return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
  }

  @FunctionalInterface
  interface MethodConsumer { void accept(Method method); }

  record Getter(Method method, int score, Set<String> tokens) {}
  record Setter(Method method, int score, Set<String> tokens) {}
  record Pair(Getter getter, Setter setter, int score) {
    String describe() {
      return MethodHealthProbe.describe(getter.method()) + " -> " + MethodHealthProbe.describe(setter.method()) + ", score=" + score;
    }
  }
  private record PairResult(StepResult step, boolean externalSideEffect) {}
  private static boolean shouldStopAfterCapLimitedPulse(DamageContext context, TargetResult targetResult) {
    StepResult step = targetResult.step();
    if (!targetResult.damagePulse()) return false;
    if (!step.progress() || step.dealt() <= DamageConstants.DAMAGE_EPS) return false;
    return step.dealt() + DamageConstants.DAMAGE_TOLERANCE < context.amount();
  }

  private record TargetResult(StepResult step, boolean externalSideEffect, boolean damagePulse) {}

  private record FieldSnapshot(Map<Key, Object> values, Map<Key, String> paths) {
    static FieldSnapshot capture(LivingEntity victim, int max, int maxDepth) {
      Map<Key, Object> values = new LinkedHashMap<>();
      Map<Key, String> paths = new LinkedHashMap<>();
      IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
      captureObject("this", victim, 0, maxDepth, max, values, paths, seen);
      captureStatic(victim.getClass(), max, values, paths);
      return new FieldSnapshot(values, paths);
    }

    private static void captureObject(String path, Object object, int depth, int maxDepth, int max, Map<Key, Object> values, Map<Key, String> paths, IdentityHashMap<Object, Boolean> seen) {
      if (object == null || values.size() >= max || seen.containsKey(object)) return;
      seen.put(object, true);
      Class<?> cls = object.getClass();
      while (cls != null && cls != Object.class) {
        for (Field field : cls.getDeclaredFields()) {
          if (values.size() >= max) return;
          if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;
          try {
            field.setAccessible(true);
            Object value = field.get(object);
            String fieldPath = path + "." + field.getDeclaringClass().getSimpleName() + "#" + field.getName();
            Key key = new Key(System.identityHashCode(object), field);
            if (isSimple(field.getType())) {
              values.put(key, value);
              paths.put(key, fieldPath);
            } else if (depth < maxDepth && value != null && shouldEnterObject(field, value)) {
              captureObject(fieldPath, value, depth + 1, maxDepth, max, values, paths, seen);
            }
          } catch (Throwable ignored) { }
        }
        cls = cls.getSuperclass();
      }
    }

    private static void captureStatic(Class<?> start, int max, Map<Key, Object> values, Map<Key, String> paths) {
      Class<?> cls = start;
      while (cls != null && cls != Object.class && values.size() < max) {
        for (Field field : cls.getDeclaredFields()) {
          if (values.size() >= max) return;
          if (!Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;
          try {
            field.setAccessible(true);
            Object value = field.get(null);
            if (!isSimple(field.getType())) continue;
            Key key = new Key(0, field);
            values.put(key, value);
            paths.put(key, "static." + field.getDeclaringClass().getSimpleName() + "#" + field.getName());
          } catch (Throwable ignored) { }
        }
        cls = cls.getSuperclass();
      }
    }

    private static boolean isSimple(Class<?> type) {
      return type.isPrimitive()
          || Number.class.isAssignableFrom(type)
          || type == Boolean.class
          || type == Character.class
          || type == String.class
          || type.isEnum();
    }

    private static boolean shouldEnterObject(Field field, Object object) {
      String className = object.getClass().getName();
      if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("sun.")) return false;
      if (className.startsWith("com.google.common.")) return false;

      String name = (field.getName() + " " + object.getClass().getSimpleName()).toLowerCase(Locale.ROOT);
      return name.contains("data")
          || name.contains("state")
          || name.contains("manager")
          || name.contains("cap")
          || name.contains("info")
          || name.contains("boss")
          || name.contains("phase")
          || name.contains("handler")
          || name.contains("controller")
          || name.contains("util")
          || name.contains("health")
          || name.contains("hurt")
          || name.contains("damage");
    }

    List<FieldDiff> diff(FieldSnapshot after) {
      List<FieldDiff> diffs = new ArrayList<>();
      for (Map.Entry<Key, Object> entry : values.entrySet()) {
        Key key = entry.getKey();
        if (!after.values.containsKey(key)) continue;
        Object oldValue = entry.getValue();
        Object newValue = after.values.get(key);
        if (!Objects.equals(oldValue, newValue)) {
          diffs.add(new FieldDiff(paths.getOrDefault(key, key.field().getName()), oldValue, newValue));
        }
      }
      return diffs;
    }
  }

  private record Key(int ownerIdentity, Field field) {}

  private record FieldDiff(String path, Object oldValue, Object newValue) {
    @Override
    public String toString() {
      return path + ": " + oldValue + " -> " + newValue;
    }
  }
}
