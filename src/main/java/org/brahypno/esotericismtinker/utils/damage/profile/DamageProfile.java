package org.brahypno.esotericismtinker.utils.damage.profile;

import java.lang.reflect.Field;
import java.util.*;

public final class DamageProfile {
    private static final int LEARNED_FIELD_MAX_FAILURES = 2;
    private static final int ZERO_GATE_THRESHOLD = 3;
    private final String key;
    private final EnumMap<ProbeStepId, StepMemory> steps = new EnumMap<>(ProbeStepId.class);
    private final Set<String> rejectedHealthFields = new HashSet<>();
    private final Set<String> rejectedCapFields = new HashSet<>();
    private final List<String> notes = new ArrayList<>();
    private Field learnedHealthField;
    private Field learnedCapField;
    private int learnedHealthFieldFailures;
    private int learnedCapFieldFailures;
    private boolean healthFieldExplored;
    private boolean capSourceExplored;
    private boolean suspectExternalCap;
    private boolean suspectBytecodeOrEventCap;
    private float observedCap = -1.0F;
    private int capHits;
    private int zeroGateHits;
    private int failedLethalCount;
    private int attemptId;
    private int lastZeroGateAttempt = -1;

    public DamageProfile(String key) {
        this.key = key;
        for (ProbeStepId id : ProbeStepId.values())
            steps.put(id, new StepMemory());
    }

    public String key() {return key;}

    public StepMemory step(ProbeStepId id) {return steps.get(id);}

    public boolean shouldSkip(ProbeStepId id) {
        if (id == ProbeStepId.LEARNED_HEALTH_FIELD)
            return learnedHealthField == null || learnedHealthFieldFailures >= LEARNED_FIELD_MAX_FAILURES;
        if (id == ProbeStepId.LEARNED_CAP_FIELD)
            return learnedCapField == null || learnedCapFieldFailures >= LEARNED_FIELD_MAX_FAILURES;
        return step(id).shouldSkip();
    }

    public void beginAttempt() {
        attemptId++;
    }

    public void recordStep(ProbeStepId id, boolean success, boolean authoritative, float dealt) {
        step(id).record(success, authoritative, dealt);
    }

    public void observePossibleCap(float requested, float dealt) {
        if (requested <= 0.01F)
            return;
        if (dealt <= 0.01F){
            observeZeroGateOncePerAttempt("possible cap dealt zero");
            return;
        }
        if (dealt + 0.999F >= requested)
            return;
        if (observedCap < 0.0F){
            observedCap = dealt;
            capHits = 1;
            return;
        }
        if (Math.abs(observedCap - dealt) <= Math.max(0.25F, observedCap * 0.05F)){
            capHits++;
            observedCap = observedCap * 0.75F + dealt * 0.25F;
        }else if (dealt < observedCap){
            observedCap = dealt;
            capHits = Math.max(1, capHits - 1);
        }
    }

    public void observeZeroGate(String reason) {
        observeZeroGateOncePerAttempt(reason);
    }

    public void observeZeroGateOncePerAttempt(String reason) {
        if (lastZeroGateAttempt == attemptId)
            return;
        lastZeroGateAttempt = attemptId;
        zeroGateHits++;
        addNote("zero gate observed: " + reason + ", hits=" + zeroGateHits);
    }

    public boolean stableDamageCap() {return capHits >= 2 && observedCap > 0.01F;}

    public boolean stableZeroGate() {return zeroGateHits >= ZERO_GATE_THRESHOLD;}

    public float observedCap() {return observedCap;}

    public int capHits() {return capHits;}

    public int zeroGateHits() {return zeroGateHits;}

    public Field learnedHealthField() {return learnedHealthField;}

    public void learnHealthField(Field field) {
        if (field == null || isRejectedHealthField(field))
            return;
        this.learnedHealthField = field;
        this.learnedHealthFieldFailures = 0;
        addNote("learned health field " + describeField(field));
    }

    public Field learnedCapField() {return learnedCapField;}

    public void learnCapField(Field field) {
        if (field == null || isRejectedCapField(field))
            return;
        this.learnedCapField = field;
        this.learnedCapFieldFailures = 0;
        addNote("learned cap field " + describeField(field));
    }

    public void recordLearnedHealthFieldResult(boolean authoritative, Field field, String reason) {
        if (field == null)
            return;
        if (authoritative){
            learnedHealthFieldFailures = 0;
            return;
        }
        learnedHealthFieldFailures++;
        addNote("learned health field failed " + describeField(field) + ": " + reason + ", failures=" + learnedHealthFieldFailures);
        if (learnedHealthFieldFailures >= LEARNED_FIELD_MAX_FAILURES)
            rejectLearnedHealthField(reason);
    }

    public void recordLearnedCapFieldResult(boolean authoritative, Field field, String reason) {
        if (field == null)
            return;
        if (authoritative){
            learnedCapFieldFailures = 0;
            return;
        }
        learnedCapFieldFailures++;
        addNote("learned cap field failed " + describeField(field) + ": " + reason + ", failures=" + learnedCapFieldFailures);
        if (learnedCapFieldFailures >= LEARNED_FIELD_MAX_FAILURES)
            rejectLearnedCapField(reason);
    }

    public void rejectLearnedHealthField(String reason) {
        if (learnedHealthField == null)
            return;
        rejectedHealthFields.add(fieldKey(learnedHealthField));
        addNote("reject health field " + describeField(learnedHealthField) + ": " + reason);
        learnedHealthField = null;
        learnedHealthFieldFailures = 0;
    }

