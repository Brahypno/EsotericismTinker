package org.brahypno.esotericismtinker.utils.damage.reflect;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DamageMethodInvoker {
    private DamageMethodInvoker() {}

    public static MethodInvokeResult invokeActuallyHurt(LivingEntity victim, DamageSource source, float amount) {
        if (victim == null || source == null || amount <= 0.0F)
            return MethodInvokeResult.empty("invokeActuallyHurt abort: invalid args");
        List<String> lines = new ArrayList<>();
        boolean invoked = false;
        boolean affected = false;
        MethodInvokeResult special = invokeLivingEntityActuallyHurtSpecial(victim, source, amount);
        lines.addAll(special.lines());
        invoked |= special.invoked();
        affected |= special.affected();
        if (affected)
            return new MethodInvokeResult(true, true, lines);
        float beforeHealth = victim.getHealth();
        float beforeAbsorption = victim.getAbsorptionAmount();
        int beforeDeathTime = victim.deathTime;
        boolean beforeAlive = victim.isAlive();
        boolean beforeRemoved = victim.isRemoved();
        for (Method method : collectActuallyHurtMethods(victim)) {
            try {
                method.setAccessible(true);
                lines.add("invokeActuallyHurt reflect try: " + method.getDeclaringClass().getName() + "#" + method.getName());
                method.invoke(victim, source, amount);
                invoked = true;
                float afterHealth = victim.getHealth();
                float afterAbsorption = victim.getAbsorptionAmount();
                boolean nowAffected = afterHealth < beforeHealth || afterAbsorption < beforeAbsorption || victim.deathTime > beforeDeathTime ||
                                      beforeAlive != victim.isAlive() || beforeRemoved != victim.isRemoved();
                lines.add("invokeActuallyHurt reflect result: method=" + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + ", health " +
                          beforeHealth + " -> " + afterHealth + ", absorption " + beforeAbsorption + " -> " + afterAbsorption + ", deathTime " +
                          beforeDeathTime + " -> " + victim.deathTime + ", alive " + beforeAlive + " -> " + victim.isAlive() + ", removed " + beforeRemoved +
                          " -> " + victim.isRemoved() + ", affected=" + nowAffected);
                affected |= nowAffected;
                if (affected)
                    break;
            }
            catch (Throwable e) {
                lines.add("invokeActuallyHurt reflect error: " + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + ", error=" +
                          e.getClass().getSimpleName());
            }
        }
        return new MethodInvokeResult(invoked, affected, lines);
    }

    public static MethodInvokeResult invokeLivingEntitySetHealthSpecial(LivingEntity victim, float targetHealth) {
        if (victim == null)
            return MethodInvokeResult.empty("invokeLivingEntitySetHealthSpecial abort: null victim");
        List<String> lines = new ArrayList<>();
        float beforeHealth = victim.getHealth();
        float beforeAbsorption = victim.getAbsorptionAmount();
        int beforeDeathTime = victim.deathTime;
        boolean beforeAlive = victim.isAlive();
        boolean beforeRemoved = victim.isRemoved();
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(LivingEntity.class, MethodHandles.lookup());
            MethodHandle handle = findSpecialSetHealthHandle(lookup);
            if (handle == null)
                return MethodInvokeResult.empty("invokeLivingEntitySetHealthSpecial skipped: no setHealth/m_21153_ handle");
            lines.add("invokeLivingEntitySetHealthSpecial try: before=" + beforeHealth + ", target=" + targetHealth);
            handle.invoke(victim, targetHealth);
            float afterHealth = victim.getHealth();
            float afterAbsorption = victim.getAbsorptionAmount();
            boolean affected =
                    afterHealth < beforeHealth || afterAbsorption < beforeAbsorption || victim.deathTime > beforeDeathTime || beforeAlive != victim.isAlive() ||
                    beforeRemoved != victim.isRemoved();
            lines.add("invokeLivingEntitySetHealthSpecial result: health " + beforeHealth + " -> " + afterHealth + ", absorption " + beforeAbsorption + " -> " +
                      afterAbsorption + ", deathTime " + beforeDeathTime + " -> " + victim.deathTime + ", alive " + beforeAlive + " -> " + victim.isAlive() +
                      ", removed " + beforeRemoved + " -> " + victim.isRemoved() + ", affected=" + affected);
            return new MethodInvokeResult(true, affected, lines);
        }
        catch (Throwable e) {
            lines.add("invokeLivingEntitySetHealthSpecial error: " + e.getClass().getSimpleName());
            return new MethodInvokeResult(false, false, lines);
        }
    }

    private static MethodInvokeResult invokeLivingEntityActuallyHurtSpecial(LivingEntity victim, DamageSource source, float amount) {
        List<String> lines = new ArrayList<>();
        float beforeHealth = victim.getHealth();
        float beforeAbsorption = victim.getAbsorptionAmount();
        int beforeDeathTime = victim.deathTime;
        boolean beforeAlive = victim.isAlive();
        boolean beforeRemoved = victim.isRemoved();
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(LivingEntity.class, MethodHandles.lookup());
            MethodHandle handle = findSpecialActuallyHurtHandle(lookup);
            if (handle == null)
                return MethodInvokeResult.empty("invokeActuallyHurt special skipped: no actuallyHurt/m_6475_ handle");
            lines.add("invokeActuallyHurt special try: LivingEntity actuallyHurt");
            handle.invoke(victim, source, amount);
            float afterHealth = victim.getHealth();
            float afterAbsorption = victim.getAbsorptionAmount();
            boolean affected =
                    afterHealth < beforeHealth || afterAbsorption < beforeAbsorption || victim.deathTime > beforeDeathTime || beforeAlive != victim.isAlive() ||
                    beforeRemoved != victim.isRemoved();
            lines.add("invokeActuallyHurt special result: health " + beforeHealth + " -> " + afterHealth + ", absorption " + beforeAbsorption + " -> " +
                      afterAbsorption + ", deathTime " + beforeDeathTime + " -> " + victim.deathTime + ", alive " + beforeAlive + " -> " + victim.isAlive() +
                      ", removed " + beforeRemoved + " -> " + victim.isRemoved() + ", affected=" + affected);
            return new MethodInvokeResult(true, affected, lines);
        }
        catch (Throwable e) {
            lines.add("invokeActuallyHurt special error: " + e.getClass().getSimpleName());
            return new MethodInvokeResult(false, false, lines);
        }
    }

    private static MethodHandle findSpecialActuallyHurtHandle(MethodHandles.Lookup lookup) {
        try {
            return lookup.findSpecial(LivingEntity.class, "actuallyHurt", MethodType.methodType(void.class, DamageSource.class, float.class),
                                      LivingEntity.class);
        }
        catch (Throwable ignored) {}
        try {
            return lookup.findSpecial(LivingEntity.class, "m_6475_", MethodType.methodType(void.class, DamageSource.class, float.class), LivingEntity.class);
        }
        catch (Throwable ignored) {}
        return null;
    }

    private static MethodHandle findSpecialSetHealthHandle(MethodHandles.Lookup lookup) {
        try {
            return lookup.findSpecial(LivingEntity.class, "setHealth", MethodType.methodType(void.class, float.class), LivingEntity.class);
        }
        catch (Throwable ignored) {}
        try {
            return lookup.findSpecial(LivingEntity.class, "m_21153_", MethodType.methodType(void.class, float.class), LivingEntity.class);
        }
        catch (Throwable ignored) {}
        return null;
    }

    private static List<Method> collectActuallyHurtMethods(LivingEntity victim) {
        List<Method> methods = new ArrayList<>();
        Class<?> cls = victim.getClass();
        while (cls != null && cls != Object.class) {
            for (Method method : cls.getDeclaredMethods()) {
                if (method.getReturnType() != void.class)
                    continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 2)
                    continue;
                if (params[0] != DamageSource.class)
                    continue;
                if (params[1] != float.class && params[1] != Float.TYPE)
                    continue;
                String name = method.getName();
                if (name.equals("actuallyHurt") || name.equals("m_6475_") || name.toLowerCase(Locale.ROOT).contains("actuallyhurt"))
                    methods.add(method);
            }
            cls = cls.getSuperclass();
        }
        methods.sort(Comparator.comparingInt(m -> actuallyHurtMethodScore((Method) m, victim)).reversed());
        return methods;
    }

    private static int actuallyHurtMethodScore(Method method, LivingEntity victim) {
        int score = 0;
        if (method.getDeclaringClass() == LivingEntity.class)
            score += 100;
        if (method.getName().equals("actuallyHurt"))
            score += 100;
        if (method.getName().equals("m_6475_"))
            score += 80;
        if (method.getDeclaringClass() == victim.getClass())
            score += 40;
        return score;
    }
}
