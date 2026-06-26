package org.brahypno.esotericismtinker.utils.damage.profile;

public final class StepMemory {
    private int calls;
    private int successes;
    private int authoritativeHits;
    private int zeroProgress;
    private float bestDealt;

    public void record(boolean success, boolean authoritative, float dealt) {
        calls++;
        if (success) successes++;
        if (authoritative) authoritativeHits++;
        if (dealt <= 0.01F) zeroProgress++;
        bestDealt = Math.max(bestDealt, dealt);
    }

    public boolean shouldSkip() {
        return calls >= 3 && successes == 0 && authoritativeHits == 0;
    }

    public int calls() {
        return calls;
    }

    public int successes() {
        return successes;
    }

    public int authoritativeHits() {
        return authoritativeHits;
    }

    public int zeroProgress() {
        return zeroProgress;
    }

    public float bestDealt() {
        return bestDealt;
    }

    public String summary() {
        return "calls=" + calls + ", successes=" + successes + ", authoritativeHits=" + authoritativeHits + ", zeroProgress=" + zeroProgress + ", bestDealt=" + bestDealt;
    }
}
