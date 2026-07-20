package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

public record NoumenonTuningEntry(
        ResourceLocation id,
        int costPerLevel,
        int maxLevel,
        int minLevel,
        NoumenonRequirement requirement,
        NoumenonTuningEffect effect,
        NoumenonDisplay display
) {
    public boolean canShow(IToolContext context, NoumenonData data) {
        return data.level >= minLevel && requirement.matches(context);
    }

    public int modifyRejection(IToolContext context, NoumenonData data, int level, int rejection) {
        return effect.modifyRejection(context, data, Math.min(level, maxLevel), rejection);
    }
}
