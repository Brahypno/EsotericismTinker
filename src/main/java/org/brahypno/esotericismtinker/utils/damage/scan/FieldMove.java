package org.brahypno.esotericismtinker.utils.damage.scan;

public record FieldMove(boolean moved, float dealtEquivalent) {
    public static FieldMove failed() {
        return new FieldMove(false, 0.0F);
    }
}
