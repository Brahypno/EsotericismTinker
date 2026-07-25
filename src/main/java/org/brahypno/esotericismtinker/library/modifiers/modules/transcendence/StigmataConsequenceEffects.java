package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataConsequence;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Runtime implementations for the 17 Stigmata consequence families.
 *
 * <p>Every handler couples its benefit to a cost or an unstable world event. Flat bonuses are
 * applied through TiC runtime hooks so the consequence remains contextual and never becomes a
 * hidden permanent stat increase.</p>
 */
@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID)
public final class StigmataConsequenceEffects {
    private static final ThreadLocal<Boolean> APPLYING_CONSEQUENCE_DAMAGE =
            ThreadLocal.withInitial(() -> false);

    private static final ResourceLocation SACRAMENT_RESERVE =
            EsotericismTinker.getLocation("stigmata_sacrament_reserve");
    private static final ResourceLocation SACRAMENT_LAST_FOOD =
            EsotericismTinker.getLocation("stigmata_sacrament_last_food");
    private static final UUID PENANCE_ATTACK_SPEED =
            UUID.fromString("6f2903a6-bb64-4fa5-8d4e-d97cd447d544");
    private static final String INCUBATION_MARKER = "esotericism_tinker_stigmata_incubation";
    private static final String INCUBATION_POWER = "esotericism_tinker_stigmata_incubation_power";
    private static final String OBSESSION_OWNER = "esotericism_tinker_stigmata_obsession_owner";

    private static final Map<ServerLevel, List<PendingLightning>> PENDING_LIGHTNING = new HashMap<>();
    private static final Map<ServerLevel, List<PendingSculk>> PENDING_SCULK = new HashMap<>();
    private static final Map<UUID, Long> INCUBATION_SURGE_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> INCUBATION_SURGE_POWER = new HashMap<>();
    private static final Map<UUID, Long> PENANCE_REFRESH = new HashMap<>();

    private static final Handler EXALTATION = new ExaltationHandler();
    private static final Handler MALEDICTION = new MaledictionHandler();
    private static final Handler JUDGEMENT = new JudgementHandler();
    private static final Handler OBSESSION = new ObsessionHandler();
    private static final Handler INCUBATION = new IncubationHandler();
    private static final Handler DEFILEMENT = new DefilementHandler();
    private static final Handler DEVOURING = new DevouringHandler();
    private static final Handler SACRAMENT = new SacramentHandler();
    private static final Handler OFFERING = new OfferingHandler();
    private static final Handler IMMOLATION = new ImmolationHandler();
    private static final Handler PENANCE = new PenanceHandler();
    private static final Handler DOMINION = new ImprintHandler(Imprint.DOMINION);
    private static final Handler NOCTURNE = new ImprintHandler(Imprint.NOCTURNE);
    private static final Handler ZENITH = new ImprintHandler(Imprint.ZENITH);
    private static final Handler ABYSS = new ImprintHandler(Imprint.ABYSS);
    private static final Handler BEATITUDE = new ImprintHandler(Imprint.BEATITUDE);
    private static final Handler ANATHEMA = new ImprintHandler(Imprint.ANATHEMA);

    private StigmataConsequenceEffects() {}

    public record ConsequenceState(IToolStackView tool, StigmataData data, int stage, int overload) {
        public static ConsequenceState of(IToolStackView tool, StigmataData data) {
            return new ConsequenceState(tool, data, data.stage(), data.overload(tool));
        }
    }

    public interface Handler {
        default int onToolDamage(
                ConsequenceState state, ModifierEntry modifier, int amount,
                @Nullable LivingEntity holder) {
            return amount;
        }

        default void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {}

        default float getMeleeDamage(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float baseDamage, float damage) {
            return damage;
        }

        default float beforeMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damage, float baseKnockback, float knockback) {
            return knockback;
        }

        default void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {}

        default float modifyBreakSpeed(
                ConsequenceState state, ModifierEntry modifier,
                slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext context,
                float speed) {
            return speed;
        }

        default void afterBlockBreak(
                ConsequenceState state, ModifierEntry modifier, ToolHarvestContext context) {}

        default void onLauncherHitEntity(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget,
                float damageDealt) {}

        default void onLauncherHitBlock(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity owner, BlockPos target) {}

        default void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {}

