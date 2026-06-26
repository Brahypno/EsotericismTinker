package org.brahypno.esotericismtinker.utils.damage;

public enum SetHealthFallbackMode {
    NEVER,
    IF_NO_PROGRESS,
    IF_NOT_ENOUGH;

    public boolean shouldFallback(float requested, float dealt, boolean authoritative) {
        return switch (this) {
            case NEVER -> false;
            case IF_NO_PROGRESS -> !authoritative && dealt <= DamageConstants.DAMAGE_EPS;
            case IF_NOT_ENOUGH -> dealt + DamageConstants.DAMAGE_EPS < requested;
        };
    }
}
