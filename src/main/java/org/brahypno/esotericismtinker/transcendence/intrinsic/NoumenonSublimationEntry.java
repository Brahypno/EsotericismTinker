package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

public record NoumenonSublimationEntry(
        ResourceLocation id,
        int costPerLevel,
        int maxLevel,
        int minLevel,
        NoumenonRequirement requirement,
        NoumenonSublimationEffect effect,
        NoumenonDisplay display
) {
    public boolean canShow(IToolContext context, NoumenonData data) {
        return data.level >= minLevel && requirement.matches(context);
    }

    public void apply(IToolContext context, ModifierEntry source, int level) {
        effect.apply(context, source, Math.min(level, maxLevel));
    }
}
