package org.brahypno.esotericismtinker.utils.damage.state;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.esotericismtinker.utils.damage.DamageConstants;
import org.brahypno.esotericismtinker.utils.damage.DamageContext;
import org.brahypno.esotericismtinker.utils.damage.DamageProbeResult;
import org.brahypno.esotericismtinker.utils.damage.profile.DamageProfile;
import org.brahypno.esotericismtinker.utils.damage.profile.DamageProfileCache;
import org.brahypno.esotericismtinker.utils.damage.step.KillPathKind;
import org.brahypno.esotericismtinker.utils.damage.step.StepResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Correlated NBT group probe.
 *
 * This is deliberately not a generic NBT fuzzer. It learns groups only from
 * authoritative damage diffs, then mutates only the values that moved together
 * with real damage: usually vanilla Health, one numeric HP mirror, and one
 * strongly correlated ForgeCaps byte blob. ForgeCaps are never mutated merely
 * because their name looks interesting.
 */
public final class NbtMutationProbe {
  private static final int MAX_GROUPS = 16;
  private static final float MATCH_TOLERANCE = 0.75F;
  private static final Map<String, CorrelatedGroup> GROUPS = new LinkedHashMap<>();

  private NbtMutationProbe() {}

  public static void observeDamageDiff(String prefix, NbtStateDiff.Snapshot before, NbtStateDiff.Snapshot after, float actualDealt, DamageProbeResult result) {
    if (before == null || after == null || actualDealt <= DamageConstants.DAMAGE_EPS) return;

    List<NumericMove> numericMoves = numericMoves(before, after, actualDealt);
    if (numericMoves.size() < 2) return;

    List<BlobMove> blobMoves = blobMoves(before, after, numericMoves, actualDealt, result, prefix);
    numericMoves.sort(Comparator.comparingInt(NumericMove::score).reversed().thenComparing(NumericMove::path));

    NumericMove primary = numericMoves.get(0);
    NumericMove mirror = null;
    for (NumericMove move : numericMoves) {
      if (!move.path().equals(primary.path())) {
        mirror = move;
        break;
      }
    }
    if (mirror == null) return;

    BlobMove blob = blobMoves.isEmpty() ? null : blobMoves.get(0);
    String key = primary.path() + "|" + mirror.path() + "|" + (blob == null ? "none" : blob.path() + ":" + blob.format());
    CorrelatedGroup old = GROUPS.get(key);
    int hits = old == null ? 1 : old.hits() + 1;
    CorrelatedGroup group = new CorrelatedGroup(primary.path(), mirror.path(), blob == null ? null : blob.path(), blob == null ? BlobFormat.NONE : blob.format(), hits);
    GROUPS.put(key, group);
    trimGroups();

    result.add(prefix + " nbt_correlated_group_observed: hits=" + hits
        + ", primary=" + primary.path()
        + ", mirror=" + mirror.path()
        + (blob == null ? ", forgeCap=none" : ", forgeCap=" + blob.path() + ", format=" + blob.format()
            + ", decoded " + trim(blob.beforeDecoded()) + " -> " + trim(blob.afterDecoded())));
  }

