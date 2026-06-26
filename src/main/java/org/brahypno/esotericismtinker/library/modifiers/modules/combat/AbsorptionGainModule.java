package org.brahypno.esotericismtinker.library.modifiers.modules.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.modifiers.EsotericismTinkerHook;
import org.brahypno.esotericismtinker.library.modifiers.hook.ProjectileHurtHook;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MonsterMeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule.SlotInCharge;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;
import java.util.List;


/**
 * Module that converts damage dealt or taken into absorption for the holder.
 * <p>
 * holder:
 * - weapon: attacker / projectile owner
 * - armor: wearer
 * <p>
 * target:
 * - weapon: damaged entity
 * - armor: attacking/source entity, if present
 */
public interface AbsorptionGainModule extends ModifierModule, ConditionalModule<IToolStackView> {
    /**
     * Shared chance field
     */
    RecordField<LevelingValue, AbsorptionGainModule> CHANCE_FIELD =
            LevelingValue.LOADABLE.defaultField("chance", LevelingValue.flat(1), AbsorptionGainModule::chance);

    /**
     * Predicate on the entity gaining absorption
     */
    RecordField<IJsonPredicate<LivingEntity>, AbsorptionGainModule> HOLDER_FIELD =
            LivingEntityPredicate.LOADER.defaultField("holder", LivingEntityPredicate.ANY, AbsorptionGainModule::holder);

    /**
     * Predicate on the entity damaged by / damaging the holder
     */
    RecordField<IJsonPredicate<LivingEntity>, AbsorptionGainModule> TARGET_FIELD =
            LivingEntityPredicate.LOADER.defaultField("target", LivingEntityPredicate.ANY, AbsorptionGainModule::target);

    /**
     * Damage to absorption conversion ratio
     */
    RecordField<LevelingValue, AbsorptionGainModule> RATIO_FIELD =
            LevelingValue.LOADABLE.requiredField("ratio", AbsorptionGainModule::ratio);

    /**
     * Max absorption as holder max health ratio
     */
    RecordField<LevelingValue, AbsorptionGainModule> MAX_RATIO_FIELD =
            LevelingValue.LOADABLE.defaultField("max_ratio", LevelingValue.flat(2.0f), AbsorptionGainModule::maxRatio);

    /**
     * Chance of applying absorption gain
     */
    default LevelingValue chance() {
        return LevelingValue.flat(1);
    }

    /**
     * Entity receiving absorption
     */
    default IJsonPredicate<LivingEntity> holder() {
        return LivingEntityPredicate.ANY;
    }

    /**
     * Other living entity in the interaction
     */
    default IJsonPredicate<LivingEntity> target() {
        return LivingEntityPredicate.ANY;
    }

    /**
     * Damage-to-absorption conversion ratio
     */
    LevelingValue ratio();

    /**
     * Maximum absorption compared to holder max health
     */
    default LevelingValue maxRatio() {
        return LevelingValue.flat(2.0f);
    }

    /**
     * Checks if the module passes chance
     */
    default boolean checkChance(float level) {
        float chance = chance().compute(level);
        return !(chance > 0) || (!(chance >= 1) && !(TConstruct.RANDOM.nextFloat() < chance));
    }

    /**
     * Checks if the module passes chance
     */
    default boolean checkChance(ModifierEntry modifier) {
        return checkChance(modifier.getEffectiveLevel());
    }

    /**
     * Adds absorption to the holder
     */
    default void addAbsorption(LivingEntity holder, float damage, float scaledLevel, ModifierEntry modifier) {
        float current = holder.getAbsorptionAmount();
        float max = holder.getMaxHealth() * maxRatio().compute(modifier.getEffectiveLevel());
        if (damage <= 0 || current >= max){
            return;
        }

        float conversion = scaledLevel * ratio().compute(modifier.getEffectiveLevel());
        if (conversion <= 0){
            return;
        }

        holder.setAbsorptionAmount(Math.min(current + damage * conversion, max));
    }

    /**
     * Creates a builder instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for datagen/provider usage
     */
    class Builder extends ModuleBuilder.Stack<Builder> {
        private IJsonPredicate<LivingEntity> holder = LivingEntityPredicate.ANY;
        private IJsonPredicate<LivingEntity> target = LivingEntityPredicate.ANY;
        @Nullable
        private LevelingValue chance = LevelingValue.flat(1);
        private LevelingValue ratio = LevelingValue.flat(1.0f);
        private LevelingValue maxRatio = LevelingValue.flat(2.0f);

        private Builder() {}

        public Builder holder(IJsonPredicate<LivingEntity> holder) {
            this.holder = holder;
            return this;
        }

        public Builder target(IJsonPredicate<LivingEntity> target) {
            this.target = target;
            return this;
        }

        public Builder chance(LevelingValue chance) {
            this.chance = chance;
            return this;
        }

        public Builder ratio(LevelingValue ratio) {
            this.ratio = ratio;
            return this;
        }

        public Builder maxRatio(LevelingValue maxRatio) {
            this.maxRatio = maxRatio;
            return this;
        }

        public Weapon buildWeapon() {
            return new Weapon(chance, holder, target, ratio, maxRatio, condition);
        }

        public Armor buildArmor() {
            return new Armor(chance, holder, target, ratio, maxRatio, condition);
        }
    }

