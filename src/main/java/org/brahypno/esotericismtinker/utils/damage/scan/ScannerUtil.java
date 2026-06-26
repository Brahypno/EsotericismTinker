package org.brahypno.esotericismtinker.utils.damage.scan;

final class ScannerUtil {
    private ScannerUtil() {}

    static boolean containsAny(String s, String... keys) {
        for (String key : keys) if (s.contains(key)) return true;
        return false;
    }

    static double asDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return Double.NaN;
    }
}
