package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonData;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
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
    private static final String CONSEQUENCE_SEED = "consequence_seed";

    private @Nullable StigmataEntry manifestation;
    private @Nullable StigmataEntry alienation;
    private @Nullable StigmataEntry sealing;
    private int consequenceSeed = -1;

    public StigmataData() {}

    public StigmataData copy() {
        StigmataData copy = new StigmataData();
        copy.manifestation = manifestation;
        copy.alienation = alienation;
        copy.sealing = sealing;
        copy.consequenceSeed = consequenceSeed;
        return copy;
    }

    public boolean hasConsequenceSeed() {
        return 0 <= consequenceSeed;
    }

    public int consequenceSeed() {
        return consequenceSeed;
    }

    public void assignConsequenceSeed(RandomSource random) {
        if (!hasConsequenceSeed()) {
            consequenceSeed = random.nextInt(0x100);
        }
    }

    public StigmataConsequence consequence() {
        return StigmataConsequence.fromSeed(consequenceSeed);
    }

    /**
     * Computes burden from the exact materials recorded by each active Stigmata stage.
     */
    public int burden() {
        return materialTier(manifestation)
               + materialTier(alienation) * 2
               + materialTier(sealing) * 4;
    }

    /**
     * Computes the target tool's inherent Stigmata capacity from its resolvable materials.
     */
    public static int capacity(IToolContext context) {
        MaterialNBT materials = context.getMaterials();
        int partCount = ToolPartsHook.parts(context.getDefinition()).size();
        if (0 == partCount){
            partCount = ToolMaterialHook.stats(context.getDefinition()).size();
        }

        double tierTotal = 0.0D;
        int resolvedMaterialCount = 0;
        for (int index = 0; index < materials.size(); index++) {
            MaterialVariant variant = materials.get(index);
            IMaterial material = variant.get();
            if (IMaterial.UNKNOWN == material){
                continue;
            }

            tierTotal += material.getTier();
            resolvedMaterialCount++;
        }

        double averageMaterialTier =
                0 == resolvedMaterialCount ? 0.0D : tierTotal / resolvedMaterialCount;
        int baseCapacity = (int) Math.floor(5.0D * averageMaterialTier);
        int partPenalty = 2 * Math.max(0, partCount - 2);
        return Math.max(0, baseCapacity - partPenalty);
    }

    /**
     * Assigned Noumenon tuning levels offset burden but never increase capacity.
     */
    public static int attunement(IToolContext context) {
        long total = 0L;
        for (int value : NoumenonData.read(context).tunings.values()) {
            total += Math.max(0, value);
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public int overload(IToolContext context) {
        return Math.max(0, burden() - capacity(context) - attunement(context));
    }

    private static int materialTier(@Nullable StigmataEntry entry) {
        if (null == entry){
            return 0;
        }
        IMaterialRegistry materials = MaterialRegistry.getInstance();
        IMaterial material = materials.getMaterial(entry.materialId());
        return IMaterial.UNKNOWN == material ? 0 : material.getTier();
    }

    public int stage() {
        if (null == manifestation){
            return 0;
        }
        if (null == alienation){
            return 1;
        }
        if (null == sealing){
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
        if (0 == stage){
            return Collections.emptyList();
        }
        List<StigmataEntry> result = new ArrayList<>(stage);
        result.add(manifestation);
        if (2 <= stage){
            result.add(alienation);
        }
        if (3 <= stage){
            result.add(sealing);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Rejects non-contiguous data instead of allowing orphan upper stages.
     * This is intentionally deterministic for commands, old saves and malformed NBT.
     */
    public void normalize() {
        if (null == manifestation){
            alienation = null;
            sealing = null;
        }else if (null == alienation){
            sealing = null;
        }
    }

    public CompoundTag serialize() {
        normalize();
        CompoundTag tag = new CompoundTag();
        if (hasConsequenceSeed()){
            tag.putInt(CONSEQUENCE_SEED, consequenceSeed);
        }
        if (null != manifestation){
            tag.put(MANIFESTATION, manifestation.serialize());
        }
        if (null != alienation){
            tag.put(ALIENATION, alienation.serialize());
        }
        if (null != sealing){
            tag.put(SEALING, sealing.serialize());
        }
        return tag;
    }

    public static StigmataData deserialize(CompoundTag tag) {
        StigmataData data = new StigmataData();
        if (tag.contains(CONSEQUENCE_SEED, CompoundTag.TAG_INT)){
            data.consequenceSeed = tag.getInt(CONSEQUENCE_SEED) & 0xFF;
        }
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
        if (0 == stage()){
            tool.getPersistentData().remove(KEY);
        }else {
            tool.getPersistentData().put(KEY, serialize());
        }
    }
}
