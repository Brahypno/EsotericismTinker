package org.brahypno.esotericismtinker.utils.damage.step;

import org.brahypno.esotericismtinker.utils.damage.DamageContext;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;

public interface DamageStep {
    String name();
    StepKind kind();
    StepResult run(DamageContext context, DamageProbeResult result);
}
