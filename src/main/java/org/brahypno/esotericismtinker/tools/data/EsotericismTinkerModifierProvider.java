package org.brahypno.esotericismtinker.tools.data;

import net.minecraft.data.PackOutput;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataCommonConsequenceModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataDefensiveConsequenceModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.NoumenonModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataOffensiveConsequenceModule;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.data.predicate.item.ItemPredicate;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.data.tinkering.AbstractModifierProvider;
import slimeknights.tconstruct.library.modifiers.util.ModifierLevelDisplay;

public class EsotericismTinkerModifierProvider
        extends AbstractModifierProvider {
    public EsotericismTinkerModifierProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addModifiers() {
        buildModifier(EsotericismTinkerModifiers.NOUMENON_CROWN)
                .levelDisplay(new ModifierLevelDisplay.UniqueForLevels(3))
                .addModule(NoumenonModule.INSTANCE);

        buildModifier(EsotericismTinkerModifiers.STIGMATA)
                .levelDisplay(new ModifierLevelDisplay.UniqueForLevels(3))
                .addModule(StigmataModule.INSTANCE)
                .addModule(StigmataCommonConsequenceModule.builder().build())
                .addModule(StigmataOffensiveConsequenceModule.builder()
                                                             .toolItem(ItemPredicate.tag(TinkerTags.Items.ARMOR).inverted())
                                                             .build())
                .addModule(StigmataDefensiveConsequenceModule.builder()
                                                             .toolItem(ItemPredicate.tag(TinkerTags.Items.ARMOR))
                                                             .build());
    }

    @Override
    public @NotNull String getName() {
        return "Esotericism Tinker Modifier Provider";
    }
}
