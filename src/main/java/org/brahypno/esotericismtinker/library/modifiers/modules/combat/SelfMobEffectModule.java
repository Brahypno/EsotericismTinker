package org.brahypno.esotericismtinker.library.modifiers.modules.combat;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.brahypno.esotericismtinker.utils.ETHelper;
import org.jetbrains.annotations.ApiStatus;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.json.RandomLevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MonsterMeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.tools.modules.armor.CounterModule;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static slimeknights.tconstruct.TConstruct.RANDOM;

public record SelfMobEffectModule(IJsonPredicate<LivingEntity> target, @Nullable MobEffect effect, RandomLevelingValue level, RandomLevelingValue time,
                                  LevelingValue chance, boolean applyBeforeMelee,
                                  ModifierCondition<IToolStackView> condition,
                                  @Nullable TagKey<MobEffect> effectTag, boolean transferNegativeEffects,
                                  boolean removeSelfEffects) implements OnAttackedModifierHook, MeleeHitModifierHook, MonsterMeleeHitModifierHook, ProjectileHitModifierHook, ModifierModule, ModifierCondition.ConditionalModule<IToolStackView> {
    public static final RecordLoadable<SelfMobEffectModule> LOADER = RecordLoadable.create(
            LivingEntityPredicate.LOADER.defaultField("target", SelfMobEffectModule::target),
            Loadables.MOB_EFFECT.nullableField("effect", SelfMobEffectModule::effect),
            RandomLevelingValue.LOADABLE.defaultField("level", RandomLevelingValue.flat(1), SelfMobEffectModule::level),
            RandomLevelingValue.LOADABLE.defaultField("time", RandomLevelingValue.flat(0), SelfMobEffectModule::time),
            LevelingValue.LOADABLE.defaultField("chance", LevelingValue.eachLevel(0.25f), false, SelfMobEffectModule::chance),
            BooleanLoadable.INSTANCE.defaultField("apply_before_melee", false, false, SelfMobEffectModule::applyBeforeMelee),
            ModifierCondition.TOOL_FIELD,
            Loadables.MOB_EFFECT_TAG.nullableField("effect_tag", SelfMobEffectModule::effectTag),
            BooleanLoadable.INSTANCE.defaultField("transfer_negative_effects", false, false, SelfMobEffectModule::transferNegativeEffects),
            BooleanLoadable.INSTANCE.defaultField("remove_self_effects", true, false, SelfMobEffectModule::removeSelfEffects),
            SelfMobEffectModule::new);
    private static final List<ModuleHook<?>>
            DEFAULT_HOOKS = HookProvider.<SelfMobEffectModule>defaultHooks(ModifierHooks.ON_ATTACKED, ModifierHooks.MELEE_HIT, ModifierHooks.MONSTER_MELEE_HIT,
                                                                           ModifierHooks.PROJECTILE_HIT);

    /**
     * @apiNote Internal constructor, use {@link #builder(MobEffect)}
     */
    @ApiStatus.Internal
    public SelfMobEffectModule {}

    /** Preserves the constructor for existing self-effect modules. */
    public SelfMobEffectModule(IJsonPredicate<LivingEntity> target, @Nullable MobEffect effect, RandomLevelingValue level,
                               RandomLevelingValue time, LevelingValue chance, boolean applyBeforeMelee,
                               ModifierCondition<IToolStackView> condition, @Nullable TagKey<MobEffect> effectTag) {
        this(target, effect, level, time, chance, applyBeforeMelee, condition, effectTag, false, true);
    }

    /** Creates a builder for transferring existing harmful effects. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder instance
     */
    public static Builder builder(MobEffect effect) {
        return Builder.effect(effect);
    }

    public static Builder builder(TagKey<MobEffect> effectTag) {
        return Builder.effectTag(effectTag);
    }

    /**
     * Creates a builder instance
     */
    public static Builder builder(Supplier<? extends MobEffect> effect) {
        return Builder.effect(effect.get());
    }

    private static void addRandomEffectFromTagIfMissing(LivingEntity target, TagKey<MobEffect> tag, int duration, int amplifier) {
        Registry<MobEffect> registry = BuiltInRegistries.MOB_EFFECT;

        // 1) 玩家当前是否已经有任意一个属于该 tag 的效果
        boolean hasTaggedEffect = target.getActiveEffects().stream()
                                        .map(MobEffectInstance::getEffect)
                                        .anyMatch(effect -> registry.wrapAsHolder(effect).is(tag));

        if (hasTaggedEffect){
            return;
        }

        // 2) 取出 tag 里的所有效果
        Optional<HolderSet.Named<MobEffect>> opt = registry.getTag(tag);
        if (opt.isEmpty()){
            return;
        }

        HolderSet.Named<MobEffect> candidates = opt.get();
        if (candidates.size() == 0){
            return;
        }

        // 3) 随机抽一个
        Holder<MobEffect> chosen = candidates.get(target.getRandom().nextInt(candidates.size()));

        // 4) 给玩家添加
        MobEffectInstance inst = new MobEffectInstance(chosen.get(), duration, amplifier);
        target.addEffect(inst);
    }

    /**
     * Applies the effect for the given level
     */
    private void applyEffect(@Nullable LivingEntity target, float scaledLevel) {
        if (target == null || !this.target.matches(target)){
            return;
        }
        int level = Math.round(this.level.computeValue(scaledLevel)) - 1;
        if (level < 0){
            return;
        }
        float duration = this.time.computeValue(scaledLevel);
        if (duration > 0){
            if (null != this.effect)
                target.addEffect(new MobEffectInstance(effect, (int) duration, level));
            else if (null != this.effectTag){
                addRandomEffectFromTagIfMissing(target, effectTag, (int) duration, level);
            }
        }
    }

    /** In transfer mode the target predicate applies to the recipient, and level/time are kept from the owner. */
    private boolean transferEffects(@Nullable LivingEntity owner, @Nullable LivingEntity recipient) {
        if (owner == null || recipient == null || owner == recipient || owner.level().isClientSide
            || !recipient.isAlive() || !target.matches(recipient)){
            return false;
        }
        boolean changed = false;
        // Removing from the owner's live collection while iterating would invalidate the iterator.
        for (MobEffectInstance instance : new ArrayList<>(owner.getActiveEffects())) {
            if (instance.getEffect().getCategory() == MobEffectCategory.HARMFUL){
                // NBT also copies hidden effect layers, which the copy constructor omits.
                MobEffectInstance copy = MobEffectInstance.load(instance.save(new CompoundTag()));
                boolean received = copy != null && recipient.addEffect(copy, owner);
                if (received && removeSelfEffects){
                    changed |= owner.removeEffect(instance.getEffect());
                }
                changed |= received;
            }
        }
        return changed;
    }

    private void applyOrTransfer(@Nullable LivingEntity owner, @Nullable LivingEntity recipient, float scaledLevel) {
        if (transferNegativeEffects){
            float chance = this.chance.compute(scaledLevel);
            if (chance >= 1 || RANDOM.nextFloat() < chance){
                transferEffects(owner, recipient);
            }
        } else {
            applyEffect(owner, scaledLevel);
        }
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        if (isDirectDamage && tool.hasTag(TinkerTags.Items.ARMOR) && condition.matches(tool, modifier)){
            LivingEntity defender = context.getEntity();
            float scaledLevel = CounterModule.getLevel(tool, modifier, slotType, defender);
            float chance = this.chance.compute(scaledLevel);
            if (chance >= 1 || RANDOM.nextFloat() < chance){
                if (transferNegativeEffects){
                    if (!transferEffects(defender, ETHelper.getLivingTarget(source.getEntity()))){
                        return;
                    }
                } else {
                    applyEffect(defender, scaledLevel);
                }
                ToolDamageUtil.damageAnimated(tool, 1, defender, slotType);
            }
        }
    }

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        if (applyBeforeMelee && condition.matches(tool, modifier)){
            applyOrTransfer(context.getAttacker(), ETHelper.getLivingTarget(context.getTarget()), modifier.getEffectiveLevel());
        }
        return knockback;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!applyBeforeMelee && condition.matches(tool, modifier)){
            applyOrTransfer(context.getAttacker(), ETHelper.getLivingTarget(context.getTarget()), modifier.getEffectiveLevel());
        }
    }

    @Override
    public void onMonsterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage) {
        if (condition.matches(tool, modifier)){
            applyOrTransfer(context.getAttacker(), ETHelper.getLivingTarget(context.getTarget()), modifier.getEffectiveLevel());
        }
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        return onProjectileHitEntity(modifiers, persistentData, modifier, projectile, hit, attacker, target, true);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile,
                                         EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target, boolean notBlocked) {
        if ((!transferNegativeEffects || notBlocked) && condition.modifierLevel().test(modifier.getLevel())){
            applyOrTransfer(attacker, target, modifier.getEffectiveLevel());
        }
        return false;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<SelfMobEffectModule> getLoader() {
        return LOADER;
    }

    /**
     * Builder for this modifier in datagen
     */
    public static class Builder extends ModuleBuilder.Stack<Builder> {
        private MobEffect effect;
        private TagKey<MobEffect> effectTag;
        private IJsonPredicate<LivingEntity> target = LivingEntityPredicate.ANY;
        private RandomLevelingValue level = RandomLevelingValue.flat(1);
        private RandomLevelingValue time = RandomLevelingValue.flat(0);
        private LevelingValue chance = LevelingValue.eachLevel(0.25f);
        private boolean applyBeforeMelee = false;
        private boolean transferNegativeEffects = false;
        private boolean removeSelfEffects = true;

        /** Transfers all HARMFUL effects instead of adding a configured effect to the owner. */
        public Builder transferNegativeEffects() {
            this.transferNegativeEffects = true;
            return this;
        }

        /** Whether successful transfers remove the owner's effect; false copies it instead. */
        public Builder removeSelfEffects(boolean remove) {
            this.removeSelfEffects = remove;
            return this;
        }

        public Builder applyBeforeMelee(boolean before) {
            this.applyBeforeMelee = before;
            return this;
        }

        public static Builder effect(MobEffect effect) {
            Builder builder = new Builder();
            builder.effect = Objects.requireNonNull(effect, "effect");
            return builder;
        }

        public static Builder effectTag(TagKey<MobEffect> effectTag) {
            Builder builder = new Builder();
            builder.effectTag = Objects.requireNonNull(effectTag, "effectTag");
            return builder;
        }

        public Builder target(IJsonPredicate<LivingEntity> target) {
            this.target = target;
            return this;
        }

        public Builder level(RandomLevelingValue level) {
            this.level = level;
            return this;
        }

        public Builder chance(LevelingValue chance) {
            this.chance = chance;
            return this;
        }

        public Builder time(RandomLevelingValue time) {
            this.time = time;
            return this;
        }

        /**
         * Builds the finished modifier
         */
        public SelfMobEffectModule build() {
            boolean hasEffect = effect != null;
            boolean hasEffectTag = effectTag != null;

            if ((hasEffect ? 1 : 0) + (hasEffectTag ? 1 : 0) + (transferNegativeEffects ? 1 : 0) != 1){
                throw new IllegalStateException("Exactly one of effect, effectTag or transferNegativeEffects must be set");
            }
            return new SelfMobEffectModule(target, effect, level, time, chance, applyBeforeMelee, condition, effectTag,
                                           transferNegativeEffects, removeSelfEffects);
        }
    }
}
