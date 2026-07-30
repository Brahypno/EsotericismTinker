package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects.ConsequenceState;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

/**
 * Shared state lookup for the common, offensive, and defensive Stigmata hook bridges.
 */
interface StigmataConsequenceModuleBridge extends ConditionalModule<IToolContext> {
    @Nullable
    static ConsequenceState getActiveState(IToolStackView tool) {
        StigmataData data = StigmataData.read(tool);
        if (!data.hasConsequenceSeed() || 0 >= data.stage()){
            return null;
        }
        ConsequenceState state = ConsequenceState.of(tool, data);
        return 0 < state.overload() ? state : null;
    }

    @Nullable
    default ConsequenceState getOwnActiveState(
            IToolStackView tool, ModifierEntry modifier) {
        return condition().matches(tool, modifier) ? getActiveState(tool) : null;
    }

    @Override
    ModifierCondition<IToolContext> condition();
}
