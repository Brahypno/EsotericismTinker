package org.brahypno.esotericismtinker.library.modifiers;

import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.tconstruct.library.module.HookProvider;

import javax.annotation.Nullable;

public interface EsotericismTinkerModifierModule extends GenericLoaderRegistry.IHaveLoader, HookProvider {
    GenericLoaderRegistry<slimeknights.tconstruct.library.modifiers.modules.ModifierModule> LOADER =
            new GenericLoaderRegistry<>("Esotericism Tinker Modifier Module", false);

    @Nullable
    default Integer getPriority() {
        return null;
    }
}
