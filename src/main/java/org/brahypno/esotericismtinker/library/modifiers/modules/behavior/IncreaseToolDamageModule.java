package org.brahypno.esotericismtinker.library.modifiers.modules.behavior;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.library.json.math.ModifierFormula;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.json.variable.VariableFormula;
import slimeknights.tconstruct.library.json.variable.stat.ConditionalStatFormula;
import slimeknights.tconstruct.library.json.variable.stat.ConditionalStatVariable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static slimeknights.tconstruct.TConstruct.RANDOM;

/** Increases durability loss by a percentage, an added amount, or a multiplier. */
public record IncreaseToolDamageModule(IJsonPredicate<LivingEntity> holder, IJsonPredicate<ModifierId> cause,
                                      ConditionalStatFormula formula, ModifierCondition<IToolStackView> condition, Mode mode)
        implements ModifierModule, ToolDamageModifierHook, TooltipModifierHook, ModifierCondition.ConditionalModule<IToolStackView> {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.defaultHooks(ModifierHooks.TOOL_DAMAGE, ModifierHooks.TOOLTIP);
    public static final RecordLoadable<IncreaseToolDamageModule> LOADER = RecordLoadable.create(
            LivingEntityPredicate.LOADER.defaultField("entity", IncreaseToolDamageModule::holder),
            ModifierPredicate.LOADER.defaultField("cause", IncreaseToolDamageModule::cause),
            ConditionalStatFormula.IDENTITY_LOADER.directField(IncreaseToolDamageModule::formula),
            ModifierCondition.TOOL_FIELD,
            new EnumLoadable<>(Mode.class).defaultField("mode", Mode.PERCENT, false, IncreaseToolDamageModule::mode),
            IncreaseToolDamageModule::new);

    public enum Mode {
        PERCENT,
        ADD,
        MULTIPLY
    }

    /** Existing callers continue to use proportional increases. */
    public IncreaseToolDamageModule(IJsonPredicate<LivingEntity> holder, IJsonPredicate<ModifierId> cause,
                                    ConditionalStatFormula formula, ModifierCondition<IToolStackView> condition) {
        this(holder, cause, formula, condition, Mode.PERCENT);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Applies a proportional increase, preserving the original API. */
    public static int increaseDamage(int amount, float percentage) {
        return increaseDamage(amount, percentage, Mode.PERCENT);
    }

    /** Random rounding preserves the expected damage for fractional additions and multipliers. */
    public static int increaseDamage(int amount, float increase, Mode mode) {
        if (amount <= 0 || !(increase > (mode == Mode.MULTIPLY ? 1 : 0))){
            return amount;
        }
        double increased = switch (mode) {
            case PERCENT -> amount * (1.0D + increase);
            case ADD -> amount + (double) increase;
            case MULTIPLY -> amount * (double) increase;
        };
        if (increased >= Integer.MAX_VALUE){
            return Integer.MAX_VALUE;
        }
        int result = (int) increased;
        return result + (RANDOM.nextFloat() < increased - result ? 1 : 0);
    }

    private boolean matchesHolder(@Nullable LivingEntity holder) {
        return holder == null ? this.holder == LivingEntityPredicate.ANY : this.holder.matches(holder);
    }

    @Deprecated
    @Override
    public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder) {
        return beforeDamageTool(tool, modifier, amount, holder, null, ModifierId.EMPTY);
    }

    @Override
    public int beforeDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder,
                                @Nullable ItemStack stack, ModifierId cause) {
        if (condition.matches(tool, modifier) && this.cause.matches(cause) && matchesHolder(holder)){
            return increaseDamage(amount, formula.apply(tool, modifier, holder, 0, 1), mode);
        }
        return amount;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry entry, @Nullable Player player, List<Component> tooltip,
                           TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (condition.matches(tool, entry)){
            float increase = formula.apply(tool, entry, tooltipKey == TooltipKey.SHIFT ? player : null, 0, 1);
            if (increase > (mode == Mode.MULTIPLY ? 1 : 0)){
                Modifier modifier = entry.getModifier();
                String formatted = switch (mode) {
                    case PERCENT -> Util.PERCENT_BOOST_FORMAT.format(increase);
                    case ADD -> Util.BONUS_FORMAT.format(increase);
                    case MULTIPLY -> Util.MULTIPLIER_FORMAT.format(increase);
                };
                tooltip.add(modifier.applyStyle(Component.literal(formatted + " ")
                                                         .append(modifier.getDisplayName())));
            }
        }
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<IncreaseToolDamageModule> getLoader() {
        return LOADER;
    }

    public static class Builder extends VariableFormula.Builder<Builder, IncreaseToolDamageModule, ConditionalStatVariable> {
        private IJsonPredicate<LivingEntity> holder = LivingEntityPredicate.ANY;
        private IJsonPredicate<ModifierId> cause = ModifierPredicate.ANY;
        private Mode mode = Mode.PERCENT;

        private Builder() {
            super(ConditionalStatFormula.VARIABLES);
        }

        public Builder holder(IJsonPredicate<LivingEntity> holder) {
            this.holder = Objects.requireNonNull(holder, "holder");
            return this;
        }

        public Builder cause(IJsonPredicate<ModifierId> cause) {
            this.cause = Objects.requireNonNull(cause, "cause");
            return this;
        }

        /** The formula is the extra amount; eachLevel(n) adds n times the effective level. */
        public Builder additive() {
            this.mode = Mode.ADD;
            return this;
        }

        /** The formula is the total multiplier; eachLevel(n) multiplies by n times the effective level. */
        public Builder multiplicative() {
            this.mode = Mode.MULTIPLY;
            return this;
        }

        /** The formula is the proportional increase, matching the original module behavior. */
        @Override
        public Builder percent() {
            this.mode = Mode.PERCENT;
            return this;
        }

        @Override
        protected IncreaseToolDamageModule build(ModifierFormula formula) {
            return new IncreaseToolDamageModule(holder, cause, new ConditionalStatFormula(formula, variables, percent), condition, mode);
        }
    }
}
