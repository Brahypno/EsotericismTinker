package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

import java.util.Set;

public record NoumenonSublimationGroup(
        ResourceLocation id,
        NoumenonRequirement requirement,
        Set<ResourceLocation> replaces,
        NoumenonDisplay display
) {
    public boolean matches(IToolContext context) {
        return requirement.matches(context);
    }
}
