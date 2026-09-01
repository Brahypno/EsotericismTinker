package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataConsequence;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataStage;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Runtime implementations for the 17 Stigmata consequence families.
 *
 * <p>A consequence is not a second modifier bonus. At manifestation it can still be directed,
 * at alienation it stops distinguishing bearer from target, and at sealing it acts without the
 * bearer's command. Overload increases severity, but never restores control.</p>
 */
@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID)
public final class StigmataConsequenceEffects {
    private static final ThreadLocal<Boolean> APPLYING_CONSEQUENCE_DAMAGE =
            ThreadLocal.withInitial(() -> false);

    private static final String INCUBATION_MARKER = "esotericism_tinker_stigmata_incubation";
    private static final String INCUBATION_POWER = "esotericism_tinker_stigmata_incubation_power";
    private static final String INCUBATION_STAGE = "esotericism_tinker_stigmata_incubation_stage";
    private static final String INCUBATION_OWNER = "esotericism_tinker_stigmata_incubation_owner";
    private static final String OBSESSION_OWNER = "esotericism_tinker_stigmata_obsession_owner";

    private static final Map<ServerLevel, List<PendingLightning>> PENDING_LIGHTNING = new HashMap<>();
    private static final Map<ServerLevel, List<PendingSculk>> PENDING_SCULK = new HashMap<>();
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

    public record ConsequenceState(
            StigmataConsequence consequence, int consequenceSeed,
            int stage, int overload) {
        public static ConsequenceState of(IToolStackView tool, StigmataData data) {
            return new ConsequenceState(
                    data.consequence(), data.consequenceSeed(),
                    data.stage(), data.overload(tool));
        }
    }

    public interface Handler {
        default void addTooltip(
                ConsequenceState state, boolean armor, List<Component> tooltip) {
            StigmataStage stage = StigmataStage.byIndex(state.stage());
            if (null == stage){
                return;
            }
            Component problem = armor
                                ? state.consequence().armorStageName(stage)
                                : state.consequence().stageName(stage);
            tooltip.add(problem.copy().withStyle(ChatFormatting.DARK_RED));
        }

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

        default void onProjectileLaunch(
                ConsequenceState state, ModifierEntry modifier, LivingEntity shooter,
                Projectile projectile, ModDataNBT persistentData, boolean primary) {}

        default float modifyProjectileHurt(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                DamageSource source, @Nullable LivingEntity attacker,
                LivingEntity target, float amount) {
            return amount;
        }

        default void onLeftClickEmpty(
                ConsequenceState state, ModifierEntry modifier, Player player,
                Level level, EquipmentSlot slot) {}

        default void onLeftClickBlock(
                ConsequenceState state, ModifierEntry modifier,
                PlayerInteractEvent.LeftClickBlock event, Player player,
                Level level, EquipmentSlot slot, BlockState blockState,
                BlockPos pos) {}

        default void onLeftClickEntity(
                ConsequenceState state, ModifierEntry modifier,
                AttackEntityEvent event, Player player, Level level,
                EquipmentSlot slot, Entity target) {}

        default void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {}

        default float getProtectionModifier(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float modifierValue) {
            return modifierValue;
        }

        default float modifyHurt(
                ConsequenceState state, ModifierEntry modifier,
                EquipmentContext context, EquipmentSlot slot,
                DamageSource source, float amount, boolean isDirectDamage) {
            return amount;
        }

        default void addAttributes(
                ConsequenceState state, ModifierEntry modifier, EquipmentSlot slot,
                BiConsumer<Attribute, AttributeModifier> consumer) {}
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

    private static boolean dealConsequenceDamage(LivingEntity entity, float amount) {
        if (APPLYING_CONSEQUENCE_DAMAGE.get()){
            return false;
        }
        APPLYING_CONSEQUENCE_DAMAGE.set(true);
        try {
            return entity.hurt(entity.damageSources().magic(), amount);
        }
        finally {
            APPLYING_CONSEQUENCE_DAMAGE.set(false);
        }
    }

    private static int power(ConsequenceState state) {
        return Math.max(1, Math.min(10, state.overload() + state.stage() - 1));
    }

    private static float grace(ConsequenceState state) {
        float rate = switch (state.stage()) {
            case 1 -> 0.03F;
            case 2 -> 0.02F;
            default -> 0.01F;
        };
        return 1.0F + rate * power(state);
    }

