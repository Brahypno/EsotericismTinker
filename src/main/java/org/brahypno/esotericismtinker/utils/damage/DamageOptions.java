package org.brahypno.esotericismtinker.utils.damage;

public record DamageOptions(ScanLevel scanLevel, FinalKillPolicy killPolicy, boolean debug, int maxRetry, int maxAbsoluteFieldTries, int maxAbsoluteDataTries,
                            boolean mediumActuallyHurtFallback, SetHealthFallbackMode setHealthFallbackMode) {
    public static DamageOptions medium() {
        return new DamageOptions(ScanLevel.MEDIUM, FinalKillPolicy.NO_REMOVE, true, DamageConstants.MAX_RETRY_TOTAL, 0, 0, true,
                                 SetHealthFallbackMode.IF_NO_PROGRESS);
    }

    public static DamageOptions basicHitOnly() {
        return new DamageOptions(ScanLevel.BASIC_HIT, FinalKillPolicy.NO_REMOVE, true, 1, 0, 0, true, SetHealthFallbackMode.IF_NO_PROGRESS);
    }

    public static DamageOptions setHealthEquivalent() {
        return new DamageOptions(ScanLevel.SET_HEALTH_EQUIVALENT, FinalKillPolicy.NO_REMOVE, true, DamageConstants.MAX_RETRY_TOTAL, 0, 0, true,
                                 SetHealthFallbackMode.IF_NOT_ENOUGH);
    }

    public static DamageOptions mediumWithTrueSetHealthFallback() {
        return new DamageOptions(ScanLevel.MEDIUM, FinalKillPolicy.NO_REMOVE, true, DamageConstants.MAX_RETRY_TOTAL, 0, 0, true,
                                 SetHealthFallbackMode.IF_NOT_ENOUGH);
    }

    public static DamageOptions finalNoRemove() {
        return new DamageOptions(ScanLevel.FINAL_ABSOLUTE, FinalKillPolicy.NO_REMOVE, true, DamageConstants.MAX_RETRY_TOTAL,
                                 DamageConstants.MAX_ABSOLUTE_FIELD_TRIES, DamageConstants.MAX_ABSOLUTE_DATA_TRIES, true, SetHealthFallbackMode.IF_NOT_ENOUGH);
    }

    public static DamageOptions finalRemove() {
        return new DamageOptions(ScanLevel.FINAL_ABSOLUTE, FinalKillPolicy.REMOVE_AS_LAST_RESORT, true, DamageConstants.MAX_RETRY_TOTAL,
                                 DamageConstants.MAX_ABSOLUTE_FIELD_TRIES, DamageConstants.MAX_ABSOLUTE_DATA_TRIES, true, SetHealthFallbackMode.IF_NOT_ENOUGH);
    }

    public DamageOptions withMediumActuallyHurtFallback(boolean enabled) {
        return new DamageOptions(scanLevel, killPolicy, debug, maxRetry, maxAbsoluteFieldTries, maxAbsoluteDataTries, enabled, setHealthFallbackMode);
    }

    public DamageOptions withSetHealthFallbackMode(SetHealthFallbackMode mode) {
        return new DamageOptions(scanLevel, killPolicy, debug, maxRetry, maxAbsoluteFieldTries, maxAbsoluteDataTries, mediumActuallyHurtFallback, mode);
    }
}
