package org.brahypno.esotericismtinker.tools.data;

import net.minecraft.data.PackOutput;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.NoumenonModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataArmorConsequenceModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataToolConsequenceModule;
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
                .addModule(StigmataToolConsequenceModule.builder()
                                                        .toolItem(ItemPredicate.tag(TinkerTags.Items.WORN_ARMOR).inverted())
                                                        .build())
                .addModule(StigmataArmorConsequenceModule.builder()
                                                         .toolItem(ItemPredicate.tag(TinkerTags.Items.ARMOR))
                                                         .build());
    }

    @Override
    public @NotNull String getName() {
        return "Esotericism Tinker Modifier Provider";
    }
}