  public static StepResult tryNbtMutation(DamageContext context, DamageProbeResult result, String prefix) {
    LivingEntity victim = context.victim();
    if (victim == null) return StepResult.noProgress();

    DamageSnapshot before = DamageSnapshot.of(victim);
    CompoundTag source = new CompoundTag();
    try {
      victim.saveWithoutId(source);
    } catch (Throwable e) {
      result.add(prefix + " skipped: saveWithoutId error=" + e.getClass().getSimpleName());
      return StepResult.noProgress();
    }

    List<CorrelatedGroup> candidates = matchingGroups(source);
    result.add(prefix + " correlated_groups=" + candidates.size() + ", noRollback=true, forgeCapsOnlyIfCorrelated=true");
    if (candidates.isEmpty()) {
      result.add(prefix + " skipped: no learned correlated NBT group");
      return StepResult.noProgress();
    }

    CorrelatedGroup group = candidates.get(0);
    float requested = context.remainingOrAmount(result);
    float targetHealth = Math.max(0.0F, before.health() - Math.max(1.0F, requested));
    CompoundTag mutated = source.copy();

    int applied = 0;
    applied += writeNumeric(mutated, group.primaryPath(), targetHealth, result, prefix, "primary");
    applied += writeNumeric(mutated, group.mirrorPath(), targetHealth, result, prefix, "mirror");
    applied += writeBlob(mutated, source, group, targetHealth, before.health(), result, prefix);

    if (applied == 0) {
      result.add(prefix + " skipped: correlated group had no writable values, group=" + group.describe());
      return StepResult.noProgress();
    }

    try {
      victim.load(mutated);
    } catch (Throwable e) {
      result.add(prefix + " load error: " + e.getClass().getSimpleName() + ", group=" + group.describe());
      return StepResult.noProgress();
    }

    DamageSnapshot after = DamageSnapshot.of(victim);
    float dealt = positiveDelta(before.health(), after.health()) + positiveDelta(before.absorption(), after.absorption());
    boolean authoritative = after.authoritativeChangeFrom(before) || result.killConfirmed();
    result.recordDamageLikeChange(before, after);
    // Group writes can bypass the normal damage pipeline and may include codec offsets.
    // Do not feed their delta into observed damage-cap learning.

    DamageProfile profile = DamageProfileCache.profileFor(victim);
    boolean bypassedCap = bypassedObservedCap(profile, requested, dealt) || result.killConfirmed();
    if (authoritative && !bypassedCap) {
      result.add(prefix + " group_write_cap_limited: group=" + group.describe()
          + ", health " + before.health() + " -> " + after.health()
          + ", dealt=" + dealt
          + ", observedCap=" + profile.observedCap()
          + ", not counted as terminal success");
    } else if (!authoritative) {
      result.add(prefix + " group_write_rejected: group=" + group.describe()
          + ", health " + before.health() + " -> " + after.health()
          + ", dealt=" + dealt);
    } else {
      result.add(prefix + " group_write_success: group=" + group.describe()
          + ", health " + before.health() + " -> " + after.health()
          + ", dealt=" + dealt
          + ", bypassedCap=" + bypassedCap);
      result.markKillPath(KillPathKind.AUTHORITY_STATE, prefix + " correlated NBT group");
    }

    return new StepResult(authoritative && bypassedCap, dealt, authoritative && bypassedCap ? KillPathKind.AUTHORITY_STATE : KillPathKind.NONE);
  }

  private static List<NumericMove> numericMoves(NbtStateDiff.Snapshot before, NbtStateDiff.Snapshot after, float dealt) {
    List<NumericMove> moves = new ArrayList<>();
    for (Map.Entry<String, NbtStateDiff.Value> entry : before.values().entrySet()) {
      String path = entry.getKey();
      NbtStateDiff.Value left = entry.getValue();
      NbtStateDiff.Value right = after.values().get(path);
      if (left == null || right == null || left.numeric() == null || right.numeric() == null) continue;
      double delta = left.numeric() - right.numeric();
      if (Math.abs(delta - dealt) > MATCH_TOLERANCE) continue;
      int score = scoreNumericPath(path);
      if (score <= 0) continue;
      moves.add(new NumericMove(path, left.numeric(), right.numeric(), score));
    }
    return moves;
  }

  private static List<BlobMove> blobMoves(NbtStateDiff.Snapshot before, NbtStateDiff.Snapshot after, List<NumericMove> numericMoves, float dealt, DamageProbeResult result, String prefix) {
    List<BlobMove> moves = new ArrayList<>();
    for (Map.Entry<String, NbtStateDiff.Value> entry : before.values().entrySet()) {
      String path = entry.getKey();
      String lower = path.toLowerCase(Locale.ROOT);
      if (!lower.contains("forgecaps")) continue;
      NbtStateDiff.Value left = entry.getValue();
      NbtStateDiff.Value right = after.values().get(path);
      if (left == null || right == null || Objects.equals(left, right)) continue;

      byte[] beforeBytes = parseByteArray(left.text());
      byte[] afterBytes = parseByteArray(right.text());
      if (beforeBytes == null || afterBytes == null || beforeBytes.length != afterBytes.length || beforeBytes.length != 4) continue;

      for (BlobFormat format : List.of(BlobFormat.FLOAT_BE, BlobFormat.FLOAT_LE, BlobFormat.INT_BE, BlobFormat.INT_LE)) {
        double decodedBefore = decode(beforeBytes, format);
        double decodedAfter = decode(afterBytes, format);
        double delta = decodedBefore - decodedAfter;
        if (Math.abs(delta - dealt) > MATCH_TOLERANCE) continue;
        if (!matchesAnyNumeric(decodedBefore, decodedAfter, numericMoves)) continue;
        BlobMove move = new BlobMove(path, format, decodedBefore, decodedAfter, 100);
        moves.add(move);
        result.add(prefix + " forgecap_blob_decode_candidate: path=" + path
            + ", format=" + format
            + ", decoded=" + trim(decodedBefore) + " -> " + trim(decodedAfter)
            + ", matchesActualDealt=true");
        break;
      }
    }
    moves.sort(Comparator.comparingInt(BlobMove::score).reversed().thenComparing(BlobMove::path));
    return moves;
  }

