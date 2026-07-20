package org.brahypno.esotericismtinker.transcendence.intrinsic;

import slimeknights.tconstruct.library.tools.nbt.IToolContext;

@FunctionalInterface
public interface NoumenonTuningEffect {
    int modifyRejection(IToolContext context, NoumenonData data, int level, int rejection);
}
