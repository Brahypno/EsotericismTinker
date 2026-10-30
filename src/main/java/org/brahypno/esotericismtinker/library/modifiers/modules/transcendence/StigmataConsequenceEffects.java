package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataConsequence;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataOverloadDegree;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataStage;
import org.joml.Vector3f;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Per-bearer, per-family instability. Ordinary actions build pressure, a visible warning precedes
 * loss of control, and each family has its own remedy. Runtime pressure survives changing tools.
 * All terrain-facing manifestations are temporary entities or particles.
 */
@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID)
public final class StigmataConsequenceEffects {
    public static final String RUNTIME_KEY = "esotericism_tinker_stigmata_instability";
    public static final String SUMMON_KEY = "esotericism_tinker_stigmata_summon";
    public static final String TEST_DUMMY = "esotericism_tinker_stigmata_test_dummy";
    private static final String ZONE_KEY = "esotericism_tinker_stigmata_zone";
    private static final ThreadLocal<Boolean> APPLYING_CONSEQUENCE_DAMAGE =
            ThreadLocal.withInitial(() -> false);
    private static final Handler HANDLER = new ReworkedHandler();
    public static final int WARNING_THRESHOLD = 70;
    private static final int OMEN_THRESHOLD = 50;
    public static final int CANCEL_THRESHOLD = 40;
    public static final int COOLDOWN_TICKS = 120;

    private StigmataConsequenceEffects() {}

