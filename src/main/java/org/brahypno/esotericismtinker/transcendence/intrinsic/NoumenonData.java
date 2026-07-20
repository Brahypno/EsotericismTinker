package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class NoumenonData {
    public int level;

    /**
     * Substrate supplied by level. Shared by Reception slots and Tuning.
     */
    public int substratePoints;
    /**
     * Elevation supplied by level. Used by sublimations and investiture traits.
     */
    public int elevationPoints;

    /**
     * Debug-only Substrate and Elevation adjustments; normal gameplay must leave these at zero.
     */
    public int debugSubstratePoints;
    public int debugElevationPoints;

    /**
     * Raw TConstruct slot type names such as upgrades, abilities, traits, armor, or addon slot types.
     */
    public final Map<String, Integer> receptionSlots = new LinkedHashMap<>();
    /**
     * Abilities derived from the tool's own definition.
     */
    public final Map<ResourceLocation, Integer> sublimations = new LinkedHashMap<>();
    /**
     * Rejection and conflict stabilizers applied to the tool.
     */
    public final Map<ResourceLocation, Integer> tunings = new LinkedHashMap<>();

    /**
     * Source tool definition captured by Investiture.
     */
    @Nullable
    public ResourceLocation investedDefinition;
    public boolean investitureLocked;
    public int investitureRejection;
    /**
     * Trait snapshot granted by the invested source tool definition.
     */
    public final Map<ResourceLocation, Integer> investedTraits = new LinkedHashMap<>();

    public static NoumenonData read(IToolContext context) {return read(context.getPersistentData());}

    public static NoumenonData read(IToolStackView tool) {return read(tool.getPersistentData());}

    public static NoumenonData read(IModDataView data) {
        NoumenonData result = new NoumenonData();
        result.level = data.getInt(NoumenonKeys.LEVEL);
        result.debugSubstratePoints = data.getInt(NoumenonKeys.DEBUG_SUBSTRATE_POINTS);
        result.debugElevationPoints = data.getInt(NoumenonKeys.DEBUG_ELEVATION_POINTS);
        result.refreshPointCapacities();

        result.receptionSlots.putAll(readReceptionMap(data, NoumenonKeys.RECEPTION_SLOTS));
        result.sublimations.putAll(readResourceMap(data, NoumenonKeys.SUBLIMATIONS));
        result.tunings.putAll(readResourceMap(data, NoumenonKeys.TUNINGS));

        String investiture = data.getString(NoumenonKeys.INVESTED_DEFINITION);
        if (!investiture.isEmpty())
            result.investedDefinition = new ResourceLocation(investiture);
        result.investitureLocked = data.getBoolean(NoumenonKeys.INVESTITURE_LOCKED);
        result.investitureRejection = data.getInt(NoumenonKeys.INVESTITURE_REJECTION);
        result.investedTraits.putAll(readResourceMap(data, NoumenonKeys.INVESTED_TRAITS));
        return result;
    }

    public void write(ToolDataNBT data) {
        refreshPointCapacities();
        data.putInt(NoumenonKeys.LEVEL, level);
        data.putInt(NoumenonKeys.SUBSTRATE_POINTS, substratePoints);
        data.putInt(NoumenonKeys.ELEVATION_POINTS, elevationPoints);
        if (debugSubstratePoints != 0){
            data.putInt(NoumenonKeys.DEBUG_SUBSTRATE_POINTS, debugSubstratePoints);
        }else {
            data.remove(NoumenonKeys.DEBUG_SUBSTRATE_POINTS);
        }

        if (debugElevationPoints != 0){
            data.putInt(NoumenonKeys.DEBUG_ELEVATION_POINTS, debugElevationPoints);
        }else {
            data.remove(NoumenonKeys.DEBUG_ELEVATION_POINTS);
        }

        writeStringMap(data, NoumenonKeys.RECEPTION_SLOTS, receptionSlots);
        writeResourceMap(data, NoumenonKeys.SUBLIMATIONS, sublimations);
        writeResourceMap(data, NoumenonKeys.TUNINGS, tunings);

        if (investedDefinition == null)
            data.remove(NoumenonKeys.INVESTED_DEFINITION);
        else
            data.putString(NoumenonKeys.INVESTED_DEFINITION, investedDefinition.toString());
        if (investitureLocked){
            data.putBoolean(NoumenonKeys.INVESTITURE_LOCKED, true);
        }else {
            data.remove(NoumenonKeys.INVESTITURE_LOCKED);
        }

        if (investitureRejection != 0){
            data.putInt(NoumenonKeys.INVESTITURE_REJECTION, investitureRejection);
        }else {
            data.remove(NoumenonKeys.INVESTITURE_REJECTION);
        }
        writeResourceMap(data, NoumenonKeys.INVESTED_TRAITS, investedTraits);
    }

    public void clearInvestiture() {
        investedDefinition = null;
        investitureLocked = false;
        investitureRejection = 0;
        investedTraits.clear();
    }

    public int usedReceptionSlotPoints() {
        int used = 0;
        for (Map.Entry<String, Integer> entry : receptionSlots.entrySet()) {
            used += NoumenonDatabase.getReceptionCost(entry.getKey()) * entry.getValue();
        }
        return used;
    }

    public int usedTuningPoints() {
        int used = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : tunings.entrySet()) {
            used += NoumenonDatabase.getTuningCost(entry.getKey()) * entry.getValue();
        }
        return used;
    }

    public int usedSubstratePoints() {
        return usedReceptionSlotPoints() + usedTuningPoints();
    }

    public int substratePointConsumption() {return -usedSubstratePoints();}

    public int remainingSubstratePoints() {
        return substratePoints + substratePointConsumption();
    }

    public int usedSublimationPoints() {
        int used = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : sublimations.entrySet()) {
            used += NoumenonDatabase.getSublimationCost(entry.getKey()) * entry.getValue();
        }
        return used;
    }

    public int usedInvestiturePoints() {
        if (investedDefinition == null)
            return 0;
        int used = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : investedTraits.entrySet()) {
            int level = Math.max(0, entry.getValue());
            used += NoumenonDatabase.getInvestitureTraitCost(investedDefinition, entry.getKey()) * level;
        }
        return used;
    }

    public int usedElevationPoints() {
        return usedSublimationPoints() + usedInvestiturePoints();
    }

    public int elevationPointConsumption() {return -usedElevationPoints();}

    public int remainingElevationPoints() {
        return elevationPoints + elevationPointConsumption();
    }

    /**
     * Recomputes all level-derived point supplies. Stored point values are never authoritative.
     */
    public void refreshPointCapacities() {
        substratePoints = clampPointValue((long) baseSubstratePoints() + debugSubstratePoints);
        elevationPoints = clampPointValue((long) baseElevationPoints() + debugElevationPoints);
    }

    private static final int[] BASE_SUBSTRATE_POINTS = {0, 4, 7, 11, 16};

    public int baseSubstratePoints() {
        int safeLevel = Mth.clamp(level, 0, BASE_SUBSTRATE_POINTS.length - 1);
        return BASE_SUBSTRATE_POINTS[safeLevel];
    }

    public int baseElevationPoints() {
        int safeLevel = Math.max(0, level);
        return clampPointValue((long) safeLevel * safeLevel);
    }

    public boolean hasInvestitureSnapshot() {
        return investedDefinition != null && investitureLocked && !investedTraits.isEmpty();
    }

    public boolean isValid() {
        return remainingSubstratePoints() >= 0
               && remainingElevationPoints() >= 0
               && (!investitureLocked || investedDefinition != null)
               && (!investitureLocked || !investedTraits.isEmpty());
    }

    private static int clampPointValue(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    private static Map<String, Integer> readReceptionMap(IModDataView data, ResourceLocation key) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!data.contains(key, Tag.TAG_COMPOUND))
            return result;
        CompoundTag tag = data.getCompound(key);
        for (String slotType : tag.getAllKeys()) {
            int value = tag.getInt(slotType);
            if (value > 0)
                result.put(slotType, value);
        }
        return result;
    }

    private static Map<ResourceLocation, Integer> readResourceMap(IModDataView data, ResourceLocation key) {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        if (!data.contains(key, Tag.TAG_COMPOUND))
            return result;
        CompoundTag tag = data.getCompound(key);
        for (String raw : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id != null)
                result.put(id, tag.getInt(raw));
        }
        return result;
    }

    private static void writeStringMap(ToolDataNBT data, ResourceLocation key, Map<String, Integer> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() > 0)
                tag.putInt(entry.getKey(), entry.getValue());
        }
        if (tag.isEmpty())
            data.remove(key);
        else
            data.put(key, tag);
    }

    private static void writeResourceMap(ToolDataNBT data, ResourceLocation key, Map<ResourceLocation, Integer> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : map.entrySet()) {
            if (entry.getValue() > 0)
                tag.putInt(entry.getKey().toString(), entry.getValue());
        }
        if (tag.isEmpty())
            data.remove(key);
        else
            data.put(key, tag);
    }
}
