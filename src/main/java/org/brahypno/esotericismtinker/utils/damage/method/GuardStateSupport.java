package org.brahypno.esotericismtinker.utils.damage.method;

import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Caches discovered high-confidence guard clearers per runtime class, including empty results.
 * Generic only: no target class names or mod-specific literals.
 *
 * External use:
 *   GuardStateSupport.clearDamageGuards(target);
 *   GuardStateSupport.clearDamageGuards(target, "my_prefix", LOGGER::debug);
 */
public final class GuardStateSupport {
  private static final ClassValue<List<GuardCall>> CACHE = new ClassValue<>() {
    @Override
    protected List<GuardCall> computeValue(Class<?> type) {
      List<GuardCall> calls = new ArrayList<>();
      for (Class<?> cls = type; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
        for (Method method : cls.getDeclaredMethods()) {
          if (!isGuardSetter(method)) continue;
          try {
            method.setAccessible(true);
            calls.add(new GuardCall(method));
          } catch (RuntimeException ignored) {}
        }
      }
      return List.copyOf(calls);
    }
  };

  private GuardStateSupport() {}

  /**
   * Public lightweight entry point for external hit code.
   * Discovers generic guard setters once for each runtime class, then reuses that metadata.
   */
  public static int clearDamageGuards(LivingEntity victim) {
    return clearDamageGuards(victim, "external_guard_clear", null);
  }

  /**
   * Public external entry point with optional logger.
   * The logger may be null; messages are already one-line diagnostic strings.
   */
  public static int clearDamageGuards(LivingEntity victim, String prefix, Consumer<String> logger) {
    if (victim == null) return 0;

    List<GuardCall> calls = CACHE.get(victim.getClass());
    int changed = 0;
    for (GuardCall call : calls) changed += call.invoke(victim, prefix, logger) ? 1 : 0;
    if (logger != null) {
      logger.accept(prefix + " guard_cache_used: class=" + victim.getClass().getName()
          + ", calls=" + calls.size() + ", invoked=" + changed);
    }
    return changed;
  }

  /** Existing DamageProbe integration overload. */
  public static int clearDamageGuards(LivingEntity victim, DamageProbeResult result, String prefix) {
    return clearDamageGuards(victim, prefix, result == null ? null : result::add);
  }

  /** Number of guard calls for this runtime class; discovers metadata if needed. */
  public static int cachedGuardCount(LivingEntity victim) {
    if (victim == null) return 0;
    return CACHE.get(victim.getClass()).size();
  }

  private static boolean isGuardSetter(Method method) {
    if (Modifier.isStatic(method.getModifiers())) return false;
    if (method.getParameterCount() != 1 || method.getReturnType() != void.class) return false;
    Class<?> type = method.getParameterTypes()[0];
    if (type != int.class && type != Integer.class) return false;

    String lower = normalize(method.getName());
    if (!lower.contains("set")) return false;
    if (lower.contains("death") || lower.contains("dead") || lower.contains("dying")) return false;

    return containsAny(lower,
        "cooldown", "cool", "invul", "immune", "hurt", "hit", "iframe", "iframes",
        "shield", "block", "guard", "stun", "parry", "hurtresistant", "resistanttime");
  }

  private static String describe(Method method) {
    return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
  }

  private static String normalize(String name) {
    return name.toLowerCase(Locale.ROOT).replace("$", "").replace("_", "");
  }

  private static boolean containsAny(String s, String... keys) {
    for (String key : keys) if (s.contains(key)) return true;
    return false;
  }

  private record GuardCall(Method method) {
    boolean invoke(LivingEntity victim, String prefix, Consumer<String> logger) {
      try {
        method.invoke(victim, 0);
        if (logger != null) {
          logger.accept(prefix + " clear_guard_int_state_cached: " + describe(method) + " -> 0");
        }
        return true;
      } catch (Throwable e) {
        if (logger != null) {
          logger.accept(prefix + " clear_guard_int_state error: " + describe(method) + ", error=" + e.getClass().getSimpleName());
        }
        return false;
      }
    }
  }
}
