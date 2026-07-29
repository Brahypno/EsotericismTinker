package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataEntry;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataOverloadDegree;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierTraitHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DisplayNameModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Adds the material traits selected by the tool's active Stigmata entries.
 */
public record StigmataModule(ModifierCondition<IToolContext> condition)
        implements ModifierModule, ConditionalModule<IToolContext>, ModifierTraitHook, TooltipModifierHook, DisplayNameModifierHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS =
            HookProvider.<StigmataModule>defaultHooks(ModifierHooks.MODIFIER_TRAITS, ModifierHooks.TOOLTIP, ModifierHooks.DISPLAY_NAME);

    public static final RecordLoadable<StigmataModule> LOADER = RecordLoadable.create(
            ModifierCondition.CONTEXT_FIELD,
            StigmataModule::new
    );

    public static final StigmataModule INSTANCE =
            new StigmataModule(ModifierCondition.ANY_CONTEXT);

    @Override
    public Component getDisplayName(IToolStackView tool, ModifierEntry entry, Component name, @Nullable RegistryAccess access) {
        StigmataData data = StigmataData.read(tool);
        int level = data.hasConsequenceSeed() ? data.stage() : entry.getLevel();
        String key = entry.getModifier().getTranslationKey();
        return name.copy().withStyle(style -> style.withColor(ResourceColorManager.getTextColor(key + "." + level)));
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
    public void addTraits(IToolContext context, ModifierEntry modifier, TraitBuilder builder, boolean firstEncounter) {
        if (!firstEncounter || !condition.matches(context, modifier)){
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

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (!condition.matches(tool, modifier))
            return;
        StigmataData data = StigmataData.read(tool);
        int overload = data.overload(tool);
        int burden = data.burden();
        if (0 < overload)
            if (data.hasConsequenceSeed())
                tooltip.add(Component.translatable(
                        "modifier.esotericism_tinker.stigmata.overload.tooltip",
                        StigmataOverloadDegree.fromValues(overload, burden).displayName(),
                        data.consequence().displayName()));
            else
                tooltip.add(Component.translatable("modifier.esotericism_tinker.stigmata.overload.tooltip_1",
                                                   StigmataOverloadDegree.fromValues(overload, burden).displayName()));

    }
}