    private static float penalty(ConsequenceState state) {
        return Math.max(0.25F, 1.0F - 0.05F * power(state) - 0.12F * state.stage());
    }

    private static boolean chance(ServerLevel level, int denominator) {
        return 0 == level.random.nextInt(Math.max(1, denominator));
    }

    private static boolean periodic(
            ConsequenceState state, LivingEntity holder, int interval) {
        return 0 == Math.floorMod(
                holder.tickCount + state.consequenceSeed(), Math.max(1, interval));
    }

    private static boolean activeSlot(boolean isSelected, boolean isCorrectSlot) {
        return isSelected || isCorrectSlot;
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
        LivingEntity target = initialTarget;
        if (2 == state.stage() && level.random.nextInt(3) == 0){
            target = summoner;
        }else if (3 <= state.stage()){
            target = summoner;
        }
        if (null != target && target.isAlive()){
            vex.setTarget(target);
        }
        level.addFreshEntity(vex);
    }

    private static void summonSilverfish(
            ServerLevel level, LivingEntity owner, LivingEntity near,
            @Nullable LivingEntity initialTarget, ConsequenceState state) {
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
        silverfish.getPersistentData().putInt(INCUBATION_STAGE, state.stage());
        silverfish.getPersistentData().putString(INCUBATION_OWNER, owner.getUUID().toString());
        LivingEntity target = initialTarget;
        if (2 == state.stage() && level.random.nextInt(3) == 0){
            target = owner;
        }else if (3 <= state.stage()){
            target = owner;
        }
        if (null != target && target.isAlive()){
            silverfish.setTarget(target);
        }
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
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level){
            PENDING_LIGHTNING.remove(level);
            PENDING_SCULK.remove(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING_LIGHTNING.clear();
        PENDING_SCULK.clear();
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

    }

    @SubscribeEvent
    public static void onIncubationDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Silverfish silverfish)
            || !silverfish.getPersistentData().getBoolean(INCUBATION_MARKER)
            || !(silverfish.level() instanceof ServerLevel level)){
            return;
        }
        int strength = Math.max(1, silverfish.getPersistentData().getInt(INCUBATION_POWER));
        int stage = Math.max(1, silverfish.getPersistentData().getInt(INCUBATION_STAGE));
        MobEffect effect = 3 <= stage ? MobEffects.POISON : MobEffects.HUNGER;
        cloud(
                level, silverfish.position(), null, effect,
                Math.min(2, strength / 4), 80 + 10 * strength, 1.5F + 0.35F * stage);
        level.sendParticles(
                ParticleTypes.SOUL, silverfish.getX(), silverfish.getEyeY(), silverfish.getZ(),
                18, 0.45D, 0.35D, 0.45D, 0.04D);
    }

    private static final class ExaltationHandler implements Handler {
        private static void manifest(ConsequenceState state, LivingEntity owner, Vec3 position) {
            if (owner.level() instanceof ServerLevel level){
                cloud(level, position, owner, MobEffects.LEVITATION,
                      Math.min(2, state.stage() - 1),
                      75 + 15 * power(state), 2.0F + 0.25F * state.stage());
                if (2 <= state.stage()){
                    owner.addEffect(new MobEffectInstance(
                            MobEffects.LEVITATION, 15 + 5 * power(state),
                            Math.min(2, state.stage() - 2)));
                }
            }
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

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (3 <= state.stage() && world instanceof ServerLevel
                && periodic(state, holder, Math.max(60, 120 - 5 * power(state)))){
                holder.addEffect(new MobEffectInstance(
                        MobEffects.LEVITATION, 35 + 3 * power(state),
                        Math.min(2, power(state) / 4)));
            }
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
                if (2 <= state.stage()){
                    owner.addEffect(new MobEffectInstance(
                            effect(state), 45 + 5 * power(state),
                            Math.min(1, power(state) / 5)));
                }
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

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (3 <= state.stage() && world instanceof ServerLevel
                && periodic(state, holder, Math.max(80, 150 - 5 * power(state)))){
                manifest(state, holder, holder.position());
            }
        }
    }

    private static final class JudgementHandler implements Handler {
        private static void manifest(
                ConsequenceState state, LivingEntity owner, Vec3 position) {
            if (owner.level() instanceof ServerLevel level
                && chance(level, Math.max(2, 7 - state.stage() - power(state) / 3))){
                Vec3 center = position;
                if (2 == state.stage() && level.random.nextBoolean()){
                    center = owner.position();
                }else if (3 <= state.stage()){
                    center = owner.position();
                }
                warningLightning(level, center, 1 == state.stage() ? owner : null, state);
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

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (3 <= state.stage() && world instanceof ServerLevel level
                && periodic(state, holder, Math.max(100, 190 - 5 * power(state)))){
                warningLightning(level, holder.position(), null, state);
            }
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

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (3 <= state.stage() && world instanceof ServerLevel level
                && periodic(state, holder, Math.max(120, 220 - 5 * power(state)))){
                summonVex(level, holder, holder, state);
            }
        }
    }

    private static final class IncubationHandler implements Handler {
        private static void manifest(
                ConsequenceState state, LivingEntity owner, LivingEntity near,
                @Nullable LivingEntity target) {
            if (owner.level() instanceof ServerLevel level
                && chance(level, Math.max(8, 24 - power(state)))){
                summonSilverfish(level, owner, near, target, state);
            }
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            LivingEntity owner = context.getAttacker();
            LivingEntity target = context.getLivingTarget();
            LivingEntity near = 2 <= state.stage() || null == target ? owner : target;
            manifest(state, owner, near, target);
        }

        @Override
        public void onLauncherHitEntity(
                ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget,
                float damageDealt) {
            LivingEntity near = 2 <= state.stage() || null == livingTarget ? attacker : livingTarget;
            manifest(state, attacker, near, livingTarget);
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            LivingEntity wearer = context.getEntity();
            manifest(state, wearer, wearer, attacker(source));
        }

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (3 <= state.stage() && world instanceof ServerLevel level
                && periodic(state, holder, Math.max(120, 210 - 5 * power(state)))){
                summonSilverfish(level, holder, holder, holder, state);
            }
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

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (3 <= state.stage() && world instanceof ServerLevel level
                && periodic(state, holder, Math.max(140, 240 - 5 * power(state)))){
                spreadSculk(level, holder.blockPosition(), state);
            }
        }
    }

    private static final class DevouringHandler implements Handler {
        private static void consume(ConsequenceState state, LivingEntity entity, float base) {
            if (entity instanceof Player player){
                float exhaustion =
                        base * (1.0F + 0.25F * power(state) + 0.35F * state.stage());
                player.causeFoodExhaustion(exhaustion);
                if (2 <= state.stage()){
                    player.addEffect(new MobEffectInstance(
                            MobEffects.HUNGER, 40 + 5 * power(state),
                            Math.min(2, state.stage() - 1)));
                }
                if (3 <= state.stage() && player.getFoodData().getFoodLevel() <= 6){
                    dealConsequenceDamage(player, 0.5F + 0.15F * power(state));
                }
            }
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
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (3 <= state.stage() && world instanceof ServerLevel
                && periodic(state, holder, Math.max(60, 110 - 4 * power(state)))){
                consume(state, holder, 1.0F);
            }
        }
    }

    private static final class SacramentHandler implements Handler {
        private static void consumeExperience(ConsequenceState state, LivingEntity entity) {
            if (!(entity instanceof Player player) || 0 >= player.totalExperience){
                return;
            }
            int demanded = state.stage() + Math.max(0, power(state) - 1) / 4;
            int cost = Math.min(player.totalExperience, demanded);
            player.giveExperiencePoints(-cost);
        }

        @Override
        public void afterMeleeHit(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float damageDealt) {
            consumeExperience(state, context.getAttacker());
        }

        @Override
        public void afterBlockBreak(
                ConsequenceState state, ModifierEntry modifier, ToolHarvestContext context) {
            consumeExperience(state, context.getLiving());
        }

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (3 <= state.stage() && world instanceof ServerLevel
                && periodic(state, holder, Math.max(60, 120 - 5 * power(state)))){
                consumeExperience(state, holder);
            }
        }

        @Override
        public void onArmorAttacked(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float amount, boolean isDirectDamage) {
            consumeExperience(state, context.getEntity());
        }
    }

    private static final class OfferingHandler implements Handler {
        @Override
        public int onToolDamage(
                ConsequenceState state, ModifierEntry modifier, int amount,
                @Nullable LivingEntity holder) {
            if (null == holder || 0 >= amount){
                return amount;
            }
            float healthCost = Math.max(
                    0.5F,
                    amount * (0.35F + 0.2F * state.stage() + 0.04F * power(state)));
            boolean paid = dealConsequenceDamage(holder, healthCost);
            if (holder.level() instanceof ServerLevel level){
                level.sendParticles(
                        ParticleTypes.DAMAGE_INDICATOR,
                        holder.getX(), holder.getEyeY(), holder.getZ(),
                        4 + amount, 0.25D, 0.35D, 0.25D, 0.02D);
            }
            return paid ? 0 : amount;
        }
    }

    private static final class ImmolationHandler implements Handler {
        private static void manifest(
                ConsequenceState state, LivingEntity owner, BlockPos center) {
            if (owner.level() instanceof ServerLevel level
                && chance(level, Math.max(12, 34 - power(state)))){
                BlockPos origin = center;
                if (2 == state.stage() && level.random.nextBoolean()){
                    origin = owner.blockPosition();
                }else if (3 <= state.stage()){
                    origin = owner.blockPosition();
                    owner.setSecondsOnFire(2 + power(state) / 3);
                }
                igniteNearby(level, origin, state);
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

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (3 <= state.stage() && world instanceof ServerLevel level
                && periodic(state, holder, Math.max(100, 190 - 5 * power(state)))){
                igniteNearby(level, holder.blockPosition(), state);
                holder.setSecondsOnFire(2 + power(state) / 3);
            }
        }
    }

    private static final class PenanceHandler implements Handler {
        private static UUID attributeUuid(String attribute, EquipmentSlot slot) {
            String key = EsotericismTinker.MODID + ":stigmata_penance/"
                         + attribute + "/" + slot.getName();
            return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void addAttributes(
                ConsequenceState state, ModifierEntry modifier, EquipmentSlot slot,
                BiConsumer<Attribute, AttributeModifier> consumer) {
            int power = power(state);
            double attackSpeedReduction =
                    -Math.min(0.75D, 0.28D + 0.035D * power);
            int slownessAmplifier =
                    Math.min(3, state.stage() - 1 + power / 5);
            double movementSpeedReduction =
                    -0.15D * (slownessAmplifier + 1);

            consumer.accept(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                            attributeUuid("attack_speed", slot),
                            "Stigmata penance attack speed",
                            attackSpeedReduction,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
            consumer.accept(
                    Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            attributeUuid("movement_speed", slot),
                            "Stigmata penance movement speed",
                            movementSpeedReduction,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        @Override
        public float getMeleeDamage(
                ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                float baseDamage, float damage) {
            return damage * (1.0F + 0.02F * power(state));
        }

        @Override
        public float modifyBreakSpeed(
                ConsequenceState state, ModifierEntry modifier,
                slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext context,
                float speed) {
            float burden =
                    Math.max(0.4F, 0.92F - 0.07F * state.stage() - 0.025F * power(state));
            return speed * burden;
        }

        @Override
        public float getProtectionModifier(
                ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                EquipmentSlot slot, DamageSource source, float modifierValue) {
            return modifierValue - 0.25F * state.stage() - 0.05F * power(state);
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
                return entity.getY() <= 0.0D;
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
            return imprint.active(entity) ? grace(state) : penalty(state);
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
            float grace = 0.25F + 0.08F * power(state);
            float rejection = 0.5F + 0.2F * power(state) + 0.35F * state.stage();
            return modifierValue + (imprint.active(wearer) ? grace : -rejection);
        }

        @Override
        public void onInventoryTick(
                ConsequenceState state, ModifierEntry modifier, Level world, LivingEntity holder,
                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
            if (!activeSlot(isSelected, isCorrectSlot) || !(world instanceof ServerLevel level)
                || 0 != Math.floorMod(holder.tickCount + state.consequenceSeed(), 20)){
                return;
            }
            boolean active = imprint.active(holder);
            level.sendParticles(
                    active ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE,
                    holder.getX(), holder.getEyeY() - 0.25D, holder.getZ(),
                    3 + state.stage(), 0.25D, 0.3D, 0.25D, 0.01D);
            if (!active && 2 <= state.stage()){
                holder.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 30,
                        Math.min(2, state.stage() - 2 + power(state) / 5),
                        false, false, true));
            }
        }
    }
}