  private static boolean matchesAnyNumeric(double decodedBefore, double decodedAfter, List<NumericMove> numericMoves) {
    for (NumericMove move : numericMoves) {
      double beforeOffset = decodedBefore - move.before();
      double afterOffset = decodedAfter - move.after();
      if (Math.abs(beforeOffset - afterOffset) <= MATCH_TOLERANCE && Math.abs(beforeOffset) <= 2.0D) return true;
    }
    return false;
  }

  private static int scoreNumericPath(String path) {
    String lower = path.toLowerCase(Locale.ROOT);
    int score = 0;
    if (lower.equals("nbt.health") || lower.endsWith(".health")) score += 90;
    if (containsAny(lower, "health", "hp", "life", "vital")) score += 70;
    if (containsAny(lower, "maxhealth", "max_health", "last", "hurt", "time", "timer", "cooldown", "motion", "pos", "rotation", "air", "fall", "portal", "uuid")) score -= 80;
    return score;
  }

  private static List<CorrelatedGroup> matchingGroups(CompoundTag source) {
    List<CorrelatedGroup> groups = new ArrayList<>();
    for (CorrelatedGroup group : GROUPS.values()) {
      if (find(source, stripNbt(group.primaryPath())) == null) continue;
      if (find(source, stripNbt(group.mirrorPath())) == null) continue;
      if (group.forgeCapPath() != null && find(source, stripNbt(group.forgeCapPath())) == null) continue;
      groups.add(group);
    }
    groups.sort(Comparator.comparingInt(CorrelatedGroup::hits).reversed());
    return groups;
  }

  private static int writeNumeric(CompoundTag root, String path, float value, DamageProbeResult result, String prefix, String role) {
    PathTarget target = find(root, stripNbt(path));
    if (target == null || !(target.value() instanceof NumericTag numeric)) return 0;
    Tag replacement = sameType(numeric, value);
    if (replacement == null || replacement.equals(target.value())) return 0;
    target.parent().put(target.key(), replacement);
    result.add(prefix + " group_apply: role=" + role + ", path=" + path + ", value=" + value);
    return 1;
  }

  private static int writeBlob(CompoundTag mutated, CompoundTag source, CorrelatedGroup group, float targetHealth, float currentHealth, DamageProbeResult result, String prefix) {
    if (group.forgeCapPath() == null || group.format() == BlobFormat.NONE) return 0;
    PathTarget sourceTarget = find(source, stripNbt(group.forgeCapPath()));
    PathTarget mutatedTarget = find(mutated, stripNbt(group.forgeCapPath()));
    if (sourceTarget == null || mutatedTarget == null || !(sourceTarget.value() instanceof ByteArrayTag bytes)) return 0;
    byte[] current = bytes.getAsByteArray();
    if (current.length != 4) return 0;

    double decoded = decode(current, group.format());
    double offset = decoded - currentHealth;
    double targetDecoded = targetHealth + offset;
    byte[] replacement = encode(targetDecoded, group.format());
    mutatedTarget.parent().put(mutatedTarget.key(), new ByteArrayTag(replacement));
    result.add(prefix + " group_apply: role=forgeCapBlob, path=" + group.forgeCapPath()
        + ", format=" + group.format()
        + ", decoded=" + trim(decoded)
        + ", targetDecoded=" + trim(targetDecoded)
        + ", offset=" + trim(offset));
    return 1;
  }

