package org.brahypno.esotericismtinker.utils.damage.step;

import org.brahypno.esotericismtinker.utils.damage.DamageContext;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;

public record FunctionalDamageStep(String name, StepKind kind, StepRunner runner) implements DamageStep {
    @Override
    public StepResult run(DamageContext context, DamageProbeResult result) {
        return runner.run(context, result);
    }

    @FunctionalInterface
    public interface StepRunner {
        StepResult run(DamageContext context, DamageProbeResult result);
    }
}
