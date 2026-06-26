package org.brahypno.esotericismtinker.utils.damage.reflect;

import java.util.ArrayList;
import java.util.List;

public record MethodInvokeResult(boolean invoked, boolean affected, List<String> lines) {
    public static MethodInvokeResult empty(String reason) {
        List<String> lines = new ArrayList<>();
        lines.add(reason);
        return new MethodInvokeResult(false, false, lines);
    }
}
