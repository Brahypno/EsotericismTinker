package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime cost authority for Noumenon Crown.
 * <p>
 * Costs are data-driven. Java code may generate default JSON, but runtime should
 * not depend on Java registration for these values.
 */
public final class NoumenonCostDatabase {
    public static final int DEFAULT_PRESENT_SLOT_RECEPTION_COST = 2;
    public static final int DEFAULT_INVESTITURE_TRAIT_ELEVATION_COST = 1;

    private static final Map<String, Integer> RECEPTION_SLOT_COSTS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Map<ResourceLocation, Integer>> INVESTITURE_TRAIT_COSTS = new LinkedHashMap<>();

    private NoumenonCostDatabase() {}

    public static void replaceReceptionSlotCosts(Map<String, Integer> costs) {
        RECEPTION_SLOT_COSTS.clear();
        costs.forEach((slotType, cost) -> {
            if (slotType != null && !slotType.isBlank() && cost >= 0)
                RECEPTION_SLOT_COSTS.put(slotType, cost);
        });
    }

    public static void replaceInvestitureTraitCosts(Map<ResourceLocation, Map<ResourceLocation, Integer>> costs) {
        INVESTITURE_TRAIT_COSTS.clear();
        costs.forEach((definition, traits) -> {
            Map<ResourceLocation, Integer> copy = new LinkedHashMap<>();
            traits.forEach((trait, cost) -> {
                if (trait != null && cost >= 0)
                    copy.put(trait, cost);
            });
            if (!copy.isEmpty())
                INVESTITURE_TRAIT_COSTS.put(definition, copy);
        });
    }

    public static void putReceptionSlotCost(String slotType, int cost) {
        if (slotType != null && !slotType.isBlank() && cost >= 0)
            RECEPTION_SLOT_COSTS.put(slotType, cost);
    }

    public static void putInvestitureTraitCost(ResourceLocation toolDefinition, ResourceLocation trait, int cost) {
        if (toolDefinition == null || trait == null || cost < 0)
            return;
        INVESTITURE_TRAIT_COSTS.computeIfAbsent(toolDefinition, ignored -> new LinkedHashMap<>()).put(trait, cost);
    }

    public static int getReceptionSlotCost(String slotType) {
        if (slotType == null || slotType.isBlank())
            return 0;

        // If the slot type is not present in this runtime, it is treated as a missing addon slot.
        // Keep the saved data, but do not charge points and do not add the slot.
        if (SlotType.getIfPresent(slotType) == null)
            return 0;

        // If the slot exists but no data overrides it, default to 2.
        return RECEPTION_SLOT_COSTS.getOrDefault(slotType, DEFAULT_PRESENT_SLOT_RECEPTION_COST);
    }

    public static int getInvestitureTraitCost(ResourceLocation toolDefinition, ResourceLocation trait) {
        if (toolDefinition == null || trait == null)
            return 0;

        // Missing addon modifier: keep the saved snapshot, but do not charge and do not inject it.
        if (!ModifierManager.INSTANCE.contains(new ModifierId(trait)))
            return 0;

        return INVESTITURE_TRAIT_COSTS
                .getOrDefault(toolDefinition, Map.of())
                .getOrDefault(trait, DEFAULT_INVESTITURE_TRAIT_ELEVATION_COST);
    }

    public static Map<String, Integer> receptionSlotCostsView() {
        return Map.copyOf(RECEPTION_SLOT_COSTS);
    }

    public static Map<ResourceLocation, Map<ResourceLocation, Integer>> investitureTraitCostsView() {
        Map<ResourceLocation, Map<ResourceLocation, Integer>> copy = new LinkedHashMap<>();
        INVESTITURE_TRAIT_COSTS.forEach((definition, traits) -> copy.put(definition, Map.copyOf(traits)));
        return Map.copyOf(copy);
    }
}
