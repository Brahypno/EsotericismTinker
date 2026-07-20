package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class NoumenonDatabase {
    public static final int DEFAULT_INVESTITURE_REJECTION = 4;

    private static final Map<ResourceLocation, NoumenonReceptionEntry> RECEPTION_SLOTS = new LinkedHashMap<>();
    private static final Map<String, NoumenonReceptionEntry> RECEPTION_BY_SLOT_TYPE = new LinkedHashMap<>();
    private static final Map<ResourceLocation, NoumenonSublimationEntry> SUBLIMATIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, NoumenonTuningEntry> TUNINGS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, NoumenonInvestitureDefinitionEntry> INVESTITURES = new LinkedHashMap<>();

    private NoumenonDatabase() {}

    /**
     * GUI/effect registration only. Slot costs are data-driven by {@link NoumenonCostDatabase}.
     */
    public static void registerReception(NoumenonReceptionEntry entry) {
        RECEPTION_SLOTS.put(entry.id(), entry);
        RECEPTION_BY_SLOT_TYPE.put(entry.slotType(), entry);
    }

    public static void registerSublimation(NoumenonSublimationEntry entry) {
        SUBLIMATIONS.put(entry.id(), entry);
    }

    public static void registerTuning(NoumenonTuningEntry entry) {
        TUNINGS.put(entry.id(), entry);
    }

    public static void registerInvestitureDefinition(NoumenonInvestitureDefinitionEntry entry) {
        INVESTITURES.put(entry.id(), entry);
    }

    public static Optional<NoumenonReceptionEntry> reception(ResourceLocation id) {
        return Optional.ofNullable(RECEPTION_SLOTS.get(id));
    }

    public static Optional<NoumenonReceptionEntry> receptionBySlotType(String slotType) {
        return Optional.ofNullable(RECEPTION_BY_SLOT_TYPE.get(slotType));
    }

    public static Optional<NoumenonSublimationEntry> sublimation(ResourceLocation id) {
        return Optional.ofNullable(SUBLIMATIONS.get(id));
    }

    public static Optional<NoumenonTuningEntry> tuning(ResourceLocation id) {
        return Optional.ofNullable(TUNINGS.get(id));
    }

    public static Optional<NoumenonInvestitureDefinitionEntry> investitureDefinition(ResourceLocation id) {
        return Optional.ofNullable(INVESTITURES.get(id));
    }

    public static Collection<NoumenonReceptionEntry> visibleReceptions(IToolContext context, NoumenonData data) {
        return RECEPTION_SLOTS.values().stream().filter(entry -> entry.canShow(context, data)).toList();
    }

    public static Collection<NoumenonSublimationEntry> visibleSublimations(IToolContext context, NoumenonData data) {
        return SUBLIMATIONS.values().stream().filter(entry -> entry.canShow(context, data)).toList();
    }

    public static Collection<NoumenonTuningEntry> visibleTunings(IToolContext context, NoumenonData data) {
        return TUNINGS.values().stream().filter(entry -> entry.canShow(context, data)).toList();
    }

    public static Collection<NoumenonInvestitureDefinitionEntry> visibleInvestitureDefinitions(NoumenonData data) {
        return INVESTITURES.values().stream().filter(entry -> entry.canShow(data)).toList();
    }

    public static Collection<NoumenonReceptionEntry> allReceptions() {return RECEPTION_SLOTS.values();}

    public static Collection<NoumenonSublimationEntry> allSublimations() {return SUBLIMATIONS.values();}

    public static Collection<NoumenonTuningEntry> allTunings() {return TUNINGS.values();}

    public static Collection<NoumenonInvestitureDefinitionEntry> allInvestitureDefinitions() {return INVESTITURES.values();}

    public static int getReceptionCost(String slotType) {
        return NoumenonCostDatabase.getReceptionSlotCost(slotType);
    }

    public static int getSublimationCost(ResourceLocation id) {
        return sublimation(id).map(NoumenonSublimationEntry::costPerLevel).orElse(0);
    }

    public static int getTuningCost(ResourceLocation id) {
        return tuning(id).map(NoumenonTuningEntry::costPerLevel).orElse(0);
    }

    public static int getInvestitureTraitCost(ResourceLocation toolDefinition, ResourceLocation trait) {
        return NoumenonCostDatabase.getInvestitureTraitCost(toolDefinition, trait);
    }

    public static int getInvestitureRejection(ResourceLocation id) {
        return investitureDefinition(id).map(NoumenonInvestitureDefinitionEntry::rejection).orElse(DEFAULT_INVESTITURE_REJECTION);
    }
}