    public record ConsequenceState(
            StigmataConsequence consequence, int consequenceSeed,
            int stage, int overload, int burden) {
        public static ConsequenceState of(IToolStackView tool, StigmataData data) {
            return new ConsequenceState(data.consequence(), data.consequenceSeed(),
                    data.stage(), data.overload(tool), data.burden());
        }

        public int strength() {
            return overload <= 0 ? 0 : StigmataOverloadDegree.fromValues(overload, burden).ordinal() + 1;
        }

        public int actionGain() { return 6 + 2 * strength(); }
        public int warningTicks() { return 70 - 10 * stage; }
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

    public static Handler get(StigmataConsequence consequence) { return HANDLER; }

    private static String key(StigmataConsequence family) {
        return family.name().toLowerCase(Locale.ROOT);
    }

    private static CompoundTag root(LivingEntity holder) {
        CompoundTag persistent = holder.getPersistentData();
        if (!persistent.contains(RUNTIME_KEY, CompoundTag.TAG_COMPOUND)) {
            persistent.put(RUNTIME_KEY, new CompoundTag());
        }
        return persistent.getCompound(RUNTIME_KEY);
    }

    private static CompoundTag frame(LivingEntity holder, StigmataConsequence family) {
        CompoundTag root = root(holder);
        String key = key(family);
        if (!root.contains(key, CompoundTag.TAG_COMPOUND)) root.put(key, new CompoundTag());
        return root.getCompound(key);
    }

    public static int pressure(LivingEntity holder, StigmataConsequence family) {
        return holder.getPersistentData().getCompound(RUNTIME_KEY).getCompound(key(family)).getInt("pressure");
    }

    public static void setPressure(LivingEntity holder, StigmataConsequence family, int pressure) {
        CompoundTag frame = frame(holder, family);
        frame.putInt("pressure", Math.max(0, Math.min(100, pressure)));
        if (pressure < CANCEL_THRESHOLD) frame.remove("due");
    }

    public static void touch(ConsequenceState state, LivingEntity holder) {
        CompoundTag frame = frame(holder, state.consequence());
        long now = holder.level().getGameTime();
        // Several equipped copies share one family. Retain the strongest contribution this tick.
        if (frame.getLong("seen") != now || state.stage() > frame.getInt("stage")
            || state.strength() > state(frame, state.consequence()).strength()) {
            frame.putInt("seed", state.consequenceSeed());
            frame.putInt("stage", state.stage());
            frame.putInt("overload", state.overload());
            frame.putInt("burden", state.burden());
        }
        frame.putLong("seen", now);
    }

    private static ConsequenceState state(CompoundTag frame, StigmataConsequence family) {
        return new ConsequenceState(family, frame.getInt("seed"),
                Math.max(1, frame.getInt("stage")), Math.max(1, frame.getInt("overload")),
                Math.max(1, frame.getInt("burden")));
    }

    public static boolean isApplyingConsequenceDamage() { return APPLYING_CONSEQUENCE_DAMAGE.get(); }

    private static boolean hurt(LivingEntity holder, float amount) {
        if (APPLYING_CONSEQUENCE_DAMAGE.get() || holder.level().isClientSide || amount <= 0) return false;
        APPLYING_CONSEQUENCE_DAMAGE.set(true);
        try {
            return holder.hurt(holder.damageSources().magic(), amount);
        } finally {
            APPLYING_CONSEQUENCE_DAMAGE.set(false);
        }
    }

    private static boolean isImprint(StigmataConsequence family) {
        return family.ordinal() >= StigmataConsequence.DOMINION.ordinal();
    }

    private static boolean hasBlessing(LivingEntity holder) {
        return holder.getActiveEffects().stream().anyMatch(effect -> effect.getEffect().isBeneficial());
    }

    private static boolean satisfied(StigmataConsequence family, LivingEntity holder) {
        return switch (family) {
            case DOMINION -> holder.isPassenger();
            case NOCTURNE -> holder.level().dimensionType().hasSkyLight()
                             && holder.level().isNight() && holder.level().canSeeSky(holder.blockPosition());
            case ZENITH -> holder.getY() >= 128 && holder.level().canSeeSky(holder.blockPosition());
            case ABYSS -> holder.getY() <= 0 && holder.level().getMaxLocalRawBrightness(holder.blockPosition()) <= 7;
            case BEATITUDE -> hasBlessing(holder);
            case ANATHEMA -> holder.hasEffect(MobEffects.BAD_OMEN);
            default -> false;
        };
    }

    private static float outputMultiplier(ConsequenceState state, LivingEntity holder) {
        if (!isImprint(state.consequence())) return 1;
        if (satisfied(state.consequence(), holder)) {
            return 1.03F + 0.01F * state.strength();
        }
        // Grace is lost immediately; damage only deteriorates once pressure is appreciable.
        return pressure(holder, state.consequence()) < CANCEL_THRESHOLD ? 1 : 1 - 0.03F * state.strength();
    }

    private static void effect(LivingEntity target, MobEffect effect, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
    }

    /** One action per family per 10 ticks, including cleave, multishot, and multiple armor pieces. */
    public static void action(ConsequenceState state, LivingEntity holder,
                              @Nullable LivingEntity target, Vec3 point) {
        if (!(holder.level() instanceof ServerLevel) || isApplyingConsequenceDamage()) return;
        touch(state, holder);
        CompoundTag frame = frame(holder, state.consequence());
        long now = holder.level().getGameTime();
        if (frame.contains("action") && now - frame.getLong("action") < 10) return;
        frame.putLong("action", now);
        int gain = state.actionGain();
        if (state.consequence() == StigmataConsequence.DEVOURING && holder instanceof Player player) {
            player.causeFoodExhaustion(0.25F + 0.1F * state.strength());
            if (player.getFoodData().getFoodLevel() <= 6) gain += 8;
        }
        if (state.consequence() == StigmataConsequence.SACRAMENT && holder instanceof Player player) {
            // Enchanting can leave totalExperience above zero after the actual balance is gone.
            int available = player.experienceLevel > 0 ? state.stage()
                    : Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
            if (available > 0) player.giveExperiencePoints(-Math.min(available, state.stage()));
            else gain += 12;
        }
        if (state.consequence() == StigmataConsequence.BEATITUDE && target != null
            && (target instanceof Villager || target.isAlliedTo(holder)
                || target instanceof TamableAnimal pet && pet.isTame())) {
            gain += 25;
            frame.putLong("profaned", now);
        }
        setPressure(holder, state.consequence(), pressure(holder, state.consequence()) + gain);
        frame.putDouble("target_x", point.x);
        frame.putDouble("target_y", point.y);
        frame.putDouble("target_z", point.z);
        if (target != null) frame.putUUID("target", target.getUUID());
        else frame.remove("target");
        if (pressure(holder, state.consequence()) >= WARNING_THRESHOLD) {
            arm(state, holder, state.stage() == 1 ? point : holder.position(), target);
        }
    }

    public static boolean forceWarning(ConsequenceState state, LivingEntity holder, Vec3 point,
                                       @Nullable LivingEntity target) {
        if (state.overload() <= 0) return false;
        touch(state, holder);
        CompoundTag frame = frame(holder, state.consequence());
        frame.remove("cooldown");
        setPressure(holder, state.consequence(), WARNING_THRESHOLD);
        arm(state, holder, point, target);
        return true;
    }

    private static void arm(ConsequenceState state, LivingEntity holder, Vec3 point,
                            @Nullable LivingEntity target) {
        CompoundTag frame = frame(holder, state.consequence());
        long now = holder.level().getGameTime();
        if (frame.contains("due") || frame.getLong("cooldown") > now) return;
        frame.putLong("due", now + state.warningTicks());
        frame.putInt("warning_stage", state.stage());
        frame.putInt("warning_overload", state.overload());
        frame.putInt("warning_burden", state.burden());
        frame.putDouble("x", point.x);
        frame.putDouble("y", point.y);
        frame.putDouble("z", point.z);
        if (target != null) frame.putUUID("target", target.getUUID());
        if (target != null) frame.putUUID("warning_target", target.getUUID());
        else if (frame.hasUUID("target")) frame.putUUID("warning_target", frame.getUUID("target"));
        else frame.remove("warning_target");
        if (holder.level() instanceof ServerLevel level) {
            playOmenSound(level, holder, state.consequence(),
                    affectsBearer(state.consequence()) ? holder.position() : point, 0.5F, 0.25D);
        }
    }

    private static int remedy(ConsequenceState state, LivingEntity holder, CompoundTag frame, long now) {
        boolean resting = !frame.contains("action") || now - frame.getLong("action") >= 20;
        boolean still = !frame.contains("last_x")
                        || holder.position().distanceToSqr(new Vec3(frame.getDouble("last_x"),
                                frame.getDouble("last_y"), frame.getDouble("last_z"))) < 0.04D;
        return switch (state.consequence()) {
            case EXALTATION -> holder.onGround() && holder.isCrouching() ? -24 : 0;
            case MALEDICTION -> resting ? -40 : 0;
            case JUDGEMENT, OBSESSION, SACRAMENT -> 0;
            case INCUBATION -> resting && holder.onGround() && holder.isCrouching() ? -40 : 0;
            case DEFILEMENT -> !still ? -12 : 0;
            case DEVOURING -> resting && holder instanceof Player player
                              && player.getFoodData().getFoodLevel() >= 16
                              && player.getFoodData().getSaturationLevel() > 0 ? -40 : 0;
            case OFFERING -> resting ? -40 : 0;
            case IMMOLATION -> holder.isInWaterRainOrBubble() ? -40 : resting ? -16 : 0;
            case PENANCE -> still && !holder.swinging && !holder.isSprinting() ? -24 : 0;
            case DOMINION -> {
                if (holder.isPassenger()) {
                    frame.putLong("ridden", now);
                    yield -24;
                }
                yield frame.contains("ridden") && now - frame.getLong("ridden") <= 200 ? -6 : 2;
            }
            case NOCTURNE -> satisfied(state.consequence(), holder)
                             ? -(24 + Math.round(holder.level().getMoonBrightness() * 12)) : 2;
            case ZENITH, ABYSS -> satisfied(state.consequence(), holder) ? -24 : 2;
            case BEATITUDE -> frame.contains("profaned") && now - frame.getLong("profaned") < 100 ? 4
                             : satisfied(state.consequence(), holder) ? -24 : 2;
            case ANATHEMA -> !holder.level().getEntitiesOfClass(Villager.class,
                                     holder.getBoundingBox().inflate(12)).isEmpty() ? 6
                             : satisfied(state.consequence(), holder) ? -24 : 2;
        };
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity holder = event.getEntity();
        if (!(holder.level() instanceof ServerLevel level)) return;
        tickSummon(holder, level);
        CompoundTag persistent = holder.getPersistentData();
        if (!holder.isAlive()) {
            persistent.remove(RUNTIME_KEY);
            return;
        }
        if (!persistent.contains(RUNTIME_KEY)) return;
        CompoundTag root = persistent.getCompound(RUNTIME_KEY);
        long now = level.getGameTime();
        int highest = -1;
        StigmataConsequence displayed = null;
        for (StigmataConsequence family : StigmataConsequence.values()) {
            if (!root.contains(key(family))) continue;
            CompoundTag frame = root.getCompound(key(family));
            ConsequenceState state = state(frame, family);
            boolean active = frame.contains("seen") && now - frame.getLong("seen") <= 2;
            if (now % 20 == 0 && (!frame.contains("second") || frame.getLong("second") != now)) {
                frame.putLong("second", now);
                int change = active ? remedy(state, holder, frame, now) : -10;
                if (active && change == 0) {
                    boolean idle = !frame.contains("action") || now - frame.getLong("action") >= 20;
                    change = state.stage() == 3 ? 2 : idle ? -6 : 0;
                }
                setPressure(holder, family, pressure(holder, family) + change);
                frame.putDouble("last_x", holder.getX());
                frame.putDouble("last_y", holder.getY());
                frame.putDouble("last_z", holder.getZ());
                if (active && family == StigmataConsequence.PENANCE && pressure(holder, family) >= 40) {
                    effect(holder, MobEffects.MOVEMENT_SLOWDOWN, 25, 0);
                }
                if (active && pressure(holder, family) >= WARNING_THRESHOLD && !frame.contains("due")) {
                    Vec3 point = state.stage() == 1 && frame.contains("target_x")
                                 ? new Vec3(frame.getDouble("target_x"), frame.getDouble("target_y"),
                                            frame.getDouble("target_z")) : holder.position();
                    arm(state, holder, point, null);
                }
            }
            if (frame.contains("due")) {
                Vec3 point = new Vec3(frame.getDouble("x"), frame.getDouble("y"), frame.getDouble("z"));
                ConsequenceState warning = new ConsequenceState(family, state.consequenceSeed(),
                        frame.getInt("warning_stage"), frame.getInt("warning_overload"),
                        frame.getInt("warning_burden"));
                if (now >= frame.getLong("due")) {
                    LivingEntity target = frame.hasUUID("warning_target")
                                          && level.getEntity(frame.getUUID("warning_target")) instanceof LivingEntity living
                                          ? living : null;
                    burst(warning, holder, point, target);
                    frame.remove("due");
                    frame.putLong("cooldown", now + COOLDOWN_TICKS);
                    setPressure(holder, family, 35);
                } else {
                    warningVisuals(level, holder, point, warning, (int) (frame.getLong("due") - now));
                }
            } else if (active && pressure(holder, family) >= OMEN_THRESHOLD) {
                approachingVisuals(level, holder, state, pressure(holder, family), now);
            }
            int pressure = pressure(holder, family);
            if ((active || frame.contains("due")) && pressure > highest) {
                highest = pressure;
                displayed = family;
            }
            if (!active && pressure == 0 && !frame.contains("due")
                && now - frame.getLong("seen") > 600) root.remove(key(family));
        }
        if (now % 20 == 0 && holder instanceof Player player && displayed != null
            && hasEquippedTestSample(player, displayed)) {
            CompoundTag frame = root.getCompound(key(displayed));
            player.displayClientMessage(Component.translatable("stigmata_instability.esotericism_tinker.status",
                    displayed.displayName(), highest,
                    frame.contains("due") ? Component.translatable("stigmata_instability.esotericism_tinker.warning",
                            String.format(Locale.ROOT, "%.1f", Math.max(0, frame.getLong("due") - now) / 20.0D))
                                          : Component.translatable("stigmata_instability.esotericism_tinker.calm")),
                    true);
        }
        if (root.isEmpty()) persistent.remove(RUNTIME_KEY);
    }

    private static boolean hasEquippedTestSample(Player player, StigmataConsequence family) {
        EquipmentContext context = new EquipmentContext(player);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            var tool = context.getValidTool(slot);
            if (tool == null || tool.isBroken() || !tool.getPersistentData().contains(StigmataData.TEST_OVERLOAD)) continue;
            StigmataData data = StigmataData.read(tool);
            if (data.hasConsequenceSeed() && data.consequence() == family && data.overload(tool) > 0) return true;
        }
        return false;
    }

