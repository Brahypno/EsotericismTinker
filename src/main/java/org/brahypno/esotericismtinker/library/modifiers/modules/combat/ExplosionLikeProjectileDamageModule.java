package org.brahypno.esotericismtinker.library.modifiers.modules.combat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.CombatHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileFuseModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.utils.CustomExplosion;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 发射物命中后爆炸。
 * <p>
 * damage 是传给 CustomExplosion 的最大伤害参数。
 * 注意 CustomExplosion 中心满强度时实际伤害约为 damage + 1。
 *
 * @param damage           爆炸伤害参数
 * @param radius           爆炸半径
 * @param damageType       爆炸使用的 DamageType
 * @param placeFire        是否点火
 * @param blockInteraction 对方块的爆炸行为
 */
public record ExplosionLikeProjectileDamageModule(
        LevelingValue damage,
        LevelingValue radius,
        ResourceKey<DamageType> damageType,
        boolean placeFire,
        Explosion.BlockInteraction blockInteraction
) implements ModifierModule, ProjectileHitModifierHook, ProjectileFuseModifierHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ExplosionLikeProjectileDamageModule>defaultHooks(
            ModifierHooks.PROJECTILE_HIT,
            ModifierHooks.PROJECTILE_HIT_CLIENT,
            ModifierHooks.PROJECTILE_FUSE
    );

    public static final RecordLoadable<ExplosionLikeProjectileDamageModule> LOADER = RecordLoadable.create(
            LevelingValue.LOADABLE.requiredField("damage", ExplosionLikeProjectileDamageModule::damage),
            LevelingValue.LOADABLE.requiredField("radius", ExplosionLikeProjectileDamageModule::radius),
            Loadables.DAMAGE_TYPE_KEY.requiredField("damage_type", ExplosionLikeProjectileDamageModule::damageType),
            BooleanLoadable.INSTANCE.defaultField("place_fire", false, ExplosionLikeProjectileDamageModule::placeFire),
            new EnumLoadable<>(Explosion.BlockInteraction.class).requiredField("block_interaction", ExplosionLikeProjectileDamageModule::blockInteraction),
            ExplosionLikeProjectileDamageModule::new
    );

    public static Builder builder(LevelingValue damage, LevelingValue radius, ResourceKey<DamageType> damageType) {
        return new Builder(damage, radius, damageType);
    }

    public static Builder builder(
            float damageFlat,
            float damageEachLevel,
            float radiusFlat,
            float radiusEachLevel,
            ResourceKey<DamageType> damageType
    ) {
        return builder(new LevelingValue(damageFlat, damageEachLevel), new LevelingValue(radiusFlat, radiusEachLevel), damageType);
    }

    @Override
    public Integer getPriority() {
        return 25;
    }

    @Override
    public RecordLoadable<ExplosionLikeProjectileDamageModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    private boolean explode(ModifierEntry modifier, Projectile projectile, Vec3 location) {
        float level = modifier.getEffectiveLevel();
        float radius = this.radius.computeForLevel(level);

        if (radius <= 0.5F || projectile.getType().is(TinkerTags.EntityTypes.REUSABLE_AMMO)){
            return false;
        }

        Level world = projectile.level();
        if (!world.isClientSide){
            float damage = this.damage.computeForLevel(level);
            Entity owner = projectile.getOwner();
            DamageSource source = CombatHelper.damageSource(damageType, projectile, owner);

            ModifierUtil.updateFishingRod(projectile, 2 + 3 * modifier.getLevel(), true);

            projectile.discard();

            new CustomExplosion(
                    world,
                    location,
                    radius,
                    projectile,
                    null,
                    damage,
                    source,
                    1.0F,
                    null,
                    placeFire,
                    blockInteraction
            ).handleServer();
        }

        return true;
    }

    @Override
    public boolean onProjectileHitsBlock(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, BlockHitResult hit, @Nullable LivingEntity owner) {
        return explode(modifier, projectile, hit.getLocation());
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target, boolean notBlocked) {
        return explode(modifier, projectile, hit.getLocation());
    }

    @Override
    public void onProjectileFuseFinish(
            ModifierNBT modifiers,
            ModDataNBT persistentData,
            ModifierEntry modifier,
            ItemStack ammo,
            Projectile projectile,
            @Nullable AbstractArrow arrow
    ) {
        explode(modifier, projectile, projectile.position());
    }

    public static class Builder {
        private final LevelingValue damage;
        private final LevelingValue radius;
        private final ResourceKey<DamageType> damageType;
        private boolean placeFire = false;
        private Explosion.BlockInteraction blockInteraction = Explosion.BlockInteraction.DESTROY_WITH_DECAY;

        private Builder(LevelingValue damage, LevelingValue radius, ResourceKey<DamageType> damageType) {
            this.damage = damage;
            this.radius = radius;
            this.damageType = damageType;
        }

        public Builder placeFire() {
            this.placeFire = true;
            return this;
        }

        public Builder placeFire(boolean placeFire) {
            this.placeFire = placeFire;
            return this;
        }

        public Builder blockInteraction(Explosion.BlockInteraction blockInteraction) {
            this.blockInteraction = blockInteraction;
            return this;
        }

        public Builder ignoreBlocks() {
            return blockInteraction(Explosion.BlockInteraction.KEEP);
        }

        public Builder noBlockDecay() {
            return blockInteraction(Explosion.BlockInteraction.DESTROY);
        }

        public ExplosionLikeProjectileDamageModule build() {
            return new ExplosionLikeProjectileDamageModule(
                    damage,
                    radius,
                    damageType,
                    placeFire,
                    blockInteraction
            );
        }
    }
}