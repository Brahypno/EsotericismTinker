package org.brahypno.esotericismtinker.transcendence.appearance;

import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

/**
 * Material identity, tier, and ritual value contributed by one input item.
 */
public record StigmataMaterialInput(MaterialVariantId variant, MaterialId material, int tier, double unitsPerItem) {}