    private static SimpleParticleType particle(StigmataConsequence family) {
        return switch (family) {
            case EXALTATION, ZENITH -> ParticleTypes.CLOUD;
            case MALEDICTION, SACRAMENT -> ParticleTypes.WITCH;
            case JUDGEMENT -> ParticleTypes.ELECTRIC_SPARK;
            case OBSESSION, ABYSS -> ParticleTypes.SOUL;
            case INCUBATION, DEVOURING -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case DEFILEMENT -> ParticleTypes.SCULK_CHARGE_POP;
            case OFFERING, PENANCE -> ParticleTypes.DAMAGE_INDICATOR;
            case IMMOLATION -> ParticleTypes.FLAME;
            case DOMINION -> ParticleTypes.CRIT;
            case NOCTURNE -> ParticleTypes.END_ROD;
            case BEATITUDE -> ParticleTypes.ENCHANT;
            case ANATHEMA -> ParticleTypes.ANGRY_VILLAGER;
        };
    }

    private static DustParticleOptions tint(StigmataConsequence family) {
        int color = switch (family) {
            case EXALTATION -> 0xC5ECFF;
            case MALEDICTION -> 0xB94DE8;
            case JUDGEMENT -> 0x70CFFF;
            case OBSESSION -> 0x48E6C9;
            case INCUBATION -> 0x9DBB39;
            case DEFILEMENT -> 0x08A6AD;
            case DEVOURING -> 0x77952E;
            case SACRAMENT -> 0xB99AFF;
            case OFFERING -> 0xCF233C;
            case IMMOLATION -> 0xFF782C;
            case PENANCE -> 0xA6A9B5;
            case DOMINION -> 0xE4AD42;
            case NOCTURNE -> 0x9DABEF;
            case ZENITH -> 0xE4F7FF;
            case ABYSS -> 0x365BB4;
            case BEATITUDE -> 0xFFE6A1;
            case ANATHEMA -> 0xAB244C;
        };
        return new DustParticleOptions(new Vector3f((color >> 16 & 255) / 255.0F,
                (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F), 1.35F);
    }

    private static SoundEvent manifestationSound(StigmataConsequence family) {
        return switch (family) {
            case EXALTATION -> SoundEvents.ALLAY_DEATH;
            case MALEDICTION -> SoundEvents.EVOKER_PREPARE_SUMMON;
            case JUDGEMENT -> SoundEvents.BEACON_DEACTIVATE;
            case OBSESSION -> SoundEvents.VEX_CHARGE;
            case INCUBATION -> SoundEvents.SILVERFISH_AMBIENT;
            case DEFILEMENT -> SoundEvents.SCULK_CATALYST_BLOOM;
            case DEVOURING -> SoundEvents.FOX_EAT;
            case SACRAMENT -> SoundEvents.ENCHANTMENT_TABLE_USE;
            case OFFERING -> SoundEvents.PLAYER_HURT;
            case IMMOLATION -> SoundEvents.FIRECHARGE_USE;
            case PENANCE -> SoundEvents.CHAIN_BREAK;
            case DOMINION -> SoundEvents.BELL_RESONATE;
            case NOCTURNE -> SoundEvents.PHANTOM_AMBIENT;
            case ZENITH -> SoundEvents.ELYTRA_FLYING;
            case ABYSS -> SoundEvents.SOUL_ESCAPE;
            case BEATITUDE -> SoundEvents.AMETHYST_BLOCK_BREAK;
            case ANATHEMA -> SoundEvents.ENDERMAN_STARE;
        };
    }

    private static SoundEvent omenSound(StigmataConsequence family) {
        return switch (family) {
            case EXALTATION -> SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
            case MALEDICTION -> SoundEvents.EVOKER_PREPARE_ATTACK;
            case JUDGEMENT -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case OBSESSION -> SoundEvents.VEX_AMBIENT;
            case INCUBATION -> SoundEvents.SILVERFISH_AMBIENT;
            case DEFILEMENT -> SoundEvents.SCULK_CATALYST_BLOOM;
            case DEVOURING -> SoundEvents.FOX_EAT;
            case SACRAMENT -> SoundEvents.ENCHANTMENT_TABLE_USE;
            case OFFERING -> SoundEvents.WARDEN_HEARTBEAT;
            case IMMOLATION -> SoundEvents.FIRE_AMBIENT;
            case PENANCE -> SoundEvents.CHAIN_HIT;
            case DOMINION -> SoundEvents.BELL_RESONATE;
            case NOCTURNE -> SoundEvents.PHANTOM_AMBIENT;
            case ZENITH -> SoundEvents.ELYTRA_FLYING;
            case ABYSS -> SoundEvents.SOUL_ESCAPE;
            case BEATITUDE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case ANATHEMA -> SoundEvents.ENDERMAN_STARE;
        };
    }

    private static void playOmenSound(ServerLevel level, LivingEntity holder, StigmataConsequence family,
                                      Vec3 point, float volume, double progress) {
        float pitch = switch (family) {
            case EXALTATION, JUDGEMENT, IMMOLATION -> (float) (0.7D + progress * 0.5D);
            case DEVOURING, OFFERING, PENANCE, ABYSS, ANATHEMA -> (float) (0.65D - progress * 0.1D);
            case DOMINION, NOCTURNE, BEATITUDE -> (float) (1.1D - progress * 0.4D);
            default -> (float) (0.8D + progress * 0.2D);
        };
        level.playSound(null, BlockPos.containing(point), omenSound(family), SoundSource.PLAYERS, volume, pitch);
        // Echo the same theme locally when a distant marked target would hide the warning.
        if (point.distanceToSqr(holder.position()) > 64) {
            level.playSound(null, holder.blockPosition(), omenSound(family), SoundSource.PLAYERS,
                    volume * 0.6F, pitch);
        }
    }

    /** These consequences follow the bearer; the others manifest at the fixed warning location. */
    private static boolean affectsBearer(StigmataConsequence family) {
        return switch (family) {
            case DEVOURING, SACRAMENT, OFFERING, PENANCE,
                 DOMINION, NOCTURNE, ZENITH, ABYSS, BEATITUDE, ANATHEMA -> true;
            default -> false;
        };
    }

    private static void circle(ServerLevel level, Vec3 point, ParticleOptions particle,
                               double radius, double height, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count + phase;
            level.sendParticles(particle, point.x + Math.cos(angle) * radius,
                    point.y + height, point.z + Math.sin(angle) * radius, 1, 0, 0.015D, 0, 0);
        }
    }

