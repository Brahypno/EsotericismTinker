package org.brahypno.esotericismtinker.utils.damage.method;

import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Best-effort diagnostic bytecode attribution for high-suspicion injected methods. */
public final class MixinMethodInspector {
  private static final int MAX_METHODS = 32;
  private static final int MAX_OPS_PER_METHOD = 24;

  private MixinMethodInspector() {}

  public static void inspect(LivingEntity victim, DamageProbeResult result) {
    if (victim == null) return;

    List<Candidate> candidates = collect(victim.getClass());
    result.add("mixin_method_inspector begin: runtimeClass=" + victim.getClass().getName()
        + ", candidates=" + candidates.size()
        + ", note=best_effort_resource_bytes_may_not_be_transformed; reports what suspicious methods read/write/call, not guaranteed callers");

    int shown = 0;
    Set<String> inspected = new HashSet<>();
    for (Candidate candidate : candidates) {
      if (shown++ >= MAX_METHODS) break;
      if (!inspected.add(signature(candidate.method()))) continue;
      inspectMethod(candidate, result);
    }
  }

  private static List<Candidate> collect(Class<?> start) {
    List<Candidate> list = new ArrayList<>();
    Class<?> cls = start;
    while (cls != null && cls != Object.class) {
      for (Method method : cls.getDeclaredMethods()) {
        if (Modifier.isStatic(method.getModifiers())) continue;
        int score = score(method);
        if (score <= 0) continue;
        try { method.setAccessible(true); } catch (Throwable ignored) { }
        list.add(new Candidate(method, score));
      }
      cls = cls.getSuperclass();
    }
    list.sort(Comparator.comparingInt(Candidate::score).reversed().thenComparing(c -> describe(c.method())));
    return list;
  }

  private static int score(Method method) {
    String lower = method.getName().toLowerCase(Locale.ROOT);
    int score = 0;
    if (containsAny(lower, "modify", "redirect", "wrapoperation", "handler", "localvar")) score += 24;
    if (containsAny(lower, "actuallyhurt", "hurt", "damage", "sethealth", "healthvalue", "getdamage")) score += 42;
    if (containsAny(lower, "canhurt", "cancelhurt", "invul", "cooldown", "hitcooldown", "guard")) score += 30;
    if (containsAny(lower, "sound", "loot", "drop", "light", "render")) score -= 32;

    Class<?> returnType = method.getReturnType();
    if (returnType == float.class || returnType == Float.class || returnType == boolean.class || returnType == Boolean.class) score += 12;
    for (Class<?> parameter : method.getParameterTypes()) {
      String name = parameter.getName().toLowerCase(Locale.ROOT);
      if (name.contains("damagesource") || name.contains("livingentity")) score += 8;
      if (parameter == float.class || parameter == Float.class) score += 6;
    }
    return score;
  }

  private static void inspectMethod(Candidate candidate, DamageProbeResult result) {
    Method method = candidate.method();
    result.add("mixin_method_ranked: " + describe(method)
        + ", score=" + candidate.score()
        + ", return=" + method.getReturnType().getSimpleName()
        + ", params=" + params(method));

    try (InputStream stream = method.getDeclaringClass().getResourceAsStream(method.getDeclaringClass().getSimpleName() + ".class")) {
      if (stream == null) {
        result.add("mixin_method_bytecode_unavailable: " + describe(method) + ", reason=no_class_resource");
        return;
      }

      ClassReader reader = new ClassReader(stream);
      String descriptor = Type.getMethodDescriptor(method);
      List<String> ops = new ArrayList<>();
      reader.accept(new ClassVisitor(Opcodes.ASM9) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
          if (!name.equals(method.getName()) || !desc.equals(descriptor)) return null;
          return new MethodVisitor(Opcodes.ASM9) {
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
              if (ops.size() >= MAX_OPS_PER_METHOD) return;
              if (isFieldRelevant(name)) ops.add(opcodeName(opcode) + " " + owner.replace('/', '.') + "#" + name + " " + descriptor);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
              if (ops.size() >= MAX_OPS_PER_METHOD) return;
              if (isMethodRelevant(name)) ops.add(opcodeName(opcode) + " " + owner.replace('/', '.') + "#" + name + descriptor);
            }
          };
        }
      }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

      if (ops.isEmpty()) {
        result.add("mixin_method_bytecode: " + describe(method) + ", descriptor=" + descriptor + ", relevantOps=0");
        return;
      }

      result.add("mixin_method_bytecode: " + describe(method) + ", descriptor=" + descriptor + ", relevantOps=" + ops.size());
      for (String op : ops) result.add("mixin_method_op: " + describe(method) + " :: " + op);
    } catch (Throwable e) {
      result.add("mixin_method_bytecode_error: " + describe(method) + ", error=" + e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }

  private static boolean isFieldRelevant(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    return containsAny(lower, "health", "hurt", "damage", "invul", "cooldown", "shield", "guard", "cap", "phase", "death", "armor", "absorb", "immune");
  }

  private static boolean isMethodRelevant(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    return containsAny(lower, "health", "hurt", "damage", "invul", "cooldown", "canhurt", "set", "get", "cap", "phase", "death", "armor", "absorb", "immune");
  }

  private static String params(Method method) {
    List<String> names = new ArrayList<>();
    for (Class<?> parameter : method.getParameterTypes()) names.add(parameter.getSimpleName());
    return names.toString();
  }

  private static String signature(Method method) {
    return method.getDeclaringClass().getName() + '#' + method.getName() + Type.getMethodDescriptor(method);
  }

  private static String describe(Method method) {
    return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
  }

  private static String opcodeName(int opcode) {
    return switch (opcode) {
      case Opcodes.GETFIELD -> "GETFIELD";
      case Opcodes.PUTFIELD -> "PUTFIELD";
      case Opcodes.GETSTATIC -> "GETSTATIC";
      case Opcodes.PUTSTATIC -> "PUTSTATIC";
      case Opcodes.INVOKEVIRTUAL -> "INVOKEVIRTUAL";
      case Opcodes.INVOKEINTERFACE -> "INVOKEINTERFACE";
      case Opcodes.INVOKESPECIAL -> "INVOKESPECIAL";
      case Opcodes.INVOKESTATIC -> "INVOKESTATIC";
      default -> "OP" + opcode;
    };
  }

  private static boolean containsAny(String s, String... keys) {
    for (String key : keys) if (s.contains(key)) return true;
    return false;
  }

  private record Candidate(Method method, int score) {}
}
