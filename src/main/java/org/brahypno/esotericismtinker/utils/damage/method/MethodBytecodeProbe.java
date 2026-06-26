package org.brahypno.esotericismtinker.utils.damage.method;

import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Best-effort bytecode reader for method candidates.
 * Diagnostic only: in ModLauncher/Mixin environments, class resources may not be transformed live bytes.
 */
public final class MethodBytecodeProbe {
  private static final int MAX_LINES_PER_METHOD = 32;

  private MethodBytecodeProbe() {}

  static void inspectPairs(Class<?> runtimeClass, List<MethodHealthProbe.Pair> pairs, DamageProbeResult result, int maxPairs) {
    result.add("generic_method_bytecode_probe_begin: runtimeClass=" + runtimeClass.getName()
        + ", note=best_effort_resource_bytes_may_not_be_transformed");
    Set<Method> inspected = new HashSet<>();
    int count = 0;
    for (MethodHealthProbe.Pair pair : pairs) {
      if (count++ >= maxPairs) break;
      inspectMethod(pair.getter().method(), inspected, result);
      inspectMethod(pair.setter().method(), inspected, result);
    }
  }

  private static void inspectMethod(Method method, Set<Method> inspected, DamageProbeResult result) {
    if (!inspected.add(method)) return;

    try (InputStream stream = method.getDeclaringClass().getResourceAsStream(method.getDeclaringClass().getSimpleName() + ".class")) {
      if (stream == null) {
        result.add("generic_method_bytecode_unavailable: " + MethodHealthProbe.describe(method) + ", reason=no_class_resource");
        return;
      }

      ClassReader reader = new ClassReader(stream);
      String descriptor = Type.getMethodDescriptor(method);
      List<String> lines = new ArrayList<>();

      reader.accept(new ClassVisitor(Opcodes.ASM9) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
          if (!name.equals(method.getName()) || !desc.equals(descriptor)) return null;
          return new MethodVisitor(Opcodes.ASM9) {
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
              if (lines.size() >= MAX_LINES_PER_METHOD) return;
              if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC || opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC)
                lines.add(opcodeName(opcode) + " " + owner.replace('/', '.') + "#" + name + " " + descriptor);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
              if (lines.size() >= MAX_LINES_PER_METHOD) return;
              if (looksRelevant(name)) lines.add(opcodeName(opcode) + " " + owner.replace('/', '.') + "#" + name + descriptor);
            }
          };
        }
      }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

      if (lines.isEmpty()) {
        result.add("generic_method_bytecode: " + MethodHealthProbe.describe(method) + ", descriptor=" + descriptor + ", relevantOps=0");
        return;
      }

      result.add("generic_method_bytecode: " + MethodHealthProbe.describe(method) + ", descriptor=" + descriptor + ", relevantOps=" + lines.size());
      for (String line : lines) result.add("generic_method_bytecode_op: " + MethodHealthProbe.describe(method) + " :: " + line);
    } catch (Throwable e) {
      result.add("generic_method_bytecode_error: " + MethodHealthProbe.describe(method)
          + ", error=" + e.getClass().getSimpleName()
          + ": " + e.getMessage());
    }
  }

  private static boolean looksRelevant(String name) {
    String lower = name.toLowerCase();
    return lower.contains("health")
        || lower.contains("hurt")
        || lower.contains("damage")
        || lower.contains("life")
        || lower.contains("hp")
        || lower.contains("death")
        || lower.contains("dead")
        || lower.contains("invul")
        || lower.contains("cooldown")
        || lower.contains("set")
        || lower.contains("get");
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
}
