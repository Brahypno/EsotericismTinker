package org.brahypno.esotericismtinker.utils.damage.step;

public record StepResult(boolean progress, float dealt, KillPathKind killPathKind) {
  public StepResult(boolean progress, float dealt) {
    this(progress, dealt, KillPathKind.NONE);
  }

  public static StepResult noProgress() {
    return new StepResult(false, 0.0F, KillPathKind.NONE);
  }

  public static StepResult support(boolean progress) {
    return new StepResult(progress, 0.0F, KillPathKind.CAP_CLEAR);
  }

  public StepResult withKind(KillPathKind kind) {
    return new StepResult(progress, dealt, kind == null ? KillPathKind.NONE : kind);
  }
}
