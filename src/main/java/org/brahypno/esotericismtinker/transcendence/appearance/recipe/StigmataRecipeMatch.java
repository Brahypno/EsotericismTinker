package org.brahypno.esotericismtinker.transcendence.appearance.recipe;

import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMaterialInput;

import java.util.List;

/**
 * Successful dynamic match and exact per-slot consumption plan.
 */
public record StigmataRecipeMatch(StigmataRecipe recipe, StigmataMaterialInput partMaterial, List<SlotConsumption> materialConsumption) {
    public record SlotConsumption(int slot, int count) {}
}
