package org.brahypno.esotericismtinker.utils.damage.scan;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class FieldScanner {
    private FieldScanner() {}

    public static List<FieldCandidate> collect(LivingEntity victim, boolean absolute, int minScore) {
        List<FieldCandidate> result = new ArrayList<>();
        Class<?> cls = victim.getClass();
        while (cls != null && cls != Object.class) {
            if (!absolute && (cls == LivingEntity.class || cls == Entity.class)) break;
            collectFromClass(victim, cls, result, minScore, absolute);
            cls = cls.getSuperclass();
        }
        result.sort(Comparator.comparingInt(FieldCandidate::score).reversed());
        return result;
    }

    private static void collectFromClass(LivingEntity victim, Class<?> cls, List<FieldCandidate> result, int minScore, boolean absolute) {
        for (Field field : cls.getDeclaredFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;
                if (!isNumericField(field)) continue;
                field.setAccessible(true);
                Object value = field.get(victim);
                DataMode mode = guessMode(field.getName());
                int score = scoreName(field.getName(), value, mode);
                if (absolute || score >= minScore) result.add(new FieldCandidate(field, field.getName(), value, score, mode));
            } catch (Throwable ignored) {}
        }
    }

    public static FieldMove move(LivingEntity victim, FieldCandidate candidate, float amount, DamageProbeResult result) {
        try {
            Object before = candidate.field().get(victim);
            Object after = nextValue(before, amount, candidate.mode());
            if (after == null) return FieldMove.failed();
            candidate.field().set(victim, after);
            Object confirmed = candidate.field().get(victim);
            boolean moved = movedAsDamage(before, confirmed, candidate.mode());
            float dealt = moved ? dealtEquivalent(before, confirmed, candidate.mode()) : 0.0F;
            result.add("private_field_probe " + candidate.mode() + " " + candidate.field().getDeclaringClass().getSimpleName() + "#" + candidate.name() + ": " + before + " -> " + confirmed + ", score=" + candidate.score() + ", movedDealt=" + dealt);
            return new FieldMove(moved, dealt);
        } catch (Throwable e) {
            result.add("private_field_probe " + candidate.name() + " error: " + e.getClass().getSimpleName());
            return FieldMove.failed();
        }
    }

    public static boolean isNumericField(Field field) {
        Class<?> type = field.getType();
        return type == float.class || type == Float.class || type == double.class || type == Double.class || type == int.class || type == Integer.class || type == long.class || type == Long.class;
    }

    public static Object highValue(Object before) {
        if (before instanceof Float) return 1000000.0F;
        if (before instanceof Double) return 1000000.0D;
        if (before instanceof Integer) return 1000000;
        if (before instanceof Long) return 1000000L;
        return null;
    }

    public static Object zeroValue(Object before) {
        if (before instanceof Float) return 0.0F;
        if (before instanceof Double) return 0.0D;
        if (before instanceof Integer) return 0;
        if (before instanceof Long) return 0L;
        return null;
    }

    private static DataMode guessMode(String rawName) {
        String name = rawName.toUpperCase(Locale.ROOT);
        if (ScannerUtil.containsAny(name, "TOTALDAMAGETAKEN", "TOTAL_DAMAGE_TAKEN", "DAMAGETAKEN", "DAMAGE_TAKEN", "TAKENDAMAGE", "TAKEN_DAMAGE", "HURT_TAKEN", "WOUND_TAKEN", "INJURY_TAKEN", "DAMAGE", "HURT", "WOUND", "INJURY", "DMG")) return DataMode.TAKEN;
        return DataMode.REMAINING;
    }

    private static int scoreName(String rawName, Object value, DataMode mode) {
        String name = rawName.toUpperCase(Locale.ROOT);
        int score = value instanceof Float || value instanceof Double ? 20 : 12;
        if (mode == DataMode.REMAINING && ScannerUtil.containsAny(name, "HEALTH", "HP", "LIFE", "VITAL")) score += 75;
        if (mode == DataMode.REMAINING && ScannerUtil.containsAny(name, "SHIELD", "BARRIER", "CORE", "PART")) score += 35;
        if (mode == DataMode.TAKEN && ScannerUtil.containsAny(name, "DAMAGE", "HURT", "WOUND", "INJURY", "DMG")) score += 35;
        if (ScannerUtil.containsAny(name, "ANIM", "TIMER", "TIME", "POSE", "FLAGS", "FLAG", "STATE", "MODE", "VARIANT", "TYPE", "ID", "PROGRESS", "LAST", "CLIENT", "TARGET", "ATTACK")) score -= 60;
        if (ScannerUtil.containsAny(name, "COOLDOWN", "LOCK")) score -= 40;
        if (ScannerUtil.containsAny(name, "MAX", "CAP", "LIMIT", "MULTIPLIER")) score -= 35;
        double d = ScannerUtil.asDouble(value);
        if (Double.isFinite(d) && d >= 0.0D && d <= 100000.0D) score += 8;
        else score -= 30;
        return score;
    }

    private static Object nextValue(Object before, float amount, DataMode mode) {
        if (before instanceof Float f) return Float.isFinite(f) ? (mode == DataMode.REMAINING ? Math.max(0.0F, f - amount) : Math.min(100000.0F, f + amount)) : null;
        if (before instanceof Double d) return Double.isFinite(d) ? (mode == DataMode.REMAINING ? Math.max(0.0D, d - amount) : Math.min(100000.0D, d + amount)) : null;
        if (before instanceof Integer i) return mode == DataMode.REMAINING ? Math.max(0, i - Math.max(1, (int) Math.ceil(amount))) : Math.min(100000, i + Math.max(1, (int) Math.ceil(amount)));
        if (before instanceof Long l) return mode == DataMode.REMAINING ? Math.max(0L, l - Math.max(1L, (long) Math.ceil(amount))) : Math.min(100000L, l + Math.max(1L, (long) Math.ceil(amount)));
        return null;
    }

    private static boolean movedAsDamage(Object before, Object after, DataMode mode) {
        double b = ScannerUtil.asDouble(before);
        double a = ScannerUtil.asDouble(after);
        if (!Double.isFinite(b) || !Double.isFinite(a)) return false;
        return mode == DataMode.REMAINING ? a < b : a > b;
    }

    private static float dealtEquivalent(Object before, Object after, DataMode mode) {
        double b = ScannerUtil.asDouble(before);
        double a = ScannerUtil.asDouble(after);
        if (!Double.isFinite(b) || !Double.isFinite(a)) return 0.0F;
        return (float) Math.max(0.0D, mode == DataMode.REMAINING ? b - a : a - b);
    }
}
