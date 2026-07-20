package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

/**
 * GUI/database metadata for one Reception slot option.
 *
 * <p>Important: the persisted tool data should store {@link #slotType()} as the key,
 * not {@link #id()}. This mirrors TConstruct's creative-slot modifier and keeps
 * external slot types open to addons.</p>
 */
public record NoumenonReceptionEntry(
        ResourceLocation id,
        String slotType,
        int cost,
        int maxCount,
        int minLevel,
        NoumenonRequirement requirement,
        NoumenonDisplay display
) {
    public boolean canShow(IToolContext context, NoumenonData data) {
        return data.level >= minLevel && requirement.matches(context);
    }
}
