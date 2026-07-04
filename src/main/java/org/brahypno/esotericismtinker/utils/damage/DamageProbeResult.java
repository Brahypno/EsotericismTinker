package org.brahypno.esotericismtinker.utils.damage;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.state.DamageSnapshot;
import org.brahypno.esotericismtinker.utils.damage.step.KillPathKind;

import java.util.ArrayList;
import java.util.List;

public final class DamageProbeResult {
  private final DamageContext context;
  private final List<String> lines = new ArrayList<>();
  private boolean success;
  private boolean serverDead;
  private boolean deathHandled;
  private boolean finalRequested;
  private boolean lethalRequested;
  private boolean pipelineEntered;
  private boolean damageStateChanged;
  private boolean supportChanged;
  private String strategy = "none";
  private KillPathKind killPathKind = KillPathKind.NONE;
  private boolean deathFinalized;
  private float totalDealt;

  public DamageProbeResult(DamageContext context) {
    this.context = context;
    add("created: forceLevel=" + context.forceLevel()
        + ", entity=" + describeEntity(context.entity())
        + ", victim=" + describeEntity(context.victim())
        + ", amount=" + context.amount()
        + ", initialHealth=" + context.initialHealth());
  }

  public DamageContext context() {
    return context;
  }

  public void addHeader(String name) {
    add("========== " + name + " ==========");
  }

  public void add(String line) {
    lines.add(line);
  }

  public DamageProbeResult success(String strategy) {
    this.success = true;
    this.strategy = strategy;
    add("success: " + strategy);
    return this;
  }

  public void clearSuccess(String reason) {
    if (success) add("clear_success: " + reason + ", previousStrategy=" + strategy);
    this.success = false;
    this.strategy = "none";
  }

  public DamageProbeResult fail(String reason) {
    add("failed: " + reason);
    return this;
  }

  public DamageProbeResult serverDead(String reason) {
    this.serverDead = true;
    add("server_dead: " + reason);
    return this;
  }

  public void markDeathHandled(String reason) {
    this.deathHandled = true;
    add("death_handled: " + reason);
  }

  public void markFinalRequested(boolean lethalRequested) {
    this.finalRequested = true;
    this.lethalRequested = lethalRequested;
    add("final_requested: lethal=" + lethalRequested);
  }

  public void markPipelineEntered(String reason) {
    this.pipelineEntered = true;
    add("pipeline_entered: " + reason);
  }

  public void markDamageStateChanged(String reason) {
    this.damageStateChanged = true;
    add("damage_state_changed: " + reason);
  }

  public void markSupportChanged(String reason) {
    this.supportChanged = true;
    add("support_changed: " + reason);
  }

  public void markKillPath(KillPathKind kind, String reason) {
    if (kind == null || kind == KillPathKind.NONE) return;
    this.killPathKind = kind;
    add("kill_path: " + kind + ", reason=" + reason);
  }

  public KillPathKind killPathKind() {
    return killPathKind;
  }

  public void markDeathFinalized(String reason) {
    this.deathFinalized = true;
    add("death_finalized: " + reason);
  }

  public boolean deathFinalized() {
    return deathFinalized;
  }

  public void recordDamageLikeChange(DamageSnapshot before, DamageSnapshot after) {
    if (before.health() > after.health()) {
      float dealt = before.health() - after.health();
      totalDealt += dealt;
      markDamageStateChanged("health changed " + before.health() + " -> " + after.health() + ", dealt=" + dealt);
    }
    if (before.absorption() > after.absorption()) {
      float dealt = before.absorption() - after.absorption();
      totalDealt += dealt;
      markDamageStateChanged("absorption changed " + before.absorption() + " -> " + after.absorption() + ", dealt=" + dealt);
    }
  }

  public void recordSyntheticDamage(float amount, String reason) {
    if (amount <= 0.0F) return;
    totalDealt += amount;
    markDamageStateChanged("synthetic damage=" + amount + ", reason=" + reason);
  }

