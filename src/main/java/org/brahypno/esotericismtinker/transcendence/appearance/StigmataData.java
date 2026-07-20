package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistent-data truth source for Stigmata.
 * <p>
 * The current stage is the longest continuous prefix:
 * manifestation -> alienation -> sealing.
 * Modifier level is never consulted.
 */
public final class StigmataData {
    public static final ResourceLocation KEY =
            new ResourceLocation("esotericism_tinker", "stigmata");

    private static final String MANIFESTATION = "manifestation";
    private static final String ALIENATION = "alienation";
    private static final String SEALING = "sealing";

    private @Nullable StigmataEntry manifestation;
    private @Nullable StigmataEntry alienation;
    private @Nullable StigmataEntry sealing;

    public StigmataData() {}

    public StigmataData copy() {
        StigmataData copy = new StigmataData();
        copy.manifestation = manifestation;
        copy.alienation = alienation;
        copy.sealing = sealing;
        return copy;
    }

    public int stage() {
        if (manifestation == null){
            return 0;
        }
        if (alienation == null){
            return 1;
        }
        if (sealing == null){
            return 2;
        }
        return 3;
    }

    public @Nullable StigmataEntry get(StigmataStage stage) {
        return switch (stage) {
            case MANIFESTATION -> manifestation;
            case ALIENATION -> alienation;
            case SEALING -> sealing;
        };
    }

    public void set(StigmataStage stage, @Nullable StigmataEntry entry) {
        switch (stage) {
            case MANIFESTATION -> manifestation = entry;
            case ALIENATION -> alienation = entry;
            case SEALING -> sealing = entry;
        }
        normalize();
    }

    /**
     * Retains targetStage and all earlier stages, removing all later stages.
     */
    public void truncateTo(StigmataStage targetStage) {
        switch (targetStage) {
            case MANIFESTATION -> {
                alienation = null;
                sealing = null;
            }
            case ALIENATION -> sealing = null;
            case SEALING -> {
                // Nothing later than sealing.
            }
        }
        normalize();
    }

    public List<StigmataEntry> activeEntries() {
        int stage = stage();
        if (stage == 0){
            return Collections.emptyList();
        }
        List<StigmataEntry> result = new ArrayList<>(stage);
        result.add(manifestation);
        if (stage >= 2){
            result.add(alienation);
        }
        if (stage >= 3){
            result.add(sealing);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Rejects non-contiguous data instead of allowing orphan upper stages.
     * This is intentionally deterministic for commands, old saves and malformed NBT.
     */
    public void normalize() {
        if (manifestation == null){
            alienation = null;
            sealing = null;
        }else if (alienation == null){
            sealing = null;
        }
    }

    public CompoundTag serialize() {
        normalize();
        CompoundTag tag = new CompoundTag();
        if (manifestation != null){
            tag.put(MANIFESTATION, manifestation.serialize());
        }
        if (alienation != null){
            tag.put(ALIENATION, alienation.serialize());
        }
        if (sealing != null){
            tag.put(SEALING, sealing.serialize());
        }
        return tag;
    }

    public static StigmataData deserialize(CompoundTag tag) {
        StigmataData data = new StigmataData();
        if (tag.contains(MANIFESTATION, CompoundTag.TAG_COMPOUND)){
            data.manifestation = StigmataEntry.deserialize(tag.getCompound(MANIFESTATION));
        }
        if (tag.contains(ALIENATION, CompoundTag.TAG_COMPOUND)){
            data.alienation = StigmataEntry.deserialize(tag.getCompound(ALIENATION));
        }
        if (tag.contains(SEALING, CompoundTag.TAG_COMPOUND)){
            data.sealing = StigmataEntry.deserialize(tag.getCompound(SEALING));
        }
        data.normalize();
        return data;
    }

    public static StigmataData read(IToolContext context) {
        CompoundTag persistent = context.getPersistentData().getCompound(KEY);
        return deserialize(persistent);
    }

    public static StigmataData read(ToolStack tool) {
        CompoundTag persistent = tool.getPersistentData().getCompound(KEY);
        return deserialize(persistent);
    }

    public void write(ToolStack tool) {
        normalize();
        if (stage() == 0){
            tool.getPersistentData().remove(KEY);
        }else {
            tool.getPersistentData().put(KEY, serialize());
        }
    }
}