    /** A few motes below the sight line make the cue readable in first person as well. */
    private static void glimpse(ServerLevel level, LivingEntity holder, ParticleOptions particle, int count) {
        Vec3 look = holder.getLookAngle();
        Vec3 front = new Vec3(look.x, 0, look.z);
        front = front.lengthSqr() < 0.001D ? new Vec3(0, 0, 1) : front.normalize();
        Vec3 point = holder.position().add(front.scale(0.95D)).add(0, holder.getEyeHeight() - 0.45D, 0);
        level.sendParticles(particle, point.x, point.y, point.z, count, 0.28D, 0.12D, 0.28D, 0.005D);
    }

    private static void approachingVisuals(ServerLevel level, LivingEntity holder,
                                            ConsequenceState state, int pressure, long now) {
        StigmataConsequence family = state.consequence();
        int interval = pressure >= 60 ? 20 : 30;
        if (now % interval == 0) {
            omenMotif(level, holder.position(), state, (pressure - OMEN_THRESHOLD) / 20.0D, true);
            glimpse(level, holder, omenGlimpse(family), pressure >= 60 ? 4 : 2);
        }
        if (pressure >= 60 && now % 60 == 0) {
            playOmenSound(level, holder, family, holder.position(), 0.25F, 0);
        }
    }

    private static void warningVisuals(ServerLevel level, LivingEntity holder, Vec3 point,
                                       ConsequenceState state, int remaining) {
        StigmataConsequence family = state.consequence();
        boolean imminent = remaining <= 20;
        int interval = imminent ? 5 : 10;
        if (remaining % interval == 0) {
            double progress = 1 - remaining / (double) state.warningTicks();
            if (!affectsBearer(family)) {
                // A quiet, fixed boundary shows the area without replacing the family's own omen.
                circle(level, point, tint(family), 2.5D, 0.15D, 12, 0);
            }
            omenMotif(level, affectsBearer(family) ? holder.position() : point, state, progress, false);
            glimpse(level, holder, omenGlimpse(family), imminent ? 5 : 2);
        }
        if (remaining == 20) {
            playOmenSound(level, holder, family, affectsBearer(family) ? holder.position() : point,
                    0.65F, 1);
        }
    }

    private static ParticleOptions omenGlimpse(StigmataConsequence family) {
        return switch (family) {
            case EXALTATION, NOCTURNE, ZENITH -> ParticleTypes.END_ROD;
            case DEVOURING, ANATHEMA -> ParticleTypes.SMOKE;
            case SACRAMENT -> ParticleTypes.ENCHANT;
            case OFFERING, DOMINION, BEATITUDE -> tint(family);
            case IMMOLATION, PENANCE -> ParticleTypes.ASH;
            default -> particle(family);
        };
    }

