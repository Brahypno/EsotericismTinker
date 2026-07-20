package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;

/**
 * Applies Noumenon allocations transactionally after simulating a full TConstruct rebuild.
 */
public final class NoumenonAllocationLogic {
    private NoumenonAllocationLogic() {}

    @FunctionalInterface
    public interface Mutation {
        void apply(NoumenonData data);
    }

    /**
     * Returns an error without touching {@code target}, or commits the tested mutation and returns null.
     */
    @Nullable
    public static Component validateAndApply(ToolStack target, Mutation mutation) {
        ToolStack simulated = target.copy();
        NoumenonData simulatedData = NoumenonData.read(simulated);
        mutation.apply(simulatedData);
        simulatedData.write(simulated.getPersistentData());

        if (simulatedData.remainingSubstratePoints() < 0){
            return Component.translatable(
                    "command.esotericism_tinker.noumenon_test.not_enough_substrate_after_allocation",
                    simulatedData.remainingSubstratePoints());
        }
        if (simulatedData.remainingElevationPoints() < 0){
            return Component.translatable(
                    "command.esotericism_tinker.noumenon_test.not_enough_elevation_after_allocation",
                    simulatedData.remainingElevationPoints());
        }

        simulated.rebuildStats();
        Component validation = simulated.tryValidate();
        if (validation != null){
            return Component.translatable(
                    "command.esotericism_tinker.noumenon_test.tconstruct_validation_failed", validation);
        }

        NoumenonData targetData = NoumenonData.read(target);
        mutation.apply(targetData);
        targetData.write(target.getPersistentData());
        target.rebuildStats();
        return null;
    }
}