  public boolean reachedExpectedDamage() {
    return killConfirmed() || totalDealt + DamageConstants.DAMAGE_EPS >= context.amount() * DamageConstants.DAMAGE_TOLERANCE;
  }

  /**
   * True only when damage/death state changed. Merely entering a vanilla/mod handler is not enough,
   * as many handlers return true while external caps or event gates reject all effective damage.
   */
  public boolean instantEffective() {
    return serverDead
        || totalDealt > DamageConstants.DAMAGE_EPS
        || damageStateChanged
        || killConfirmed();
  }

  public boolean killConfirmed() {
    LivingEntity victim = context.victim();
    return victim == null
        || victim.isRemoved()
        || !victim.isAlive()
        || victim.deathTime > 0
        || victim.getHealth() <= DamageConstants.DAMAGE_EPS;
  }

  public float remainingAmount() {
    return reachedExpectedDamage() ? 0.0F : Math.max(0.0F, context.amount() - totalDealt);
  }

  public float remainingAmountForStep() {
    return killConfirmed() ? 0.0F : remainingAmount();
  }

  public boolean needsSurfaceDeathFinalizer() {
    return killConfirmed() && !deathFinalized && killPathKind == KillPathKind.SURFACE_HEALTH;
  }

  public float totalDealt() { return totalDealt; }
  public boolean success() { return success; }
  public boolean serverDead() { return serverDead; }
  public boolean deathHandled() { return deathHandled; }
  public String strategy() { return strategy; }
  public List<String> lines() { return List.copyOf(lines); }

  public String debugText() {
    return "DamageProbe{"
        + "entity=" + describeEntity(context.entity())
        + ", victim=" + describeEntity(context.victim())
        + ", forceLevel=" + context.forceLevel()
        + ", amount=" + context.amount()
        + ", initialHealth=" + context.initialHealth()
        + ", currentHealth=" + currentHealth()
        + ", totalDealt=" + totalDealt
        + ", remaining=" + remainingAmount()
        + ", success=" + success
        + ", serverDead=" + serverDead
        + ", deathHandled=" + deathHandled
        + ", finalRequested=" + finalRequested
        + ", lethalRequested=" + lethalRequested
        + ", pipelineEntered=" + pipelineEntered
        + ", damageStateChanged=" + damageStateChanged
        + ", supportChanged=" + supportChanged
        + ", instantEffective=" + instantEffective()
        + ", killConfirmed=" + killConfirmed()
        + ", strategy=" + strategy
        + ", killPathKind=" + killPathKind
        + ", deathFinalized=" + deathFinalized
        + ", steps=" + lines
        + "}";
  }

  public String compatDebugText() {
    return "DamageProbe.Result{"
        + "forceLevel=" + context.forceLevel()
        + ", amount=" + context.amount()
        + ", initialHealth=" + context.initialHealth()
        + ", currentHealth=" + currentHealth()
        + ", totalDealt=" + totalDealt
        + ", remaining=" + remainingAmount()
        + ", success=" + success
        + ", serverDead=" + serverDead
        + ", deathHandled=" + deathHandled
        + ", pipelineEntered=" + pipelineEntered
        + ", damageStateChanged=" + damageStateChanged
        + ", supportChanged=" + supportChanged
        + ", instantEffective=" + instantEffective()
        + ", killConfirmed=" + killConfirmed()
        + ", strategy=" + strategy
        + ", killPathKind=" + killPathKind
        + ", deathFinalized=" + deathFinalized
        + "}";
  }

  private float currentHealth() {
    return context.victim() == null ? -1.0F : context.victim().getHealth();
  }

  private static String describeEntity(Entity entity) {
    if (entity == null) return "null";
    return entity.getClass().getName()
        + ", type=" + entity.getType()
        + ", uuid=" + entity.getUUID()
        + ", removed=" + entity.isRemoved();
  }
}