        default float getProtectionModifier(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float modifierValue) {
            return modifierValue;
        }
    }

    public static Handler get(StigmataConsequence consequence) {
        return switch (consequence) {
            case EXALTATION -> EXALTATION;
            case MALEDICTION -> MALEDICTION;
            case JUDGEMENT -> JUDGEMENT;
            case OBSESSION -> OBSESSION;
            case INCUBATION -> INCUBATION;
            case DEFILEMENT -> DEFILEMENT;
            case DEVOURING -> DEVOURING;
            case SACRAMENT -> SACRAMENT;
            case OFFERING -> OFFERING;
            case IMMOLATION -> IMMOLATION;
            case PENANCE -> PENANCE;
            case DOMINION -> DOMINION;
            case NOCTURNE -> NOCTURNE;
            case ZENITH -> ZENITH;
            case ABYSS -> ABYSS;
            case BEATITUDE -> BEATITUDE;
            case ANATHEMA -> ANATHEMA;
        };
    }

    public static boolean isApplyingConsequenceDamage() {
        return APPLYING_CONSEQUENCE_DAMAGE.get();
    }

    private static void runConsequenceDamage(Runnable action) {
        if (APPLYING_CONSEQUENCE_DAMAGE.get()){
            return;
        }
        APPLYING_CONSEQUENCE_DAMAGE.set(true);
        try {
            action.run();
        }
        finally {
            APPLYING_CONSEQUENCE_DAMAGE.set(false);
        }
    }

    private static int power(ConsequenceState state) {
        return Math.max(1, Math.min(10, state.overload() + state.stage() - 1));
    }

    private static float enhancement(ConsequenceState state) {
        return 1.0F + 0.07F * power(state) + 0.05F * state.stage();
    }

    private static float penalty(ConsequenceState state) {
        return Math.max(0.35F, 1.0F - 0.06F * power(state) - 0.05F * state.stage());
    }

    private static boolean chance(ServerLevel level, int denominator) {
        return 0 == level.random.nextInt(Math.max(1, denominator));
    }

    private static void cloud(
            ServerLevel level, Vec3 position, @Nullable LivingEntity owner, MobEffect effect,
            int amplifier, int duration, float radius) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, position.x, position.y, position.z);
        cloud.setOwner(owner);
        cloud.setRadius(radius);
        cloud.setWaitTime(5);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(-radius / duration);
        cloud.addEffect(new MobEffectInstance(effect, Math.max(20, duration / 2), amplifier));
        level.addFreshEntity(cloud);
    }

    private static void warningLightning(
            ServerLevel level, Vec3 center, @Nullable LivingEntity cause, ConsequenceState state) {
        double spread = 1.25D + 0.2D * power(state);
        Vec3 strike = center.add(
                (level.random.nextDouble() - 0.5D) * spread * 2.0D,
                0,
                (level.random.nextDouble() - 0.5D) * spread * 2.0D);
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK, strike.x, strike.y + 0.15D, strike.z,
                18 + 2 * state.stage(), spread * 0.3D, 0.15D, spread * 0.3D, 0.04D);
        PENDING_LIGHTNING.computeIfAbsent(level, ignored -> new ArrayList<>())
                         .add(new PendingLightning(strike, cause, level.getGameTime() + 16));
    }

    private static void spreadSculk(ServerLevel level, BlockPos origin, ConsequenceState state) {
        SculkSpreader spreader = SculkSpreader.createLevelSpreader();
        spreader.addCursors(origin, 35 + 15 * power(state));
        PENDING_SCULK.computeIfAbsent(level, ignored -> new ArrayList<>())
                     .add(new PendingSculk(spreader, origin.immutable(), 10 + 2 * state.stage()));
        level.sendParticles(
                ParticleTypes.SCULK_CHARGE_POP,
                origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D,
                12, 0.7D, 0.4D, 0.7D, 0.02D);
    }

    private static void summonVex(
            ServerLevel level, LivingEntity summoner, @Nullable LivingEntity initialTarget,
            ConsequenceState state) {
        String owner = summoner.getUUID().toString();
        AABB search = summoner.getBoundingBox().inflate(48.0D);
        boolean alreadyPresent = !level.getEntitiesOfClass(
                Vex.class, search,
                vex -> owner.equals(vex.getPersistentData().getString(OBSESSION_OWNER))).isEmpty();
        if (alreadyPresent){
            return;
        }
        Vex vex = EntityType.VEX.create(level);
        if (null == vex){
            return;
        }
        vex.moveTo(
                summoner.getX() + level.random.nextDouble() - 0.5D,
                summoner.getEyeY(),
                summoner.getZ() + level.random.nextDouble() - 0.5D,
                summoner.getYRot(), 0);
        vex.getPersistentData().putString(OBSESSION_OWNER, owner);
        vex.setLimitedLife(100 + 20 * power(state));
        if (null != initialTarget){
            vex.setTarget(initialTarget);
        }
        level.addFreshEntity(vex);
    }

    private static void summonSilverfish(
            ServerLevel level, LivingEntity near, ConsequenceState state) {
        Silverfish silverfish = EntityType.SILVERFISH.create(level);
        if (null == silverfish){
            return;
        }
        silverfish.moveTo(
                near.getX() + level.random.nextDouble() - 0.5D,
                near.getY(),
                near.getZ() + level.random.nextDouble() - 0.5D,
                near.getYRot(), 0);
        silverfish.getPersistentData().putBoolean(INCUBATION_MARKER, true);
        silverfish.getPersistentData().putInt(INCUBATION_POWER, power(state));
        level.addFreshEntity(silverfish);
    }

    private static void igniteNearby(ServerLevel level, BlockPos center, ConsequenceState state) {
        int attempts = 2 + state.stage();
        int radius = 2 + state.stage();
        for (int i = 0; i < attempts; i++) {
            BlockPos pos = center.offset(
                    level.random.nextInt(radius * 2 + 1) - radius,
                    level.random.nextInt(3) - 1,
                    level.random.nextInt(radius * 2 + 1) - radius);
            if (level.isEmptyBlock(pos)){
                level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
            }
        }
    }

    @Nullable
    private static LivingEntity attacker(DamageSource source) {
        return source.getEntity() instanceof LivingEntity living ? living : null;
    }

    private record PendingLightning(Vec3 position, @Nullable LivingEntity cause, long due) {}

    private record PendingSculk(SculkSpreader spreader, BlockPos origin, int ticksLeft) {
        PendingSculk tick(ServerLevel level) {
            spreader.updateCursors(level, origin, level.random, true);
            return new PendingSculk(spreader, origin, ticksLeft - 1);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (TickEvent.Phase.END != event.phase || !(event.level instanceof ServerLevel level)){
            return;
        }

        List<PendingLightning> lightning = PENDING_LIGHTNING.get(level);
        if (null != lightning){
            lightning.removeIf(pending -> {
                if (pending.due() > level.getGameTime()){
                    return false;
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (null != bolt){
                    bolt.moveTo(pending.position());
                    if (pending.cause() instanceof ServerPlayer player){
                        bolt.setCause(player);
                    }
                    level.addFreshEntity(bolt);
                }
                return true;
            });
            if (lightning.isEmpty()){
                PENDING_LIGHTNING.remove(level);
            }
        }

        List<PendingSculk> growths = PENDING_SCULK.get(level);
        if (null != growths){
            for (int i = growths.size() - 1; i >= 0; i--) {
                PendingSculk next = growths.get(i).tick(level);
                if (0 >= next.ticksLeft() || next.spreader().getCursors().isEmpty()){
                    growths.remove(i);
                }else {
                    growths.set(i, next);
                }
            }
            if (growths.isEmpty()){
                PENDING_SCULK.remove(level);
            }
        }

        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
            Long refreshed = PENANCE_REFRESH.get(player.getUUID());
            if (null != attackSpeed && (null == refreshed || refreshed < now - 1)){
                attackSpeed.removeModifier(PENANCE_ATTACK_SPEED);
                PENANCE_REFRESH.remove(player.getUUID());
            }
        }
        INCUBATION_SURGE_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        INCUBATION_SURGE_POWER.keySet().removeIf(id -> !INCUBATION_SURGE_UNTIL.containsKey(id));
    }

    @SubscribeEvent
    public static void onIncubationDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Silverfish silverfish)
            || !silverfish.getPersistentData().getBoolean(INCUBATION_MARKER)
            || !(event.getSource().getEntity() instanceof LivingEntity killer)
            || !(killer.level() instanceof ServerLevel level)){
            return;
        }
        int strength = Math.max(1, silverfish.getPersistentData().getInt(INCUBATION_POWER));
        INCUBATION_SURGE_POWER.put(killer.getUUID(), strength);
        INCUBATION_SURGE_UNTIL.put(killer.getUUID(), level.getGameTime() + 80L + 10L * strength);
        level.sendParticles(
                ParticleTypes.ENCHANT, killer.getX(), killer.getEyeY(), killer.getZ(),
                24, 0.5D, 0.6D, 0.5D, 0.15D);
    }

    private static final class ExaltationHandler implements Handler {
        private static void manifest(ConsequenceState state, LivingEntity owner, Vec3 position) {
            if (owner.level() instanceof ServerLevel level){
                cloud(level, position, owner, MobEffects.LEVITATION,
                      Math.min(2, state.stage() - 1),
                      75 + 15 * power(state), 2.0F + 0.25F * state.stage());
            }
        }

        @Override
        public float beforeMeleeHit(ConsequenceState state, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
            return knockback + 0.12F * power(state);
        }

        @Override
        public void afterMeleeHit(ConsequenceState state, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
            LivingEntity target = context.getLivingTarget();
            if (null != target){
                manifest(state, context.getAttacker(), target.position());
            }
        }

        @Override
        public void onLauncherHitEntity(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget,
                float damageDealt) {
            manifest(state, attacker, target.position());
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            manifest(state, context.getEntity(), context.getEntity().position());
        }
    }

    private static final class MaledictionHandler implements Handler {
        private static MobEffect effect(ConsequenceState state) {
            return 3 == state.stage() ? MobEffects.WITHER : MobEffects.POISON;
        }

        private static void manifest(
                ConsequenceState state, LivingEntity owner, Vec3 position) {
            if (owner.level() instanceof ServerLevel level){
                cloud(
                        level, position, owner, effect(state),
                        Math.min(2, power(state) / 4),
                        100 + 15 * power(state), 1.75F + 0.2F * state.stage());
            }
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            LivingEntity target = context.getLivingTarget();
            if (null != target){
                manifest(state, context.getAttacker(), target.position());
            }
        }

        @Override
        public void onLauncherHitEntity(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget,
                float damageDealt) {
            manifest(state, attacker, target.position());
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            manifest(state, context.getEntity(), context.getEntity().position());
        }
    }

    private static final class JudgementHandler implements Handler {
        private static void manifest(
                ConsequenceState state, LivingEntity owner, Vec3 position) {
            if (owner.level() instanceof ServerLevel level
                && chance(level, Math.max(2, 7 - state.stage() - power(state) / 3))){
                warningLightning(level, position, owner, state);
            }
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            LivingEntity target = context.getLivingTarget();
            if (null != target){
                manifest(state, context.getAttacker(), target.position());
            }
        }

        @Override
        public void onLauncherHitEntity(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget,
                float damageDealt) {
            manifest(state, attacker, target.position());
        }

        @Override
        public void onLauncherHitBlock(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity owner, BlockPos target) {
            manifest(state, owner, Vec3.atBottomCenterOf(target));
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            manifest(state, context.getEntity(), context.getEntity().position());
        }
    }

    private static final class ObsessionHandler implements Handler {
        private static void manifest(
                ConsequenceState state, LivingEntity owner, @Nullable LivingEntity target) {
            if (owner.level() instanceof ServerLevel level
                && chance(level, Math.max(3, 10 - power(state) / 2))){
                summonVex(level, owner, target, state);
            }
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            manifest(state, context.getAttacker(), context.getLivingTarget());
        }

        @Override
        public void onLauncherHitEntity(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget,
                float damageDealt) {
            manifest(state, attacker, livingTarget);
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            manifest(state, context.getEntity(), attacker(source));
        }
    }

    private static final class IncubationHandler implements Handler {
        private static void manifest(ConsequenceState state, LivingEntity near) {
            if (near.level() instanceof ServerLevel level
                && chance(level, Math.max(8, 24 - power(state)))){
                summonSilverfish(level, near, state);
            }
        }

        private static float surge(LivingEntity entity, float value) {
            Integer strength = INCUBATION_SURGE_POWER.get(entity.getUUID());
            return null == strength ? value : value * (1.08F + 0.025F * strength);
        }

        @Override
        public float getMeleeDamage(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float baseDamage, float damage) {
            return surge(context.getAttacker(), damage);
        }

        @Override
        public float modifyBreakSpeed(
                ConsequenceState state, ModifierEntry modifier,
                slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext context,
                float speed) {
            return surge(context.player(), speed);
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            LivingEntity target = context.getLivingTarget();
            manifest(state, null == target ? context.getAttacker() : target);
        }

        @Override
        public void onLauncherHitEntity(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget,
                float damageDealt) {
            manifest(state, null == livingTarget ? attacker : livingTarget);
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            manifest(state, context.getEntity());
        }
    }

    private static final class DefilementHandler implements Handler {
        private static void manifest(ConsequenceState state, LivingEntity owner, BlockPos origin) {
            if (owner.level() instanceof ServerLevel level){
                spreadSculk(level, origin, state);
            }
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            LivingEntity target = context.getLivingTarget();
            if (null != target){
                manifest(state, context.getAttacker(), target.blockPosition());
            }
        }

        @Override
        public void afterBlockBreak(
                ConsequenceState state, ModifierEntry modifier, ToolHarvestContext context) {
            manifest(state, context.getLiving(), context.getPos());
        }

        @Override
        public void onLauncherHitEntity(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget,
                float damageDealt) {
            manifest(state, attacker, target.blockPosition());
        }

        @Override
        public void onLauncherHitBlock(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity owner, BlockPos target) {
            manifest(state, owner, target);
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            manifest(state, context.getEntity(), context.getEntity().blockPosition());
        }
    }

    private static final class DevouringHandler implements Handler {
        private static void consume(ConsequenceState state, LivingEntity entity, float base) {
            if (entity instanceof Player player){
                float exhaustion = base * (1.0F + 0.2F * power(state));
                if (0 == player.getFoodData().getFoodLevel()){
                    exhaustion *= 2.0F;
                }
                player.causeFoodExhaustion(exhaustion);
            }
        }

        @Override
        public float getMeleeDamage(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float baseDamage, float damage) {
            return damage * enhancement(state);
        }

        @Override
        public float modifyBreakSpeed(
                ConsequenceState state, ModifierEntry modifier,
                slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext context,
                float speed) {
            return speed * enhancement(state);
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            consume(state, context.getAttacker(), 1.2F);
        }

        @Override
        public void afterBlockBreak(
                ConsequenceState state, ModifierEntry modifier, ToolHarvestContext context) {
            consume(state, context.getLiving(), 0.65F);
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            consume(state, context.getEntity(), 1.5F);
        }

        @Override
        public float getProtectionModifier(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float modifierValue) {
            return modifierValue + 0.75F + 0.3F * power(state);
        }
    }

    private static final class SacramentHandler implements Handler {
        private static void store(ConsequenceState state, LivingEntity entity) {
            if (!(entity instanceof Player player) || 0 >= player.totalExperience){
                return;
            }
            int cost = Math.min(player.totalExperience, 1 + state.stage() / 2);
            player.giveExperiencePoints(-cost);
            int reserve = state.tool().getPersistentData().getInt(SACRAMENT_RESERVE);
            state.tool().getPersistentData().putInt(
                    SACRAMENT_RESERVE, Math.min(40, reserve + cost * (2 + state.stage())));
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            store(state, context.getAttacker());
        }

        @Override
        public void afterBlockBreak(
                ConsequenceState state, ModifierEntry modifier, ToolHarvestContext context) {
            store(state, context.getLiving());
        }

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (!(holder instanceof Player player)){
                return;
            }
            int food = player.getFoodData().getFoodLevel();
            int previous = state.tool().getPersistentData().contains(SACRAMENT_LAST_FOOD)
                           ? state.tool().getPersistentData().getInt(SACRAMENT_LAST_FOOD)
                           : food;
            int reserve = state.tool().getPersistentData().getInt(SACRAMENT_RESERVE);
            if (food < previous && 0 < reserve){
                int restored = Math.min(previous - food, reserve);
                player.getFoodData().setFoodLevel(food + restored);
                reserve -= restored;
                state.tool().getPersistentData().putInt(SACRAMENT_RESERVE, reserve);
            }
            state.tool().getPersistentData().putInt(
                    SACRAMENT_LAST_FOOD, player.getFoodData().getFoodLevel());
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            store(state, context.getEntity());
        }
    }

    private static final class OfferingHandler implements Handler {
        @Override
        public int onToolDamage(
                ConsequenceState state, ModifierEntry modifier, int amount,
                @Nullable LivingEntity holder) {
            if (null == holder || 0 >= amount || holder.getHealth() <= 1.0F){
                return amount;
            }
            float healthCost = Math.max(0.5F, amount * (0.35F + 0.08F * state.stage()));
            if (holder.getHealth() - healthCost < 1.0F){
                return amount;
            }
            runConsequenceDamage(() -> holder.hurt(holder.damageSources().magic(), healthCost));
            if (holder.level() instanceof ServerLevel level){
                level.sendParticles(
                        ParticleTypes.DAMAGE_INDICATOR,
                        holder.getX(), holder.getEyeY(), holder.getZ(),
                        4 + amount, 0.25D, 0.35D, 0.25D, 0.02D);
            }
            return 0;
        }
    }

    private static final class ImmolationHandler implements Handler {
        private static void manifest(
                ConsequenceState state, LivingEntity owner, BlockPos center) {
            if (owner.level() instanceof ServerLevel level
                && chance(level, Math.max(12, 34 - power(state)))){
                igniteNearby(level, center, state);
            }
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            LivingEntity target = context.getLivingTarget();
            manifest(state, context.getAttacker(),
                     null == target ? context.getAttacker().blockPosition() : target.blockPosition());
        }

        @Override
        public void afterBlockBreak(
                ConsequenceState state, ModifierEntry modifier, ToolHarvestContext context) {
            manifest(state, context.getLiving(), context.getPos());
        }

        @Override
        public void onLauncherHitEntity(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget,
                float damageDealt) {
            manifest(state, attacker, target.blockPosition());
        }

        @Override
        public void onLauncherHitBlock(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity owner, BlockPos target) {
            manifest(state, owner, target);
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            manifest(state, context.getEntity(), context.getEntity().blockPosition());
        }
    }

    private static final class PenanceHandler implements Handler {
        @Override
        public float getMeleeDamage(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float baseDamage, float damage) {
            return damage * (enhancement(state) + 0.18F);
        }

        @Override
        public float modifyBreakSpeed(
                ConsequenceState state, ModifierEntry modifier,
                slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext context,
                float speed) {
            return speed * (enhancement(state) + 0.22F);
        }

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (!isSelected || !(holder instanceof ServerPlayer player)){
                return;
            }
            AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
            if (null == attackSpeed){
                return;
            }
            attackSpeed.removeModifier(PENANCE_ATTACK_SPEED);
            double reduction = -Math.min(0.75D, 0.28D + 0.035D * power(state));
            attackSpeed.addTransientModifier(new AttributeModifier(
                    PENANCE_ATTACK_SPEED, "Stigmata penance", reduction,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            PENANCE_REFRESH.put(player.getUUID(), world.getGameTime());
        }

        @Override
        public float getProtectionModifier(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float modifierValue) {
            return modifierValue + 1.0F + 0.35F * power(state);
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            context.getEntity().addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 30 + 10 * power(state),
                    Math.min(2, state.stage() - 1)));
        }
    }

    private enum Imprint {
        DOMINION {
            @Override
            boolean active(LivingEntity entity) {
                return entity.isPassenger();
            }
        },
        NOCTURNE {
            @Override
            boolean active(LivingEntity entity) {
                Level level = entity.level();
                return level.isNight() && 0 == level.getMoonPhase();
            }
        },
        ZENITH {
            @Override
            boolean active(LivingEntity entity) {
                return 128.0D < entity.getY();
            }
        },
        ABYSS {
            @Override
            boolean active(LivingEntity entity) {
                return 0.0D > entity.getY();
            }
        },
        BEATITUDE {
            @Override
            boolean active(LivingEntity entity) {
                return entity.hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
            }
        },
        ANATHEMA {
            @Override
            boolean active(LivingEntity entity) {
                return entity.hasEffect(MobEffects.BAD_OMEN);
            }
        };

        abstract boolean active(LivingEntity entity);
    }

    private record ImprintHandler(Imprint imprint) implements Handler {

        private float multiplier(ConsequenceState state, LivingEntity entity) {
            return imprint.active(entity) ? enhancement(state) : penalty(state);
        }

        @Override
        public float getMeleeDamage(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float baseDamage, float damage) {
            return damage * multiplier(state, context.getAttacker());
        }

        @Override
        public float modifyBreakSpeed(
                ConsequenceState state, ModifierEntry modifier,
                BreakSpeedContext context,
                float speed) {
            return speed * multiplier(state, context.player());
        }

        @Override
        public float getProtectionModifier(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float modifierValue) {
            LivingEntity wearer = context.getEntity();
            float value = 1.0F + 0.4F * power(state);
            return modifierValue + (imprint.active(wearer) ? value : -value);
        }

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (!isSelected || !(world instanceof ServerLevel level)
                || 0 != Math.floorMod(holder.tickCount + state.data().consequenceSeed(), 20)){
                return;
            }
            level.sendParticles(
                    imprint.active(holder) ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE,
                    holder.getX(), holder.getEyeY() - 0.25D, holder.getZ(),
                    3 + state.stage(), 0.25D, 0.3D, 0.25D, 0.01D);
        }
    }
}
