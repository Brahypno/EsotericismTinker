package org.brahypno.esotericismtinker.utils.damage.method;

import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Caches discovered high-confidence guard clearers per entity type.
 * Generic only: no target class names or mod-specific literals.
 *
 * External use:
 *   GuardStateSupport.clearDamageGuards(target);
 *   GuardStateSupport.clearDamageGuards(target, "my_prefix", LOGGER::debug);
 */
public final class GuardStateSupport {
  private static final Map<String, List<GuardCall>> CACHE = new HashMap<>();
  private static final Map<String, Set<String>> SIGNATURES = new HashMap<>();

  private GuardStateSupport() {}

  /**
   * Public lightweight entry point for external hit code.
   * Uses cached methods and also discovers new generic guard setters for this entity type.
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

    String key = entityTypeKey(victim);
    int changed = invokeCached(victim, key, prefix, logger);
    changed += discoverAndInvokeNew(victim, key, prefix, logger);
    return changed;
  }

  /** Existing DamageProbe integration overload. */
  public static int clearDamageGuards(LivingEntity victim, DamageProbeResult result, String prefix) {
    return clearDamageGuards(victim, prefix, result == null ? null : result::add);
  }

  /** Number of cached guard calls for an entity type, useful for diagnostics. */
  public static int cachedGuardCount(LivingEntity victim) {
    if (victim == null) return 0;
    List<GuardCall> calls = CACHE.get(entityTypeKey(victim));
    return calls == null ? 0 : calls.size();
  }

  private static int invokeCached(LivingEntity victim, String key, String prefix, Consumer<String> logger) {
    List<GuardCall> calls = CACHE.get(key);
    if (calls == null || calls.isEmpty()) return 0;

    int changed = 0;
    for (GuardCall call : calls) changed += call.invoke(victim, prefix, logger, true) ? 1 : 0;
    log(logger, prefix + " guard_cache_used: entityType=" + key + ", calls=" + calls.size() + ", invoked=" + changed);
    return changed;
  }

  private static int discoverAndInvokeNew(LivingEntity victim, String key, String prefix, Consumer<String> logger) {
    List<GuardCall> calls = CACHE.computeIfAbsent(key, ignored -> new ArrayList<>());
    Set<String> signatures = SIGNATURES.computeIfAbsent(key, ignored -> new HashSet<>());
    int changed = 0;

    Class<?> cls = victim.getClass();
    while (cls != null && cls != Object.class) {
      for (Method method : cls.getDeclaredMethods()) {
        if (!isGuardSetter(method)) continue;
        String signature = signature(method);
        if (!signatures.add(signature)) continue;

        try {
          method.setAccessible(true);
          GuardCall call = new GuardCall(method);
          calls.add(call);
          log(logger, prefix + " guard_cache_add: entityType=" + key + ", method=" + describe(method));
          changed += call.invoke(victim, prefix, logger, false) ? 1 : 0;
        } catch (Throwable e) {
          log(logger, prefix + " guard_cache_add error: " + describe(method) + ", error=" + e.getClass().getSimpleName());
        }
      }
      cls = cls.getSuperclass();
    }

    return changed;
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

  private static String entityTypeKey(LivingEntity victim) {
    try {
      return String.valueOf(victim.getType());
    } catch (Throwable ignored) {
      return victim.getClass().getName();
    }
  }

  private static String signature(Method method) {
    StringBuilder builder = new StringBuilder(method.getDeclaringClass().getName()).append('#').append(method.getName()).append('(');
    for (Class<?> parameter : method.getParameterTypes()) builder.append(parameter.getName()).append(';');
    return builder.append(')').toString();
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

  private static void log(Consumer<String> logger, String message) {
    if (logger != null) logger.accept(message);
  }

  private record GuardCall(Method method) {
    boolean invoke(LivingEntity victim, String prefix, Consumer<String> logger, boolean cached) {
      try {
        method.invoke(victim, 0);
        log(logger, prefix + " clear_guard_int_state" + (cached ? "_cached" : "") + ": " + describe(method) + " -> 0");
        return true;
      } catch (Throwable e) {
        log(logger, prefix + " clear_guard_int_state error: " + describe(method) + ", error=" + e.getClass().getSimpleName());
        return false;
      }
    }
  }
}
