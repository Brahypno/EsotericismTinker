package org.brahypno.esotericismtinker.transcendence.intrinsic;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

@FunctionalInterface
public interface NoumenonSublimationEffect {
    void apply(IToolContext context, ModifierEntry source, int level);
}
