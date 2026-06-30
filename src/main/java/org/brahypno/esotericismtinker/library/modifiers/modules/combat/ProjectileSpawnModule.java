package org.brahypno.esotericismtinker.library.modifiers.modules.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.utils.ETHelper;
import org.jetbrains.annotations.ApiStatus;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;
import java.util.List;


/**
 * Spawns a fixed projectile, or a random registered projectile if projectile is null.
 */
public record ProjectileSpawnModule(@Nullable EntityType<?> projectile, boolean discardOriginal, boolean noGravity, boolean useToolAccuracy,
                                    LevelingValue fallbackSpeed, LevelingValue dangerousHealthRatio,
                                    ModifierCondition<IToolStackView> condition)
        implements ModifierModule, ProjectileLaunchModifierHook, ModifierCondition.ConditionalModule<IToolStackView> {
    private static final int RANDOM_PROJECTILE_TRIES = 64;
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ProjectileSpawnModule>defaultHooks(ModifierHooks.PROJECTILE_LAUNCH);

    public static final RecordLoadable<ProjectileSpawnModule> LOADER = RecordLoadable.create(
            Loadables.ENTITY_TYPE.nullableField("projectile", ProjectileSpawnModule::projectile),
            BooleanLoadable.INSTANCE.defaultField("discard_original", false, false, ProjectileSpawnModule::discardOriginal),
            BooleanLoadable.INSTANCE.defaultField("no_gravity", false, false, ProjectileSpawnModule::noGravity),
            BooleanLoadable.INSTANCE.defaultField("use_tool_accuracy", true, false, ProjectileSpawnModule::useToolAccuracy),
            LevelingValue.LOADABLE.defaultField("fallback_speed", LevelingValue.flat(1.0f), false, ProjectileSpawnModule::fallbackSpeed),
            LevelingValue.LOADABLE.defaultField("dangerous_health_ratio", LevelingValue.flat(-1.0f), false, ProjectileSpawnModule::dangerousHealthRatio),
            ModifierCondition.TOOL_FIELD,
            ProjectileSpawnModule::new);

    /**
     * @apiNote Internal constructor, use {@link #builder()} or {@link #builder(EntityType)}
     */
    @ApiStatus.Internal
    public ProjectileSpawnModule {}

    /**
     * Creates a builder for a random registered projectile.
     */
    public static Builder builder() {
        return new Builder(null);
    }

    /**
     * Creates a builder for a fixed projectile.
     */
    public static Builder builder(EntityType<?> projectile) {
        return new Builder(projectile);
    }

    @Override
    public void onProjectileLaunch(
            IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, Projectile projectile,
            @Nullable AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
        if (shooter.level().isClientSide || !primary || !condition.matches(tool, modifier)){
            return;
        }

        Level level = shooter.level();
        Projectile spawned = createProjectile(level, this.projectile);
        if (spawned == null){
            return;
        }

        Vec3 motion = projectile.getDeltaMovement();
        Vec3 direction = motion.lengthSqr() > ETHelper.MIN_PROJECTILE_SPEED_SQR ? motion.normalize() : shooter.getLookAngle().normalize();
        float speed = motion.lengthSqr() > ETHelper.MIN_PROJECTILE_SPEED_SQR ? (float) motion.length() : fallbackSpeed.compute(modifier.getEffectiveLevel());

        spawned.setOwner(shooter);
        spawned.setNoGravity(noGravity);

        if (spawned instanceof WitherSkull skull){
            float threshold = dangerousHealthRatio.compute(modifier.getEffectiveLevel());
            if (threshold >= 0 && shooter.getHealth() <= shooter.getMaxHealth() * threshold){
                skull.setDangerous(true);
            }
        }

        ETHelper.placeProjectileOutsideShooter(spawned, shooter, direction);

        float inaccuracy = 1.0f;
        if (useToolAccuracy){
            float accuracy = tool.getStats().get(ToolStats.ACCURACY);
            inaccuracy = Math.max(0.0f, 1.0f - accuracy);
        }

        spawned.shoot(direction.x, direction.y, direction.z, speed, inaccuracy);
        level.addFreshEntity(spawned);

        if (discardOriginal){
            projectile.discard();
        }
    }

    @Nullable
    private static Projectile createProjectile(Level level, @Nullable EntityType<?> fixedType) {
        if (fixedType != null){
            Entity entity = fixedType.create(level);
            return entity instanceof Projectile projectile ? projectile : null;
        }

        List<EntityType<?>> types = ForgeRegistries.ENTITY_TYPES.getValues().stream().toList();
        if (types.isEmpty()){
            return null;
        }

        for (int i = 0; i < RANDOM_PROJECTILE_TRIES; i++) {
            EntityType<?> type = types.get(TConstruct.RANDOM.nextInt(types.size()));
            Entity entity = type.create(level);
            if (entity instanceof Projectile projectile){
                return projectile;
            }
            if (entity != null){
                entity.discard();
            }
        }
        return null;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<ProjectileSpawnModule> getLoader() {
        return LOADER;
    }

    /**
     * Builder for this module in datagen.
     */
    public static class Builder extends ModuleBuilder.Stack<Builder> {
        @Nullable
        private final EntityType<?> projectile;
        private boolean discardOriginal = true;
        private boolean noGravity = false;
        private boolean useToolAccuracy = true;
        private LevelingValue fallbackSpeed = LevelingValue.flat(1.0f);
        private LevelingValue dangerousHealthRatio = LevelingValue.flat(-1.0f);

        private Builder(@Nullable EntityType<?> projectile) {
            this.projectile = projectile;
        }

        public Builder discardOriginal(boolean discardOriginal) {
            this.discardOriginal = discardOriginal;
            return this;
        }

        public Builder noGravity(boolean noGravity) {
            this.noGravity = noGravity;
            return this;
        }

        public Builder useToolAccuracy(boolean useToolAccuracy) {
            this.useToolAccuracy = useToolAccuracy;
            return this;
        }

        public Builder fallbackSpeed(LevelingValue fallbackSpeed) {
            this.fallbackSpeed = fallbackSpeed;
            return this;
        }

        /**
         * Set to a non-negative ratio to make WitherSkull dangerous below this holder health ratio.
         */
        public Builder dangerousHealthRatio(LevelingValue dangerousHealthRatio) {
            this.dangerousHealthRatio = dangerousHealthRatio;
            return this;
        }

        /**
         * Builds the finished module.
         */
        public ProjectileSpawnModule build() {
            return new ProjectileSpawnModule(projectile, discardOriginal, noGravity, useToolAccuracy, fallbackSpeed, dangerousHealthRatio, condition);
        }
    }
}