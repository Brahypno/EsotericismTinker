package org.brahypno.esotericismtinker.common.data.tags;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import slimeknights.tconstruct.library.data.tinkering.AbstractModifierTagProvider;

import static slimeknights.tconstruct.common.TinkerTags.Modifiers.EXTRACT_MODIFIER_BLACKLIST;
import static slimeknights.tconstruct.common.TinkerTags.Modifiers.REMOVE_MODIFIER_BLACKLIST;

/** Generates TConstruct modifier tags owned by Esotericism Tinker. */
public class ModifierTagProvider extends AbstractModifierTagProvider {
    public ModifierTagProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, EsotericismTinker.MODID, existingFileHelper);
    }

    @Override
    protected void addTags() {
        tag(REMOVE_MODIFIER_BLACKLIST).add(EsotericismTinkerModifiers.NOUMENON_CROWN);
        tag(EXTRACT_MODIFIER_BLACKLIST).add(EsotericismTinkerModifiers.NOUMENON_CROWN);
        tag(REMOVE_MODIFIER_BLACKLIST).add(EsotericismTinkerModifiers.STIGMATA);
        tag(EXTRACT_MODIFIER_BLACKLIST).add(EsotericismTinkerModifiers.STIGMATA);
    }

    @Override
    public String getName() {
        return "Esotericism Tinker Modifier Tag Provider";
    }
}