    /**
     * Weapon implementation: melee and projectile hurt grant absorption to the attacker/holder.
     */
    record Weapon(
            LevelingValue chance,
            IJsonPredicate<LivingEntity> holder,
            IJsonPredicate<LivingEntity> target,
            LevelingValue ratio,
            LevelingValue maxRatio,
            ModifierCondition<IToolStackView> condition
    ) implements AbsorptionGainModule, MeleeHitModifierHook, MonsterMeleeHitModifierHook, ProjectileHurtHook {
        private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<Weapon>defaultHooks(
                ModifierHooks.MELEE_HIT,
                ModifierHooks.MONSTER_MELEE_HIT
        );

        public static final RecordLoadable<Weapon> LOADER = RecordLoadable.create(
                CHANCE_FIELD,
                HOLDER_FIELD,
                TARGET_FIELD,
                RATIO_FIELD,
                MAX_RATIO_FIELD,
                ModifierCondition.TOOL_FIELD,
                Weapon::new
        );

        /**
         * @apiNote use {@link AbsorptionGainModule.Builder#buildWeapon()}
         */
        @Internal
        public Weapon {}

        @Override
        public RecordLoadable<? extends Weapon> getLoader() {
            return LOADER;
        }

        @Override
        public List<ModuleHook<?>> getDefaultHooks() {
            return DEFAULT_HOOKS;
        }

        @Override
        public void addModules(slimeknights.tconstruct.library.module.ModuleHookMap.Builder builder) {
            builder.addHook(this, EsotericismTinkerHook.PROJECTILE_HURT);
        }

        @Override
        public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
            if (context.isCritical() && !context.getAttacker().level().isClientSide){
                onMonsterMeleeHit(tool, modifier, context, damageDealt);
            }
        }

        @Override
        public void onMonsterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage) {
            if (damage <= 0 || !condition.matches(tool, modifier) || checkChance(modifier)){
                return;
            }

            LivingEntity attacker = context.getAttacker();
            LivingEntity damaged = context.getLivingTarget();
            if (!holder.matches(attacker) || null != damaged && !target.matches(damaged)){
                return;
            }

            float scaledLevel = modifier.getEffectiveLevel() * tool.getMultiplier(ToolStats.ATTACK_DAMAGE);
            addAbsorption(attacker, damage, scaledLevel, modifier);
        }

        @Override
        public float modifyProjectileHurt(
                ModifierNBT modifiers,
                ModDataNBT persistentData,
                ModifierEntry modifier,
                Projectile projectile,
                DamageSource source,
                @Nullable LivingEntity attacker,
                LivingEntity target,
                float amount
        ) {
            if (attacker == null || attacker.level().isClientSide || amount <= 0 || checkChance(modifier)){
                return amount;
            }

            // ProjectileHurtHook does not provide IToolStackView, so only the modifier-level part of condition can be checked here.
            if (!condition.modifierLevel().test(modifier.getLevel())){
                return amount;
            }

            if (holder.matches(attacker) && this.target.matches(target)){
                addAbsorption(attacker, amount, modifier.getEffectiveLevel(), modifier);
            }

            return amount;
        }
    }

    /**
     * Armor implementation: damage taken grants absorption to the wearer.
     */
    @Internal
    record Armor(
            LevelingValue chance,
            IJsonPredicate<LivingEntity> holder,
            IJsonPredicate<LivingEntity> target,
            LevelingValue ratio,
            LevelingValue maxRatio,
            ModifierCondition<IToolStackView> condition
    ) implements AbsorptionGainModule, ModifyDamageModifierHook {
        private static final TinkerDataKey<SlotInCharge> SLOT_KEY = TinkerDataCapability.TinkerDataKey.of(EsotericismTinker.getLocation("absorption_gain"));
        private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<Armor>defaultHooks(
                ModifierHooks.MODIFY_HURT
        );

        public static final RecordLoadable<Armor> LOADER = RecordLoadable.create(
                CHANCE_FIELD,
                HOLDER_FIELD,
                TARGET_FIELD,
                RATIO_FIELD,
                MAX_RATIO_FIELD,
                ModifierCondition.TOOL_FIELD,
                Armor::new
        );

        /**
         * @apiNote use {@link AbsorptionGainModule.Builder#buildArmor()}
         */
        @Internal
        public Armor {}

        @Override
        public RecordLoadable<? extends Armor> getLoader() {
            return LOADER;
        }

        @Override
        public List<ModuleHook<?>> getDefaultHooks() {
            return DEFAULT_HOOKS;
        }

        @Override
        public void addModules(slimeknights.tconstruct.library.module.ModuleHookMap.Builder builder) {
            builder.addModule(new SlotInChargeModule(SLOT_KEY));
        }

        @Override
        public float modifyDamageTaken(
                IToolStackView tool,
                ModifierEntry modifier,
                EquipmentContext context,
                EquipmentSlot slotType,
                DamageSource source,
                float amount,
                boolean isDirectDamage
        ) {
            if (amount <= 0 || !condition.matches(tool, modifier)){
                return amount;
            }

            int level = SlotInChargeModule.getLevel(context.getTinkerData(), SLOT_KEY, slotType);
            if (level <= 0 || checkChance(level)){
                return amount;
            }

            LivingEntity wearer = context.getEntity();
            if (!holder.matches(wearer)){
                return amount;
            }

            if (source.getEntity() instanceof LivingEntity attacker && !target.matches(attacker)){
                return amount;
            }

            addAbsorption(wearer, amount, level, modifier);
            return amount;
        }
    }
}