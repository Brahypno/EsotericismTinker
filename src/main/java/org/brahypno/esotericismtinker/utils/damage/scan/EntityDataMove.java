package org.brahypno.esotericismtinker.utils.damage.scan;

public record EntityDataMove(boolean moved, float dealtEquivalent) {
    public static EntityDataMove failed() {
        return new EntityDataMove(false, 0.0F);
    }
}
