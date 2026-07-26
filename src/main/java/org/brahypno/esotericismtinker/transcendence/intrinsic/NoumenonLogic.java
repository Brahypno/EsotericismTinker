package org.brahypno.esotericismtinker.transcendence.intrinsic;

import slimeknights.tconstruct.library.tools.nbt.IToolContext;

public final class NoumenonLogic {
    private NoumenonLogic() {}

    public static int computeRejection(IToolContext context, NoumenonData data) {
        int rejection = data.level * data.level;
        rejection += data.usedSubstratePoints();
        rejection += data.usedElevationPoints() * 2;
        rejection += data.investitureRejection;

        rejection -= Math.max(0, data.tuning);
        return Math.max(0, rejection);
    }
}