    /** The sparse approach and the full warning share a theme, rather than a common countdown ring. */
    private static void omenMotif(ServerLevel level, Vec3 point, ConsequenceState state,
                                   double progress, boolean sparse) {
        progress = Math.max(0, Math.min(1, progress));
        StigmataConsequence family = state.consequence();
        DustParticleOptions color = tint(family);
        int count = sparse ? 8 : progress >= 0.65D ? 24 : 16;
        double phase = level.getGameTime() * 0.06D * (1 + progress * 0.6D);
        // Later stages bring the same manifestation closer to the bearer or marked origin.
        double spread = (sparse ? 0.55D : 1) * (1.3D - 0.2D * (state.stage() - 1));
        for (int i = 0; i < count; i++) {
            double t = i / (double) (count - 1);
            double angle = Math.PI * 2 * t;
            double spin = angle + phase;
            ParticleOptions type = color;
            double x = 0, y = 0, z = 0;
            double vx = 0, vy = 0, vz = 0;
            switch (family) {
                case EXALTATION -> {
                    type = i % 3 == 0 ? ParticleTypes.CLOUD : ParticleTypes.END_ROD;
                    x = Math.cos(spin * 2) * (0.6D + 0.3D * t);
                    z = Math.sin(spin * 2) * (0.6D + 0.3D * t);
                    y = 0.2D + t * (1.2D + 1.4D * progress);
                    vy = 0.04D + 0.08D * progress;
                }
                case MALEDICTION -> {
                    type = ParticleTypes.WITCH;
                    double radius = 1.6D - 0.8D * progress;
                    x = Math.cos(-spin) * radius;
                    z = Math.sin(-spin) * radius;
                    y = 0.3D + 0.6D * Math.pow(Math.sin(angle * 2 + phase), 2);
                    vx = z * 0.05D - x * 0.02D;
                    vz = -x * 0.05D - z * 0.02D;
                }
                case JUDGEMENT -> {
                    type = ParticleTypes.ELECTRIC_SPARK;
                    x = (i % 2 == 0 ? -1 : 1) * (0.12D + 0.2D * Math.sin(phase + i));
                    z = 0.18D * Math.sin(i * 2.2D + phase);
                    y = 1.6D + t * (0.8D + 0.8D * progress);
                    vx = Math.sin(i + phase) * 0.05D;
                    vy = -0.08D * (0.3D + progress);
                }
                case OBSESSION -> {
                    type = ParticleTypes.SOUL;
                    double trail = phase + (i % 2) * Math.PI + (i / 2) * 0.08D;
                    double radius = 1.6D - 0.8D * progress;
                    x = Math.cos(trail) * radius;
                    z = Math.sin(trail) * radius;
                    y = 0.6D + 0.7D * t;
                    vx = -z * 0.05D;
                    vz = x * 0.05D;
                }
                case INCUBATION -> {
                    type = i % 3 == 0 ? ParticleTypes.SPORE_BLOSSOM_AIR : color;
                    double bud = Math.PI * 2 * (i % 3) / 3;
                    x = Math.cos(bud) * 0.9D + Math.cos(angle * 3) * 0.18D;
                    z = Math.sin(bud) * 0.9D + Math.sin(angle * 3) * 0.18D;
                    y = 0.08D + 0.09D * (1 + Math.sin(phase * 2)) * (1 + Math.cos(angle));
                    vy = 0.015D + progress * 0.04D;
                }
                case DEFILEMENT -> {
                    type = i % 3 == 0 ? ParticleTypes.SCULK_CHARGE_POP : color;
                    double branch = Math.PI * 2 * (i % 5) / 5;
                    double radius = (0.2D + 1.9D * progress) * (0.2D + 0.8D * t);
                    x = Math.cos(branch) * radius + Math.sin(i * 5 + phase) * 0.06D;
                    z = Math.sin(branch) * radius + Math.cos(i * 5 + phase) * 0.06D;
                    y = 0.1D;
                }
                case DEVOURING -> {
                    type = ParticleTypes.SMOKE;
                    double radius = 1.5D * (1 - t) * (1 - 0.4D * progress) + 0.2D;
                    x = Math.cos(spin) * radius;
                    z = Math.sin(spin) * radius;
                    y = 0.5D + 0.5D * t;
                    vx = -x * 0.08D;
                    vy = (0.8D - y) * 0.08D;
                    vz = -z * 0.08D;
                }
                case SACRAMENT -> {
                    type = i % 3 == 0 ? ParticleTypes.ENCHANT : color;
                    double radius = 0.4D + t * (0.6D + progress);
                    x = Math.cos(angle) * radius;
                    z = Math.sin(angle) * radius;
                    y = 0.6D + 1.1D * t;
                    vx = Math.cos(angle) * (0.035D + 0.06D * progress);
                    vz = Math.sin(angle) * (0.035D + 0.06D * progress);
                    vy = 0.03D;
                }
                case OFFERING -> {
                    double radius = 0.25D + 0.95D * progress * t;
                    x = Math.cos(angle) * radius;
                    z = Math.sin(angle) * radius;
                    y = 0.65D + 0.55D * t;
                    vx = Math.cos(angle) * 0.06D;
                    vz = Math.sin(angle) * 0.06D;
                    vy = 0.02D;
                }
                case IMMOLATION -> {
                    type = sparse ? ParticleTypes.ASH
                            : i % 3 == 0 || progress < 0.4D ? ParticleTypes.SMOKE : ParticleTypes.FLAME;
                    x = Math.cos(angle) * (0.5D + 0.4D * progress);
                    z = Math.sin(angle) * (0.5D + 0.4D * progress);
                    y = 0.1D + t * (0.4D + 0.6D * progress);
                    vy = 0.03D + 0.04D * progress;
                }
                case PENANCE -> {
                    double chain = Math.PI * (i % 4) / 2;
                    double radius = 1.4D - 0.7D * progress;
                    x = Math.cos(chain) * radius + Math.cos(angle * 4) * 0.08D;
                    z = Math.sin(chain) * radius + Math.sin(angle * 4) * 0.08D;
                    y = 0.15D + t * 1.9D;
                    vy = -0.03D;
                }
                case DOMINION -> {
                    double tremor = progress * 0.14D * Math.sin(phase * 3 + i);
                    x = Math.cos(angle) * 0.9D + tremor;
                    z = Math.sin(angle) * 0.9D - tremor;
                    y = 1.9D + (i % 2 == 0 ? 0.25D : 0);
                    if (progress > 0.75D && i % 3 == 0) y -= progress * 0.5D;
                }
                case NOCTURNE -> {
                    type = t < progress * 0.45D ? ParticleTypes.SMOKE : ParticleTypes.END_ROD;
                    double crescent = -Math.PI * 0.75D + Math.PI * 1.5D * t;
                    x = Math.cos(crescent) * 0.8D;
                    y = 1.1D + Math.sin(crescent) * 0.8D;
                    z = Math.sin(phase) * 0.15D;
                }
                case ZENITH -> {
                    type = i % 3 == 0 ? ParticleTypes.CLOUD : ParticleTypes.END_ROD;
                    double stream = Math.PI * 2 * (i % 3) / 3;
                    x = Math.cos(stream) * (0.7D - 0.15D * progress);
                    z = Math.sin(stream) * (0.7D - 0.15D * progress);
                    y = 2.8D - t * (1.4D + progress);
                    vy = -0.1D - 0.07D * progress;
                }
                case ABYSS -> {
                    type = i % 3 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : color;
                    double tendril = phase * 0.4D + Math.PI * 2 * (i % 3) / 3 + t;
                    x = Math.cos(tendril) * 0.8D * (1 - 0.4D * t);
                    z = Math.sin(tendril) * 0.8D * (1 - 0.4D * t);
                    y = 0.05D + t * (0.6D + 1.5D * progress);
                    vy = 0.035D;
                }
                case BEATITUDE -> {
                    type = progress > 0.6D && i % 3 == 0 ? ParticleTypes.ASH : color;
                    x = Math.cos(angle) * 0.95D;
                    z = Math.sin(angle) * 0.95D;
                    y = 1.85D + Math.sin(angle) * 0.08D;
                    if (i % 4 == 0 && progress > 0.3D) {
                        y -= progress * 0.6D;
                        vy = -0.04D;
                    }
                }
                case ANATHEMA -> {
                    type = i % 3 == 0 ? ParticleTypes.SMOKE : color;
                    double barb = Math.PI * 2 * (i % 5) / 5;
                    double radius = 0.5D + t * (0.8D + 0.5D * progress);
                    x = Math.cos(barb) * radius;
                    z = Math.sin(barb) * radius;
                    y = 0.2D + t * 0.9D;
                    vx = Math.cos(barb) * progress * 0.03D;
                    vz = Math.sin(barb) * progress * 0.03D;
                    vy = type == color ? 0 : 0.04D;
                }
            }
            double px = point.x + x * spread;
            double py = point.y + y * (sparse ? 0.65D : 1);
            double pz = point.z + z * spread;
            level.sendParticles(type, px, py, pz, 0, vx, vy, vz, 1);
            if (!sparse && type != color && i % 4 == 0) {
                level.sendParticles(color, px, py, pz, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void spiral(ServerLevel level, Vec3 point, ParticleOptions particle,
                               double radius, double height, boolean descending, int count) {
        for (int i = 0; i < count; i++) {
            double fraction = i / (double) (count - 1);
            double angle = fraction * Math.PI * 4;
            double y = descending ? height * (1 - fraction) : height * fraction;
            level.sendParticles(particle, point.x + Math.cos(angle) * radius, point.y + 0.2D + y,
                    point.z + Math.sin(angle) * radius, 0, 0, descending ? -1 : 1, 0, 0.12D);
        }
    }

    private static void rays(ServerLevel level, Vec3 point, ParticleOptions particle, boolean inward) {
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2 * i / 20;
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            double radius = inward ? 1.8D : 0.3D;
            level.sendParticles(particle, point.x + x * radius, point.y + 0.8D, point.z + z * radius,
                    0, inward ? -x : x, inward ? -0.1D : 0.25D, inward ? -z : z, 0.18D);
        }
    }

    private static void burstVisuals(ServerLevel level, LivingEntity holder, Vec3 marked,
                                     ConsequenceState state) {
        StigmataConsequence family = state.consequence();
        Vec3 point = affectsBearer(family) ? holder.position() : marked;
        DustParticleOptions color = tint(family);
        if (!affectsBearer(family)) circle(level, point, color, 2.5D, 0.25D, 36, 0);
        level.sendParticles(particle(family), point.x, point.y + 0.8D, point.z,
                24 + state.stage() * 6, 0.7D, 0.5D, 0.7D, 0.06D);
        switch (family) {
            case EXALTATION -> {
                spiral(level, point, ParticleTypes.CLOUD, 1.2D, 2.8D, false, 28);
                spiral(level, point, ParticleTypes.END_ROD, 0.75D, 2.8D, false, 20);
            }
            case MALEDICTION -> {
                spiral(level, point, ParticleTypes.WITCH, 1.4D, 2.2D, false, 32);
                rays(level, point, ParticleTypes.SMOKE, false);
            }
            case JUDGEMENT -> {
                spiral(level, point, ParticleTypes.ELECTRIC_SPARK, 0.55D, 4, true, 32);
                rays(level, point, ParticleTypes.ELECTRIC_SPARK, false);
            }
            case OBSESSION -> {
                spiral(level, point, ParticleTypes.SOUL, 1.1D, 2.2D, false, 24);
                rays(level, point, ParticleTypes.REVERSE_PORTAL, true);
            }
            case INCUBATION -> {
                circle(level, point, color, 1.3D, 0.3D, 24, 0);
                rays(level, point, ParticleTypes.SPORE_BLOSSOM_AIR, false);
            }
            case DEFILEMENT -> {
                circle(level, point, color, 1.2D, 0.4D, 24, 0);
                rays(level, point, ParticleTypes.SCULK_CHARGE_POP, false);
            }
            case DEVOURING -> {
                rays(level, point, ParticleTypes.SMOKE, true);
                spiral(level, point, color, 0.65D, 1.2D, true, 24);
            }
            case SACRAMENT -> {
                rays(level, point, ParticleTypes.ENCHANT, true);
                spiral(level, point, ParticleTypes.REVERSE_PORTAL, 1.1D, 1.8D, true, 24);
            }
            case OFFERING -> {
                rays(level, point, color, false);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, point.x, point.y + 1, point.z,
                        14, 0.5D, 0.5D, 0.5D, 0.08D);
            }
            case IMMOLATION -> {
                rays(level, point, ParticleTypes.FLAME, false);
                spiral(level, point, ParticleTypes.SMOKE, 1.1D, 2.4D, false, 24);
            }
            case PENANCE -> {
                for (int i = 0; i < 4; i++) {
                    double angle = Math.PI * i / 2;
                    Vec3 pillar = point.add(Math.cos(angle), 0, Math.sin(angle));
                    spiral(level, pillar, color, 0.15D, 2.2D, true, 10);
                }
                level.sendParticles(ParticleTypes.ASH, point.x, point.y + 1.6D, point.z,
                        24, 1, 0.5D, 1, 0.02D);
            }
            case DOMINION -> {
                circle(level, point, color, 1.1D, 2.1D, 24, 0);
                spiral(level, point, ParticleTypes.CRIT, 1.1D, 2.1D, true, 28);
            }
            case NOCTURNE -> {
                spiral(level, point, ParticleTypes.END_ROD, 1.4D, 2, true, 24);
                rays(level, point, ParticleTypes.REVERSE_PORTAL, true);
            }
            case ZENITH -> {
                spiral(level, point, ParticleTypes.CLOUD, 1.5D, 3.5D, true, 32);
                spiral(level, point, ParticleTypes.END_ROD, 0.8D, 3.5D, true, 24);
            }
            case ABYSS -> {
                spiral(level, point, ParticleTypes.SOUL_FIRE_FLAME, 1.1D, 2.6D, false, 28);
                rays(level, point, ParticleTypes.SOUL, true);
            }
            case BEATITUDE -> {
                circle(level, point, color, 1.1D, 2, 28, 0);
                rays(level, point.add(0, 1.1D, 0), ParticleTypes.ENCHANTED_HIT, false);
            }
            case ANATHEMA -> {
                rays(level, point, color, false);
                spiral(level, point, ParticleTypes.SMOKE, 1.2D, 2.5D, false, 28);
            }
        }
        glimpse(level, holder, color, 8);
        level.playSound(null, BlockPos.containing(point), manifestationSound(family),
                SoundSource.PLAYERS, 0.8F, 0.8F + state.stage() * 0.1F);
    }

    private static void zone(ServerLevel level, LivingEntity owner, Vec3 point,
                             StigmataConsequence family, MobEffect effect, int duration, int amplifier) {
        for (AreaEffectCloud old : level.getEntitiesOfClass(AreaEffectCloud.class,
                owner.getBoundingBox().inflate(48), cloud -> cloud.getPersistentData().hasUUID(ZONE_KEY)
                        && owner.getUUID().equals(cloud.getPersistentData().getUUID(ZONE_KEY))
                        && cloud.getPersistentData().getString("family").equals(key(family)))) old.discard();
        AreaEffectCloud cloud = new AreaEffectCloud(level, point.x, point.y, point.z);
        cloud.setOwner(owner);
        cloud.setParticle(particle(family));
        cloud.setRadius(2.5F);
        cloud.setWaitTime(0);
        cloud.setDuration(100);
        cloud.setRadiusPerTick(-0.015F);
        cloud.addEffect(new MobEffectInstance(effect, duration, amplifier));
        cloud.getPersistentData().putUUID(ZONE_KEY, owner.getUUID());
        cloud.getPersistentData().putString("family", key(family));
        level.addFreshEntity(cloud);
    }

    private static void burst(ConsequenceState state, LivingEntity holder, Vec3 point,
                              @Nullable LivingEntity target) {
        if (!(holder.level() instanceof ServerLevel level)) return;
        int strength = state.strength();
        burstVisuals(level, holder, point, state);
        switch (state.consequence()) {
            case EXALTATION -> zone(level, holder, point, state.consequence(),
                    MobEffects.LEVITATION, 35 + 5 * strength, state.stage() == 3 ? 1 : 0);
            case MALEDICTION -> zone(level, holder, point, state.consequence(),
                    state.stage() == 3 ? MobEffects.WITHER : MobEffects.POISON, 60 + 10 * strength, 0);
            case JUDGEMENT -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(point);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                for (LivingEntity near : nearby(level, point)) hurt(near, 2 + strength);
                level.playSound(null, BlockPos.containing(point), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.PLAYERS, 0.6F, 1.2F);
            }
            case OBSESSION -> summon(state, holder, point, target, false);
            case INCUBATION -> summon(state, holder, point, target, true);
            case DEFILEMENT -> zone(level, holder, point, state.consequence(), MobEffects.MOVEMENT_SLOWDOWN,
                    50 + 10 * strength, 1);
            case DEVOURING -> {
                if (holder instanceof Player player) {
                    player.causeFoodExhaustion(2 + strength);
                    effect(player, MobEffects.HUNGER, 60 + 10 * strength, 0);
                    if (state.stage() == 3 && player.getFoodData().getFoodLevel() <= 6) hurt(player, 1 + strength * 0.5F);
                }
            }
            case SACRAMENT -> {
                effect(holder, MobEffects.WEAKNESS, 60 + 15 * strength, 0);
                effect(holder, MobEffects.DIG_SLOWDOWN, 40 + 10 * strength, 0);
            }
            case OFFERING -> hurt(holder, 1 + strength * 0.5F);
            case IMMOLATION -> {
                for (LivingEntity near : nearby(level, point)) near.setSecondsOnFire(2 + strength);
            }
            case PENANCE -> {
                effect(holder, MobEffects.MOVEMENT_SLOWDOWN, 60 + 10 * strength, 1);
                effect(holder, MobEffects.DIG_SLOWDOWN, 60 + 10 * strength, 1);
            }
            case DOMINION -> {
                effect(holder, MobEffects.CONFUSION, 100, 0);
                effect(holder, MobEffects.MOVEMENT_SLOWDOWN, 60 + 10 * strength, 0);
            }
            case NOCTURNE -> {
                effect(holder, MobEffects.BLINDNESS, 40 + 10 * strength, 0);
                effect(holder, MobEffects.WEAKNESS, 60, 0);
            }
            case ZENITH -> {
                holder.setDeltaMovement(holder.getDeltaMovement().add(0, -0.5D, 0));
                holder.hurtMarked = true;
                effect(holder, MobEffects.MOVEMENT_SLOWDOWN, 60 + 10 * strength, 1);
            }
            case ABYSS -> {
                effect(holder, MobEffects.GLOWING, 120 + 20 * strength, 0);
                effect(holder, MobEffects.WEAKNESS, 60 + 10 * strength, 0);
            }
            case BEATITUDE -> {
                List<MobEffectInstance> blessings = new ArrayList<>(holder.getActiveEffects());
                for (MobEffectInstance blessing : blessings) {
                    if (blessing.getEffect().isBeneficial()) {
                        holder.removeEffect(blessing.getEffect());
                        break;
                    }
                }
                effect(holder, MobEffects.WEAKNESS, 60 + 10 * strength, 0);
            }
            case ANATHEMA -> {
                effect(holder, MobEffects.GLOWING, 100 + 20 * strength, 0);
                for (Monster monster : level.getEntitiesOfClass(Monster.class,
                        holder.getBoundingBox().inflate(16))) monster.setTarget(holder);
            }
        }
    }

    private static List<LivingEntity> nearby(ServerLevel level, Vec3 point) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(2.5),
                entity -> entity.isAlive() && entity.position().distanceToSqr(point) <= 6.25D);
    }

    private static void summon(ConsequenceState state, LivingEntity holder, Vec3 point,
                               @Nullable LivingEntity target, boolean brood) {
        if (state.stage() == 1 && (target == null || !target.isAlive())) return;
        ServerLevel level = (ServerLevel) holder.level();
        int maximum = brood ? 2 : 1;
        int current = level.getEntitiesOfClass(Mob.class, holder.getBoundingBox().inflate(48),
                mob -> mob.getPersistentData().hasUUID(SUMMON_KEY)
                       && holder.getUUID().equals(mob.getPersistentData().getUUID(SUMMON_KEY))
                       && mob.getPersistentData().getBoolean("brood") == brood).size();
        for (int i = current; i < maximum; i++) {
            Mob mob = brood ? EntityType.SILVERFISH.create(level) : EntityType.VEX.create(level);
            if (mob == null) return;
            double angle = Math.PI * 2 * i / maximum;
            double distance = brood ? 2.0D : 2.75D;
            mob.moveTo(point.x + Math.cos(angle) * distance, point.y + (brood ? 0 : 1),
                    point.z + Math.sin(angle) * distance, 0, 0);
            mob.setPersistenceRequired();
            mob.getPersistentData().putUUID(SUMMON_KEY, holder.getUUID());
            mob.getPersistentData().putBoolean("brood", brood);
            mob.getPersistentData().putLong("expires", level.getGameTime() + (brood ? 300 : 400));
            mob.getPersistentData().putInt("stage", state.stage());
            LivingEntity prey = state.stage() == 1 ? target : holder;
            if (prey != null) {
                mob.getPersistentData().putUUID("prey", prey.getUUID());
                mob.setTarget(prey);
            }
            if (mob instanceof Silverfish) mob.setNoAi(true); // no infesting or waking ordinary silverfish
            if (mob instanceof Vex vex) vex.setLimitedLife(400);
            level.addFreshEntity(mob);
            level.sendParticles(brood ? ParticleTypes.SPORE_BLOSSOM_AIR : ParticleTypes.SOUL,
                    mob.getX(), mob.getY() + 0.4D, mob.getZ(), 18, 0.3D, 0.4D, 0.3D, 0.04D);
        }
    }

    private static void tickSummon(LivingEntity entity, ServerLevel level) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.hasUUID(SUMMON_KEY)) return;
        if (level.getGameTime() >= tag.getLong("expires")) {
            entity.discard();
            return;
        }
        if (tag.hasUUID("prey")) {
            Entity prey = level.getEntity(tag.getUUID("prey"));
            if (!(prey instanceof LivingEntity living) || !living.isAlive()) {
                entity.discard();
                return;
            }
            // Vanilla vex targeting can replace setTarget() with a nearby player.
            // Rebind from the saved prey, including after an entity reload.
            if (entity instanceof Vex vex) vex.setTarget(living);
        }
        if (entity instanceof Silverfish && tag.hasUUID("prey")
            && level.getEntity(tag.getUUID("prey")) instanceof LivingEntity prey && prey.isAlive()) {
            Vec3 step = prey.position().subtract(entity.position());
            if (step.lengthSqr() > 1) entity.setDeltaMovement(step.normalize().scale(0.12D).add(0, -0.04D, 0));
            else if (entity.tickCount % 20 == 0) {
                effect(prey, MobEffects.POISON, 40, 0);
                effect(prey, MobEffects.HUNGER, 60, 0);
                entity.discard();
            }
        }
    }

    @SubscribeEvent
    public static void onSummonDeath(LivingDeathEvent event) {
        LivingEntity mob = event.getEntity();
        CompoundTag tag = mob.getPersistentData();
        if (tag.contains(RUNTIME_KEY)) clear(mob);
        if (tag.hasUUID(SUMMON_KEY) && mob.level() instanceof ServerLevel level
            && level.getEntity(tag.getUUID(SUMMON_KEY)) instanceof LivingEntity owner) {
            setPressure(owner, tag.getBoolean("brood") ? StigmataConsequence.INCUBATION : StigmataConsequence.OBSESSION, 0);
            StigmataConsequence family = tag.getBoolean("brood")
                    ? StigmataConsequence.INCUBATION : StigmataConsequence.OBSESSION;
            circle(level, mob.position(), particle(family), 0.6D, 0.15D, 10, 0);
            level.sendParticles(particle(family), mob.getX(), mob.getY() + 1, mob.getZ(),
                    28, 0.7D, 0.8D, 0.7D, 0.04D);
        }
    }

    @SubscribeEvent
    public static void onSummonDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().hasUUID(SUMMON_KEY)
            || event.getEntity().getPersistentData().getBoolean(TEST_DUMMY)) event.getDrops().clear();
    }

    @SubscribeEvent
    public static void onSummonExperience(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().hasUUID(SUMMON_KEY)
            || event.getEntity().getPersistentData().getBoolean(TEST_DUMMY)) event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void onExperiencePickup(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        int pressure = pressure(player, StigmataConsequence.SACRAMENT);
        if (pressure > 0) setPressure(player, StigmataConsequence.SACRAMENT,
                pressure - event.getOrb().getValue() * 4);
    }

    public static int clear(LivingEntity holder) {
        holder.getPersistentData().remove(RUNTIME_KEY);
        if (!(holder.level() instanceof ServerLevel level)) return 0;
        int removed = 0;
        for (Entity entity : level.getEntities(holder, holder.getBoundingBox().inflate(64),
                entity -> entity.getPersistentData().hasUUID(SUMMON_KEY)
                          && holder.getUUID().equals(entity.getPersistentData().getUUID(SUMMON_KEY))
                          || entity.getPersistentData().hasUUID(ZONE_KEY)
                             && holder.getUUID().equals(entity.getPersistentData().getUUID(ZONE_KEY)))) {
            entity.discard();
            removed++;
        }
        return removed;
    }

    private static final class ReworkedHandler implements Handler {
        @Override
        public void addTooltip(ConsequenceState state, boolean armor, List<Component> tooltip) {
            Handler.super.addTooltip(state, armor, tooltip);
            tooltip.add(Component.translatable("stigmata_instability.esotericism_tinker." + key(state.consequence()))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("stigmata_instability.esotericism_tinker.rules",
                    state.actionGain(), String.format(Locale.ROOT, "%.1f", state.warningTicks() / 20.0D))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        @Override
        public void onInventoryTick(ConsequenceState state, ModifierEntry modifier, Level world,
                                    LivingEntity holder, int itemSlot, boolean selected, boolean correct, ItemStack stack) {
            if (correct && !world.isClientSide) touch(state, holder);
        }

        @Override
        public void afterMeleeHit(ConsequenceState state, ModifierEntry modifier, ToolAttackContext context, float dealt) {
            if (dealt > 0) action(state, context.getAttacker(), context.getLivingTarget(),
                    context.getTarget().position());
        }

        @Override
        public void afterBlockBreak(ConsequenceState state, ModifierEntry modifier, ToolHarvestContext context) {
            action(state, context.getLiving(), null, Vec3.atCenterOf(context.getPos()));
        }

        @Override
        public void onLauncherHitEntity(ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                                         LivingEntity attacker, Entity target, @Nullable LivingEntity livingTarget, float dealt) {
            action(state, attacker, livingTarget, target.position());
        }

        @Override
        public void onLauncherHitBlock(ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                                        LivingEntity owner, BlockPos target) {
            if (state.consequence() == StigmataConsequence.JUDGEMENT
                || state.consequence() == StigmataConsequence.DEFILEMENT
                || state.consequence() == StigmataConsequence.IMMOLATION) {
                action(state, owner, null, Vec3.atCenterOf(target));
            }
        }

        @Override
        public void onArmorAttacked(ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                                     EquipmentSlot slot, DamageSource source, float amount, boolean direct) {
            if (amount > 0) action(state, context.getEntity(),
                    source.getEntity() instanceof LivingEntity attacker ? attacker : null,
                    context.getEntity().position());
        }

        @Override
        public float getMeleeDamage(ConsequenceState state, ModifierEntry modifier, ToolAttackContext context,
                                     float base, float damage) {
            if (state.consequence() == StigmataConsequence.PENANCE && context.isFullyCharged()
                && !context.getAttacker().isSprinting()) return damage * (1.04F + state.strength() * 0.02F);
            return damage * outputMultiplier(state, context.getAttacker());
        }

        @Override
        public float modifyProjectileHurt(ConsequenceState state, ModifierEntry modifier, Projectile projectile,
                                           DamageSource source, @Nullable LivingEntity attacker,
                                           LivingEntity target, float amount) {
            return attacker == null ? amount : amount * outputMultiplier(state, attacker);
        }

        @Override
        public float modifyBreakSpeed(ConsequenceState state, ModifierEntry modifier,
                                       slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext context, float speed) {
            return speed * outputMultiplier(state, context.player());
        }

        @Override
        public float getProtectionModifier(ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                                             EquipmentSlot slot, DamageSource source, float value) {
            if (!isImprint(state.consequence())) return value;
            if (satisfied(state.consequence(), context.getEntity())) return value + 0.25F * state.strength();
            return pressure(context.getEntity(), state.consequence()) < CANCEL_THRESHOLD
                   ? value : value - 0.25F * state.strength();
        }

        @Override
        public float modifyHurt(ConsequenceState state, ModifierEntry modifier, EquipmentContext context,
                                 EquipmentSlot slot, DamageSource source, float amount, boolean direct) {
            return state.consequence() == StigmataConsequence.ANATHEMA
                   && pressure(context.getEntity(), state.consequence()) >= WARNING_THRESHOLD
                   && !satisfied(state.consequence(), context.getEntity())
                   ? amount * (1 + 0.03F * state.strength()) : amount;
        }

        @Override
        public int onToolDamage(ConsequenceState state, ModifierEntry modifier, int amount,
                                 @Nullable LivingEntity holder) {
            if (state.consequence() != StigmataConsequence.OFFERING || holder == null || amount <= 0
                || holder.level().isClientSide || isApplyingConsequenceDamage()) return amount;
            action(state, holder, null, holder.position());
            CompoundTag frame = frame(holder, state.consequence());
            double credit = frame.getDouble("durability_credit") + amount * (0.2D + 0.1D * state.stage());
            int requested = (int) Math.floor(credit + 0.000001D);
            frame.putDouble("durability_credit", credit - requested);
            if (requested <= 0) return amount;
            float unit = 0.5F + 0.1F * state.strength();
            float before = holder.getHealth();
            hurt(holder, requested * unit);
            double paidCredit = frame.getDouble("blood_credit") + Math.max(0, before - holder.getHealth()) / unit;
            int paid = Math.min(amount, (int) Math.floor(paidCredit + 0.0001D));
            frame.putDouble("blood_credit", paidCredit - paid);
            return Math.max(0, amount - paid);
        }
    }
}