    public void rejectLearnedCapField(String reason) {
        if (learnedCapField == null)
            return;
        rejectedCapFields.add(fieldKey(learnedCapField));
        addNote("reject cap field " + describeField(learnedCapField) + ": " + reason);
        learnedCapField = null;
        learnedCapFieldFailures = 0;
    }

    public void rejectHealthField(Field field, String reason) {
        if (field == null)
            return;
        rejectedHealthFields.add(fieldKey(field));
        addNote("reject health candidate " + describeField(field) + ": " + reason);
    }

    public void rejectCapField(Field field, String reason) {
        if (field == null)
            return;
        rejectedCapFields.add(fieldKey(field));
        addNote("reject cap candidate " + describeField(field) + ": " + reason);
    }

    public boolean isRejectedHealthField(Field field) {return rejectedHealthFields.contains(fieldKey(field));}

    public boolean isRejectedCapField(Field field) {return rejectedCapFields.contains(fieldKey(field));}

    public boolean healthFieldExplored() {return healthFieldExplored;}

    public void markHealthFieldExplored() {this.healthFieldExplored = true;}

    public boolean capSourceExplored() {return capSourceExplored;}

    public void markCapSourceExplored() {this.capSourceExplored = true;}

    public void requestCapSourceRecheck(String reason) {
        this.capSourceExplored = false;
        addNote("request cap source recheck: " + reason);
    }

    public boolean suspectExternalCap() {return suspectExternalCap;}

    public void markSuspectExternalCap(String reason) {
        this.suspectExternalCap = true;
        addNote("suspect external cap: " + reason);
    }

    public boolean suspectBytecodeOrEventCap() {return suspectBytecodeOrEventCap;}

    public void markSuspectBytecodeOrEventCap(String reason) {
        this.suspectBytecodeOrEventCap = true;
        addNote("suspect bytecode/event cap: " + reason);
    }

    public void recordFailedLethal() {
        failedLethalCount++;
        if (failedLethalCount >= 3 && !stableDamageCap())
            observeZeroGateOncePerAttempt("failed lethal attempt");
    }

    public boolean corePathsFailed() {
        return failedEnough(ProbeStepId.BASIC_HANDLER)
               && failedEnough(ProbeStepId.RAW_SET_HEALTH)
               && failedEnough(ProbeStepId.SUPER_SET_HEALTH)
               && failedEnough(ProbeStepId.HARD_SET_HEALTH_ZERO)
               && failedEnough(ProbeStepId.FORCE_DIE);
    }

    public boolean absolutePathsFailed() {
        return failedEnough(ProbeStepId.ENTITY_DATA_ABSOLUTE)
               && failedEnough(ProbeStepId.PRIVATE_FIELD_ABSOLUTE);
    }

    public boolean allKnownTerminalPathsFailed() {
        return corePathsFailed() && absolutePathsFailed();
    }

    private boolean failedEnough(ProbeStepId id) {
        StepMemory memory = step(id);
        return memory.calls() >= 3 && memory.successes() == 0 && memory.authoritativeHits() == 0;
    }

    public ProbeDirective nextDirective() {
        if (learnedHealthField != null && learnedHealthFieldFailures < LEARNED_FIELD_MAX_FAILURES)
            return ProbeDirective.TRY_LEARNED_HEALTH_FIELD;
        if (learnedCapField != null && learnedCapFieldFailures < LEARNED_FIELD_MAX_FAILURES)
            return ProbeDirective.TRY_LEARNED_CAP_FIELD;
        if (!healthFieldExplored)
            return ProbeDirective.EXPLORE_HEALTH_BACKING;
        if (!capSourceExplored && shouldExploreCapSource())
            return ProbeDirective.EXPLORE_CAP_SOURCE;
        if (shouldSuspectExternal())
            return ProbeDirective.SUSPECT_EXTERNAL_CAP;
        return ProbeDirective.NORMAL;
    }

    private boolean shouldExploreCapSource() {
        return stableDamageCap() || stableZeroGate() || corePathsFailed() || failedLethalCount >= 2;
    }

    private boolean shouldSuspectExternal() {
        return suspectExternalCap
               || suspectBytecodeOrEventCap
               || (stableZeroGate() && capSourceExplored && learnedCapField == null)
               || (capSourceExplored && learnedCapField == null && learnedHealthField == null && allKnownTerminalPathsFailed())
               || (failedLethalCount >= 8 && allKnownTerminalPathsFailed());
    }

    public void addNote(String note) {
        if (notes.size() < 48)
            notes.add(note);
    }

    public String summary() {
        return "profile=" + key
               + ", directive=" + nextDirective()
               + ", observedCap=" + observedCap
               + ", capHits=" + capHits
               + ", zeroGateHits=" + zeroGateHits
               + ", healthField=" + describeField(learnedHealthField)
               + ", healthFieldFailures=" + learnedHealthFieldFailures
               + ", capField=" + describeField(learnedCapField)
               + ", capFieldFailures=" + learnedCapFieldFailures
               + ", rejectedHealthFields=" + rejectedHealthFields.size()
               + ", rejectedCapFields=" + rejectedCapFields.size()
               + ", healthFieldExplored=" + healthFieldExplored
               + ", capSourceExplored=" + capSourceExplored
               + ", corePathsFailed=" + corePathsFailed()
               + ", absolutePathsFailed=" + absolutePathsFailed()
               + ", suspectExternalCap=" + suspectExternalCap
               + ", suspectBytecodeOrEventCap=" + suspectBytecodeOrEventCap
               + ", failedLethalCount=" + failedLethalCount;
    }

    private static String fieldKey(Field field) {
        return field.getDeclaringClass().getName() + "#" + field.getName();
    }

    private static String describeField(Field field) {
        return field == null ? "none" : field.getDeclaringClass().getSimpleName() + "#" + field.getName();
    }
}
