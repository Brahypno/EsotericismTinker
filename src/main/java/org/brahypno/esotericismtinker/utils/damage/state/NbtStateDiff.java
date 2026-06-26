package org.brahypno.esotericismtinker.utils.damage.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageConstants;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Passive NBT/ForgeCaps diff. Diagnostic only: it never writes NBT back to the entity. */
public final class NbtStateDiff {
  private static final int MAX_VALUES = 1024;
  private static final int MAX_DEPTH = 6;
  private static final int MAX_STRING = 160;

  private NbtStateDiff() {}

  public static Snapshot capture(LivingEntity entity, DamageProbeResult result, String phase) {
    Map<String, Value> values = new LinkedHashMap<>();
    if (entity == null) return new Snapshot(values);

    try {
      CompoundTag tag = new CompoundTag();
      entity.saveWithoutId(tag);
      flatten("nbt", tag, 0, values);
    } catch (Throwable e) {
      result.add("nbt_state_diff capture error: phase=" + phase + ", error=" + e.getClass().getSimpleName());
    }

    return new Snapshot(values);
  }

  public static void logDiff(String prefix, Snapshot before, Snapshot after, float actualDealt, DamageProbeResult result) {
    if (before == null || after == null) return;

    List<Diff> diffs = diff(before, after, actualDealt);
    NbtMutationProbe.observeDamageDiff(prefix, before, after, actualDealt, result);
    if (diffs.isEmpty()) {
      result.add(prefix + " nbt_diff: changed=0");
      return;
    }

    result.add(prefix + " nbt_diff: changed=" + diffs.size() + ", actualDealt=" + actualDealt);
    int shown = 0;
    for (Diff diff : diffs) {
      if (shown++ >= 24) {
        result.add(prefix + " nbt_diff_candidate: ... more=" + (diffs.size() - shown + 1));
        break;
      }
      result.add(prefix + " nbt_diff_candidate: " + diff.path()
          + ": " + diff.before().text() + " -> " + diff.after().text()
          + ", score=" + diff.score()
          + ", reason=" + diff.reason());
    }
  }

  private static List<Diff> diff(Snapshot before, Snapshot after, float dealt) {
    Set<String> keys = new TreeSet<>();
    keys.addAll(before.values().keySet());
    keys.addAll(after.values().keySet());

    List<Diff> diffs = new ArrayList<>();
    for (String key : keys) {
      Value left = before.values().get(key);
      Value right = after.values().get(key);
      if (Objects.equals(left, right)) continue;
      int score = score(key, left, right, dealt);
      String reason = reason(key, left, right, dealt);
      diffs.add(new Diff(key, left == null ? Value.MISSING : left, right == null ? Value.MISSING : right, score, reason));
    }

    diffs.sort(Comparator.comparingInt(Diff::score).reversed().thenComparing(Diff::path));
    return diffs;
  }

  private static int score(String path, Value before, Value after, float dealt) {
    String lower = path.toLowerCase(Locale.ROOT);
    int score = 0;

    if (containsAny(lower, "forgecaps", "cap", "capability", "component")) score += 28;
    if (containsAny(lower, "health", "hp", "life", "max_health", "maxhealth")) score += 48;
    if (containsAny(lower, "damage", "hurt", "last_hurt", "lasthurt", "taken")) score += 32;
    if (containsAny(lower, "invul", "immune", "cooldown", "cool_down", "hurt_ticks", "hurtticks", "hitcooldown", "guard", "shield", "barrier")) score += 26;
    if (containsAny(lower, "phase", "stage", "secondphase", "death")) score += 14;
    if (containsAny(lower, "pos", "motion", "rotation", "look", "fall", "portal", "uuid", "brain", "path", "navigation")) score -= 30;

    if (before != null && after != null && before.numeric() != null && after.numeric() != null) {
      double delta = Math.abs(before.numeric() - after.numeric());
      if (dealt > DamageConstants.DAMAGE_EPS && Math.abs(delta - dealt) <= 0.5D) score += 44;
      if (Math.abs(delta - 20.0D) <= 0.5D) score += 40;
      if (Math.abs(delta - 4000.0D) <= 0.5D) score += 12;
      if (after.numeric() == 0.0D && before.numeric() > 0.0D) score += 8;
    }

    return score;
  }

  private static String reason(String path, Value before, Value after, float dealt) {
    List<String> reasons = new ArrayList<>();
    String lower = path.toLowerCase(Locale.ROOT);
    if (containsAny(lower, "forgecaps", "cap", "capability", "component")) reasons.add("cap_like_path");
    if (containsAny(lower, "health", "hp", "life", "max_health", "maxhealth")) reasons.add("health_like_path");
    if (containsAny(lower, "damage", "hurt", "taken")) reasons.add("damage_like_path");
    if (containsAny(lower, "invul", "immune", "cooldown", "guard", "shield", "barrier")) reasons.add("guard_like_path");
    if (before != null && after != null && before.numeric() != null && after.numeric() != null) {
      double delta = Math.abs(before.numeric() - after.numeric());
      if (dealt > DamageConstants.DAMAGE_EPS && Math.abs(delta - dealt) <= 0.5D) reasons.add("delta_matches_actual_dealt");
      if (Math.abs(delta - 20.0D) <= 0.5D) reasons.add("delta_matches_20_cap");
      if (after.numeric() == 0.0D && before.numeric() > 0.0D) reasons.add("cleared_to_zero");
    }
    return reasons.isEmpty() ? "changed" : String.join(",", reasons);
  }

  private static void flatten(String path, Tag tag, int depth, Map<String, Value> values) {
    if (tag == null || values.size() >= MAX_VALUES || depth > MAX_DEPTH) return;

    if (tag instanceof CompoundTag compound) {
      for (String key : compound.getAllKeys()) {
        if (values.size() >= MAX_VALUES) return;
        flatten(path + "." + key, compound.get(key), depth + 1, values);
      }
      return;
    }

    if (tag instanceof ListTag list) {
      values.put(path + ".size", new Value(Integer.toString(list.size()), (double) list.size()));
      int max = Math.min(16, list.size());
      for (int i = 0; i < max; i++) flatten(path + "[" + i + "]", list.get(i), depth + 1, values);
      return;
    }

    values.put(path, scalar(tag));
  }

  private static Value scalar(Tag tag) {
    if (tag instanceof NumericTag number) {
      double value = number.getAsDouble();
      return new Value(trim(Double.toString(value)), value);
    }
    return new Value(trim(tag.getAsString()), null);
  }

  private static String trim(String value) {
    if (value == null) return "null";
    if (value.length() <= MAX_STRING) return value;
    return value.substring(0, MAX_STRING) + "...";
  }

  private static boolean containsAny(String s, String... keys) {
    for (String key : keys) if (s.contains(key)) return true;
    return false;
  }

  public record Snapshot(Map<String, Value> values) {}
  public record Value(String text, Double numeric) { static final Value MISSING = new Value("<missing>", null); }
  private record Diff(String path, Value before, Value after, int score, String reason) {}
}
