package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

import java.util.Map;

public final class NoumenonLogic {
    private NoumenonLogic() {}

    public static int computeRejection(IToolContext context, NoumenonData data) {
        int rejection = data.level * data.level;
        rejection += data.usedSubstratePoints();
        rejection += data.usedElevationPoints() * 2;
        rejection += data.investitureRejection;

        for (Map.Entry<ResourceLocation, Integer> entry : data.tunings.entrySet()) {
            int level = entry.getValue();
            NoumenonTuningEntry tuning = NoumenonDatabase.tuning(entry.getKey()).orElse(null);
            if (tuning != null)
                rejection = tuning.modifyRejection(context, data, level, rejection);
        }
        return Math.max(0, rejection);
    }
}