  private static PathTarget find(CompoundTag root, String path) {
    String[] parts = path.split("\\.");
    CompoundTag current = root;
    for (int i = 0; i < parts.length; i++) {
      String key = parts[i];
      if (i == parts.length - 1) return current.contains(key) ? new PathTarget(current, key, current.get(key)) : null;
      Tag next = current.get(key);
      if (!(next instanceof CompoundTag child)) return null;
      current = child;
    }
    return null;
  }

  private static String stripNbt(String path) {
    return path.startsWith("nbt.") ? path.substring(4) : path;
  }

  private static Tag sameType(NumericTag old, double value) {
    if (old instanceof FloatTag) return FloatTag.valueOf((float)value);
    if (old instanceof DoubleTag) return DoubleTag.valueOf(value);
    if (old instanceof IntTag) return IntTag.valueOf((int)Math.round(value));
    if (old instanceof LongTag) return LongTag.valueOf(Math.round(value));
    if (old instanceof ShortTag) return ShortTag.valueOf((short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(value))));
    if (old instanceof ByteTag) return ByteTag.valueOf((byte)Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, Math.round(value))));
    return null;
  }

  private static byte[] parseByteArray(String text) {
    if (text == null || !text.startsWith("[B;") || !text.endsWith("]")) return null;
    String body = text.substring(3, text.length() - 1);
    if (body.isBlank()) return new byte[0];
    String[] parts = body.split(",");
    byte[] bytes = new byte[parts.length];
    try {
      for (int i = 0; i < parts.length; i++) {
        String part = parts[i].trim();
        if (part.endsWith("B") || part.endsWith("b")) part = part.substring(0, part.length() - 1);
        bytes[i] = (byte) Integer.parseInt(part);
      }
      return bytes;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static double decode(byte[] bytes, BlobFormat format) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    return switch (format) {
      case FLOAT_BE -> buffer.order(ByteOrder.BIG_ENDIAN).getFloat();
      case FLOAT_LE -> buffer.order(ByteOrder.LITTLE_ENDIAN).getFloat();
      case INT_BE -> buffer.order(ByteOrder.BIG_ENDIAN).getInt();
      case INT_LE -> buffer.order(ByteOrder.LITTLE_ENDIAN).getInt();
      case NONE -> Double.NaN;
    };
  }

  private static byte[] encode(double value, BlobFormat format) {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    switch (format) {
      case FLOAT_BE -> buffer.order(ByteOrder.BIG_ENDIAN).putFloat((float)value);
      case FLOAT_LE -> buffer.order(ByteOrder.LITTLE_ENDIAN).putFloat((float)value);
      case INT_BE -> buffer.order(ByteOrder.BIG_ENDIAN).putInt((int)Math.round(value));
      case INT_LE -> buffer.order(ByteOrder.LITTLE_ENDIAN).putInt((int)Math.round(value));
      case NONE -> {}
    }
    return buffer.array();
  }

  private static boolean bypassedObservedCap(DamageProfile profile, float requested, float dealt) {
    if (dealt <= DamageConstants.DAMAGE_EPS) return false;
    if (!profile.stableDamageCap()) return dealt + DamageConstants.DAMAGE_TOLERANCE >= requested;
    return dealt > profile.observedCap() + Math.max(0.5F, profile.observedCap() * 0.10F);
  }

  private static float positiveDelta(float before, float after) {
    return Math.max(0.0F, before - after);
  }

  private static boolean containsAny(String s, String... keys) {
    for (String key : keys) if (s.contains(key)) return true;
    return false;
  }

  private static String trim(double value) {
    return String.format(Locale.ROOT, "%.4f", value);
  }

  private static void trimGroups() {
    while (GROUPS.size() > MAX_GROUPS) {
      String first = GROUPS.keySet().iterator().next();
      GROUPS.remove(first);
    }
  }

  private enum BlobFormat { NONE, FLOAT_BE, FLOAT_LE, INT_BE, INT_LE }
  private record NumericMove(String path, double before, double after, int score) {}
  private record BlobMove(String path, BlobFormat format, double beforeDecoded, double afterDecoded, int score) {}
  private record CorrelatedGroup(String primaryPath, String mirrorPath, String forgeCapPath, BlobFormat format, int hits) {
    String describe() {
      return "primary=" + primaryPath + ", mirror=" + mirrorPath + ", forgeCap=" + (forgeCapPath == null ? "none" : forgeCapPath + ":" + format) + ", hits=" + hits;
    }
  }
  private record PathTarget(CompoundTag parent, String key, Tag value) {}
}
