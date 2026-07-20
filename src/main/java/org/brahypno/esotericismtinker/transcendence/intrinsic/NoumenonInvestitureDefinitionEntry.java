package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.resources.ResourceLocation;

/**
 * Optional GUI metadata for a source tool definition.
 * Actual borrowed traits are captured from a source ToolStack and stored as a snapshot.
 */
public record NoumenonInvestitureDefinitionEntry(
        ResourceLocation id,
        int minLevel,
        int tuningCost,
        int rejection,
        NoumenonDisplay display
) {
    public boolean canShow(NoumenonData data) {
        return data.level >= minLevel;
    }
}
