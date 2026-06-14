package org.brahypno.esotericismtinker.selenic.block.entity;

import net.minecraft.core.BlockPos;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicFailure;

import javax.annotation.Nullable;

public record SelenicFigure(
        boolean valid,
        SelenicFailure failure,
        @Nullable BlockPos crownPos,
        @Nullable BlockPos bottomPos,
        int lowerSpines,
        int upperSpines
) {
    public int totalSpines() {
        return lowerSpines + upperSpines;
    }

    public static SelenicFigure invalid(SelenicFailure failure) {
        return new SelenicFigure(false, failure, null, null, 0, 0);
    }

    public static SelenicFigure valid(
            BlockPos crownPos,
            BlockPos bottomPos,
            int lowerSpines,
            int upperSpines) {
        return new SelenicFigure(true, SelenicFailure.NONE, crownPos, bottomPos, lowerSpines, upperSpines);
    }
}