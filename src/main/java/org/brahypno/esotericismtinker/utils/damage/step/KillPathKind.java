package org.brahypno.esotericismtinker.utils.damage.step;

/**
 * Describes what kind of state was moved by a successful probe step.
 * This is intentionally generic: it classifies the path semantics, not entity names.
 */
public enum KillPathKind {
  NONE,
  /** A normal damage pipeline accepted the hit, e.g. hurt/basic handler/actuallyHurt inside the hit chain. */
  DAMAGE_PIPELINE,
  /** The step only opened likely caps/guards; it is support, not damage success. */
  CAP_CLEAR,
  /** The step is equivalent to setHealth(0) or moving a health-like backing field; post-zero die/drop finalization is required. */
  SURFACE_HEALTH,
  /** The step moved an entity-owned authoritative state group; let the entity's own death state machine run. */
  AUTHORITY_STATE,
  /** A post-health-zero finalizer invoked die/drop paths. */
  DEATH_FINALIZER,
  /** Last-resort removal. */
  REMOVE
}
