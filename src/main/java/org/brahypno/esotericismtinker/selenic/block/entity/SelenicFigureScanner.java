package org.brahypno.esotericismtinker.selenic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicFailure;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;

public final class SelenicFigureScanner {
    private static final int MAX_SPINES = 32;

    private SelenicFigureScanner() {}

    public static SelenicFigure scan(Level level, BlockPos fontPos) {
        int lowerSpines = countLowerSpines(level, fontPos);
        UpperAxis upper = scanUpperAxis(level, fontPos);

        if (!upper.hasCrown()){
            return SelenicFigure.invalid(upper.failure());
        }

        if (lowerSpines + upper.spines() <= 0){
            return SelenicFigure.invalid(SelenicFailure.NO_SPINE);
        }

        return SelenicFigure.valid(
                upper.crownPos(),
                bottomScanPos(fontPos, lowerSpines),
                lowerSpines,
                upper.spines());
    }

    private static int countLowerSpines(Level level, BlockPos fontPos) {
        int count = 0;
        BlockPos cursor = fontPos.below();

        while (level.getBlockState(cursor).is(EsotericismTinkerSelenic.astrolabeSpine.get())) {
            count++;

            if (count > MAX_SPINES){
                return MAX_SPINES;
            }

            cursor = cursor.below();
        }

        return count;
    }

    private static UpperAxis scanUpperAxis(Level level, BlockPos fontPos) {
        int spines = 0;
        BlockPos cursor = fontPos.above(2);

        while (level.getBlockState(cursor).is(EsotericismTinkerSelenic.astrolabeSpine.get())) {
            spines++;

            if (spines > MAX_SPINES){
                return UpperAxis.invalid(SelenicFailure.AXIS_BLOCKED);
            }

            cursor = cursor.above();
        }

        BlockState terminal = level.getBlockState(cursor);

        if (terminal.is(EsotericismTinkerSelenic.armillaryCrown.get())){
            return new UpperAxis(true, SelenicFailure.NONE, cursor, spines);
        }

        if (terminal.isAir()){
            return UpperAxis.invalid(SelenicFailure.NO_CROWN);
        }

        return UpperAxis.invalid(SelenicFailure.AXIS_BLOCKED);
    }

    private static BlockPos bottomScanPos(BlockPos fontPos, int lowerSpines) {
        return lowerSpines > 0 ? fontPos.below(lowerSpines) : fontPos;
    }

    private record UpperAxis(
            boolean hasCrown,
            SelenicFailure failure,
            BlockPos crownPos,
            int spines
    ) {
        private static UpperAxis invalid(SelenicFailure failure) {
            return new UpperAxis(false, failure, BlockPos.ZERO, 0);
        }
    }
}