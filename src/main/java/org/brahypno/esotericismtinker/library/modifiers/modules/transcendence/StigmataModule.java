package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataEntry;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierTraitHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

import java.util.List;

/**
 * Adds the material traits selected by the tool's active Stigmata entries.
 */
public record StigmataModule(ModifierCondition<IToolContext> condition)
        implements ModifierModule, ConditionalModule<IToolContext>, ModifierTraitHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS =
            HookProvider.<StigmataModule>defaultHooks(ModifierHooks.MODIFIER_TRAITS);

    public static final RecordLoadable<StigmataModule> LOADER = RecordLoadable.create(
            ModifierCondition.CONTEXT_FIELD,
            StigmataModule::new
    );

    public static final StigmataModule INSTANCE =
            new StigmataModule(ModifierCondition.ANY_CONTEXT);

    public StigmataModule() {
        this(ModifierCondition.ANY_CONTEXT);
    }

    @Override
    public RecordLoadable<StigmataModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public void addTraits(IToolContext context, ModifierEntry modifier,
                          TraitBuilder builder, boolean firstEncounter) {
        if (!firstEncounter || !condition.matches(context, modifier)) {
            return;
        }

        StigmataData data = StigmataData.read(context);
        for (StigmataEntry entry : data.activeEntries()) {
            for (ModifierEntry trait : MaterialRegistry.getInstance()
                    .getTraits(entry.materialId(), entry.statType())) {
                builder.add(trait);
            }
        }
    }
}
