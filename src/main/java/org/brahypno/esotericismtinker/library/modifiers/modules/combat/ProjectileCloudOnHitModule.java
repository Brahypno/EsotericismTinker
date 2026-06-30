package org.brahypno.esotericismtinker.library.modifiers.modules.combat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.json.predicate.TinkerPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Creates an area effect cloud once when a projectile hits an entity or block.
 */
public record ProjectileCloudOnHitModule(IJsonPredicate<LivingEntity> holder, IJsonPredicate<LivingEntity> target, LevelingValue chance,
                                         float radius, int duration, int waitTime, float radiusPerTick, float radiusOnUse,
                                         int color, boolean fixedColor, List<CloudEffect> effects)
        implements ModifierModule, ProjectileHitModifierHook {
    private static final ParticleOptions DEFAULT_PARTICLE = ParticleTypes.DRAGON_BREATH;
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ProjectileCloudOnHitModule>defaultHooks(ModifierHooks.PROJECTILE_HIT);

    public static final RecordLoadable<ProjectileCloudOnHitModule> LOADER = RecordLoadable.create(
            LivingEntityPredicate.LOADER.defaultField("holder", LivingEntityPredicate.ANY, ProjectileCloudOnHitModule::holder),
            LivingEntityPredicate.LOADER.defaultField("target", LivingEntityPredicate.ANY, ProjectileCloudOnHitModule::target),
            LevelingValue.LOADABLE.defaultField("chance", LevelingValue.flat(1), false, ProjectileCloudOnHitModule::chance),
            FloatLoadable.ANY.defaultField("radius", 3.0f, false, ProjectileCloudOnHitModule::radius),
            IntLoadable.ANY_SHORT.defaultField("duration", 200, false, ProjectileCloudOnHitModule::duration),
            IntLoadable.ANY_SHORT.defaultField("wait_time", 0, false, ProjectileCloudOnHitModule::waitTime),
            FloatLoadable.ANY.defaultField("radius_per_tick", -0.005f, false, ProjectileCloudOnHitModule::radiusPerTick),
            FloatLoadable.ANY.defaultField("radius_on_use", -0.5f, false, ProjectileCloudOnHitModule::radiusOnUse),
            IntLoadable.ANY_FULL.defaultField("color", 0x5C7A54, false, ProjectileCloudOnHitModule::color),
            BooleanLoadable.INSTANCE.defaultField("fixed_color", true, false, ProjectileCloudOnHitModule::fixedColor),
            CloudEffect.LOADER.list(0).defaultField("effects", List.of(), ProjectileCloudOnHitModule::effects),
            ProjectileCloudOnHitModule::new);

    /**
     * @apiNote Internal constructor, use {@link #builder()}
     */
    @ApiStatus.Internal
    public ProjectileCloudOnHitModule {}

    /**
     * Creates a builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean onProjectileHitEntity(
            ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile,
            EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target, boolean notBlocked) {
        if (tryMarkRan(persistentData, modifier)
            && checkChance(modifier)
            && TinkerPredicate.matches(this.holder, attacker)
            && TinkerPredicate.matches(this.target, target)){
            createCloud(hit.getEntity().position(), attacker);
        }
        return false;
    }

    @Override
    public boolean onProjectileHitsBlock(
            ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile,
            BlockHitResult hit, @Nullable LivingEntity owner) {
        if (tryMarkRan(persistentData, modifier)
            && checkChance(modifier)
            && TinkerPredicate.matches(this.holder, owner)){
            createCloud(hit.getBlockPos().getCenter(), owner);
        }
        return false;
    }

    private boolean tryMarkRan(ModDataNBT persistentData, ModifierEntry modifier) {
        ResourceLocation key = modifier.getId();
        if (persistentData.getBoolean(key)){
            return false;
        }
        persistentData.putBoolean(key, true);
        return true;
    }

    private boolean checkChance(ModifierEntry modifier) {
        float value = chance.compute(modifier.getEffectiveLevel());
        return value > 0 && (value >= 1 || TConstruct.RANDOM.nextFloat() < value);
    }

    private void createCloud(Vec3 pos, @Nullable LivingEntity owner) {
        if (owner == null || owner.level().isClientSide){
            return;
        }

        Level level = owner.level();
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
        cloud.setOwner(owner);
        cloud.setParticle(DEFAULT_PARTICLE);
        cloud.setPotion(Potions.EMPTY);

        if (fixedColor){
            cloud.setFixedColor(color);
        }

        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setWaitTime(waitTime);
        cloud.setRadiusPerTick(radiusPerTick);
        cloud.setRadiusOnUse(radiusOnUse);

        for (CloudEffect effect : effects) {
            cloud.addEffect(effect.create());
        }

        level.addFreshEntity(cloud);
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<ProjectileCloudOnHitModule> getLoader() {
        return LOADER;
    }

    /**
     * Builder for this module in datagen.
     */
    public static class Builder {
        private IJsonPredicate<LivingEntity> holder = LivingEntityPredicate.ANY;
        private IJsonPredicate<LivingEntity> target = LivingEntityPredicate.ANY;
        private LevelingValue chance = LevelingValue.flat(1);
        private float radius = 3.0f;
        private int duration = 200;
        private int waitTime = 0;
        private float radiusPerTick = -0.005f;
        private float radiusOnUse = -0.5f;
        private int color = 0x5C7A54;
        private boolean fixedColor = true;
        private List<CloudEffect> effects = List.of();

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

        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }

        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public Builder waitTime(int waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public Builder radiusPerTick(float radiusPerTick) {
            this.radiusPerTick = radiusPerTick;
            return this;
        }

        public Builder radiusOnUse(float radiusOnUse) {
            this.radiusOnUse = radiusOnUse;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            this.fixedColor = true;
            return this;
        }

        public Builder fixedColor(boolean fixedColor) {
            this.fixedColor = fixedColor;
            return this;
        }

        public Builder effects(List<CloudEffect> effects) {
            this.effects = effects;
            return this;
        }

        public ProjectileCloudOnHitModule build() {
            return new ProjectileCloudOnHitModule(holder, target, chance, radius, duration, waitTime, radiusPerTick, radiusOnUse, color, fixedColor, effects);
        }
    }

    /**
     * Single effect entry for the generated cloud.
     */
    public record CloudEffect(MobEffect effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        public static final RecordLoadable<CloudEffect> LOADER = RecordLoadable.create(
                Loadables.MOB_EFFECT.requiredField("effect", CloudEffect::effect),
                IntLoadable.ANY_SHORT.defaultField("duration", 60, false, CloudEffect::duration),
                IntLoadable.ANY_SHORT.defaultField("amplifier", 0, false, CloudEffect::amplifier),
                BooleanLoadable.INSTANCE.defaultField("ambient", false, false, CloudEffect::ambient),
                BooleanLoadable.INSTANCE.defaultField("visible", false, false, CloudEffect::visible),
                BooleanLoadable.INSTANCE.defaultField("show_icon", true, false, CloudEffect::showIcon),
                CloudEffect::new);

        public MobEffectInstance create() {
            return new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon);
        }
    }
}