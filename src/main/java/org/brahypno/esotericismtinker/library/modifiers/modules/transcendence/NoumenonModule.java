package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonData;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonDatabase;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonKeys;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonLogic;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonSublimationEntry;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierTraitHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Applies the stored Noumenon allocation to a tool.
 */
public record NoumenonModule(ModifierCondition<IToolContext> condition)
        implements ModifierModule, ConditionalModule<IToolContext>,
        VolatileDataModifierHook, ModifierTraitHook, ValidateModifierHook,
        ModifierRemovalHook, TooltipModifierHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<NoumenonModule>defaultHooks(
            ModifierHooks.VOLATILE_DATA,
            ModifierHooks.MODIFIER_TRAITS,
            ModifierHooks.VALIDATE,
            ModifierHooks.REMOVE,
            ModifierHooks.TOOLTIP
    );

    public static final RecordLoadable<NoumenonModule> LOADER = RecordLoadable.create(
            ModifierCondition.CONTEXT_FIELD,
            NoumenonModule::new
    );

    public static final NoumenonModule INSTANCE =
            new NoumenonModule(ModifierCondition.ANY_CONTEXT);

    public NoumenonModule() {
        this(ModifierCondition.ANY_CONTEXT);
    }

    @Override
    public RecordLoadable<NoumenonModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier,
                                ToolDataNBT volatileData) {
        if (!condition.matches(context, modifier)) {
            return;
        }

        NoumenonData data = NoumenonData.read(context);
        addReceptionSlots(data, volatileData);
        volatileData.putInt(NoumenonKeys.REJECTION, NoumenonLogic.computeRejection(context, data));
    }

    @Override
    public void addTraits(IToolContext context, ModifierEntry modifier,
                          ModifierTraitHook.TraitBuilder builder,
                          boolean firstEncounter) {
        if (!condition.matches(context, modifier)) {
            return;
        }

        NoumenonData data = NoumenonData.read(context);
        addInvestitureSnapshotTraits(data, builder);
        applySublimations(context, modifier, data);
    }

    @Nullable
    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        if (!condition.matches(tool, modifier) || NoumenonData.read(tool).isValid()) {
            return null;
        }
        return Component.translatable("modifier.esotericism_tinker.noumenon_core.invalid_points");
    }

    @Override
    public Component onRemoved(IToolStackView tool, Modifier modifier) {
        return Component.translatable("modifier.esotericism_tinker.noumenon_core.cannot_remove");
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player,
                           List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (!condition.matches(tool, modifier) || !tooltipKey.isShiftOrUnknown()) {
            return;
        }

        for (Map.Entry<String, Integer> chosen : NoumenonData.read(tool).receptionSlots.entrySet()) {
            SlotType slotType = SlotType.getIfPresent(chosen.getKey());
            if (slotType != null) {
                tooltip.add(formatCount(slotType, chosen.getValue()));
            }
        }
    }

    private static void addReceptionSlots(NoumenonData data, ToolDataNBT volatileData) {
        for (Map.Entry<String, Integer> chosen : data.receptionSlots.entrySet()) {
            SlotType type = SlotType.getIfPresent(chosen.getKey());
            if (type != null) {
                volatileData.addSlots(type, chosen.getValue());
            }
        }
    }

    private static void addInvestitureSnapshotTraits(NoumenonData data,
                                                     ModifierTraitHook.TraitBuilder builder) {
        if (!data.hasInvestitureSnapshot()) {
            return;
        }

        for (Map.Entry<ResourceLocation, Integer> entry : data.investedTraits.entrySet()) {
            ModifierId id = new ModifierId(entry.getKey());
            int level = entry.getValue();
            if (level > 0 && ModifierManager.INSTANCE.contains(id)) {
                builder.add(new ModifierEntry(id, level));
            }
        }
    }

    private static void applySublimations(IToolContext context, ModifierEntry modifier,
                                          NoumenonData data) {
        for (Map.Entry<ResourceLocation, Integer> chosen : data.sublimations.entrySet()) {
            NoumenonSublimationEntry entry =
                    NoumenonDatabase.sublimation(chosen.getKey()).orElse(null);
            if (entry != null) {
                entry.apply(context, modifier, chosen.getValue());
            }
        }
    }

    private static Component formatCount(SlotType slotType, int count) {
        String key = count > 0
                ? "modifier.esotericism_tinker.noumenon_crown.reception_slot.positive"
                : "modifier.esotericism_tinker.noumenon_crown.reception_slot";
        return Component.translatable(key, count, slotType.getDisplayName())
                .withStyle(style -> style.withColor(slotType.getColor()));
    }
}
