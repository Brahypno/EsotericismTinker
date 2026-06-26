package org.brahypno.esotericismtinker.library.modifiers.modules.build;

import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.loadable.field.LegacyField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;

import java.util.List;

/**
 * Module that adds extra modifier slots of every registered slot type to a tool.
 */
public record AllSlotModule(LevelingInt count,
                            ModifierCondition<IToolContext> condition) implements VolatileDataModifierHook, ModifierModule, ConditionalModule<IToolContext> {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<AllSlotModule>defaultHooks(ModifierHooks.VOLATILE_DATA);

    public static final RecordLoadable<AllSlotModule> LOADER = RecordLoadable.create(
            IntLoadable.ANY_SHORT.defaultField("flat", 0, m -> m.count.flat()),
            // Keep parity with ModifierSlotModule legacy json shape.
            new LegacyField<>(IntLoadable.ANY_SHORT.defaultField("each_level", 0, m -> m.count.eachLevel()), "count"),
            ModifierCondition.CONTEXT_FIELD,
            (flat, leveling, condition) -> new AllSlotModule(new LevelingInt(flat, leveling), condition)
    );

    /**
     * @apiNote Internal constructor, use {@link #builder()}
     */
    @Internal
    public AllSlotModule {}

    public AllSlotModule(int count, ModifierCondition<IToolContext> condition) {
        this(LevelingInt.eachLevel(count), condition);
    }

    public AllSlotModule(int count) {
        this(count, ModifierCondition.ANY_CONTEXT);
    }

    public AllSlotModule() {
        this(1);
    }

    @Override
    public Integer getPriority() {
        // Same priority as ModifierSlotModule so slot modules group together.
        return 50;
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
        if (!condition.matches(context, modifier)){
            return;
        }

        int amount = count.computeForLevel(modifier.getEffectiveLevel());
        if (amount == 0){
            return;
        }

        for (SlotType type : SlotType.getAllSlotTypes()) {
            volatileData.addSlots(type, amount);
        }
    }

    @Override
    public RecordLoadable<AllSlotModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    /**
     * Creates a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for the module
     */
    public static class Builder extends ModuleBuilder.Context<Builder> implements LevelingInt.Builder<AllSlotModule> {
        @Override
        public AllSlotModule amount(int flat, int eachLevel) {
            return new AllSlotModule(new LevelingInt(flat, eachLevel), condition);
        }
    }
}