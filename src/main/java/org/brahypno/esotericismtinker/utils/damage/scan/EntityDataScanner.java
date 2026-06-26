package org.brahypno.esotericismtinker.utils.damage.scan;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EntityDataScanner {
    private EntityDataScanner() {}

    public static List<DataCandidate> collect(LivingEntity victim, boolean absolute, int minScore) {
        List<DataCandidate> result = new ArrayList<>();
        Class<?> cls = victim.getClass();
        while (cls != null && cls != Object.class) {
            if (!absolute && (cls == LivingEntity.class || cls == Entity.class)) break;
            collectFromClass(victim, cls, result, minScore, absolute);
            cls = cls.getSuperclass();
        }
        result.sort(Comparator.comparingInt(DataCandidate::score).reversed());
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static void collectFromClass(LivingEntity victim, Class<?> cls, List<DataCandidate> result, int minScore, boolean absolute) {
        for (Field field : cls.getDeclaredFields()) {
            try {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                if (!EntityDataAccessor.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                EntityDataAccessor accessor = (EntityDataAccessor) field.get(null);
                Object value = victim.getEntityData().get(accessor);
                if (!(value instanceof Float) && !(value instanceof Integer)) continue;
                DataMode mode = DataMode.REMAINING;
                int score = value instanceof Float ? 38 : 30;
                if (absolute || score >= minScore) result.add(new DataCandidate(field.getName(), accessor, value, score, mode));
            } catch (Throwable ignored) {}
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static EntityDataMove move(LivingEntity victim, DataCandidate candidate, float amount, DamageProbeResult result) {
        try {
            Object before = victim.getEntityData().get(candidate.accessor());
            Object after = nextValue(before, amount, candidate.mode());
            if (after == null) return EntityDataMove.failed();
            victim.getEntityData().set((EntityDataAccessor) candidate.accessor(), after);
            Object confirmed = victim.getEntityData().get(candidate.accessor());
            boolean moved = movedAsDamage(before, confirmed, candidate.mode());
            float dealt = moved ? dealtEquivalent(before, confirmed, candidate.mode()) : 0.0F;
            result.add("entity_data_probe " + candidate.mode() + " " + candidate.name() + ": " + before + " -> " + confirmed + ", score=" + candidate.score() + ", movedDealt=" + dealt);
            return new EntityDataMove(moved, dealt);
        } catch (Throwable e) {
            result.add("entity_data_probe " + candidate.name() + " error: " + e.getClass().getSimpleName());
            return EntityDataMove.failed();
        }
    }

    private static Object nextValue(Object before, float amount, DataMode mode) {
        if (before instanceof Float f) return Float.isFinite(f) ? Math.max(0.0F, f - amount) : null;
        if (before instanceof Integer i) return Math.max(0, i - Math.max(1, (int) Math.ceil(amount)));
        return null;
    }

    private static boolean movedAsDamage(Object before, Object after, DataMode mode) {
        double b = ScannerUtil.asDouble(before);
        double a = ScannerUtil.asDouble(after);
        if (!Double.isFinite(b) || !Double.isFinite(a)) return false;
        return a < b;
    }

    private static float dealtEquivalent(Object before, Object after, DataMode mode) {
        double b = ScannerUtil.asDouble(before);
        double a = ScannerUtil.asDouble(after);
        if (!Double.isFinite(b) || !Double.isFinite(a)) return 0.0F;
        return (float) Math.max(0.0D, b - a);
    }
}
