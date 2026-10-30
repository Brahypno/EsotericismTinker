package org.brahypno.esotericismtinker.transcendence;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects.ConsequenceState;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataConsequence;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataOverloadDegree;
import org.brahypno.esotericismtinker.transcendence.appearance.test.StigmataTestCommands;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

@GameTestHolder("esotericism_tinker")
@PrefixGameTestTemplate(false)
public final class StigmataReworkGameTests {
    @GameTest(template = "selenic_empty")
    public static void severityUsesTheDisplayedOverloadRatio(GameTestHelper helper) {
        var slight = new ConsequenceState(StigmataConsequence.DOMINION, 11, 3, 1, 21);
        var critical = new ConsequenceState(StigmataConsequence.DOMINION, 11, 3, 21, 21);
        helper.assertTrue(slight.strength() == 1 && slight.actionGain() == 8,
                "One excess point on a burden of 21 must stay a slight consequence");
        helper.assertTrue(critical.strength() == 4 && critical.actionGain() == 14,
                "Critical overload must produce the documented action gain");
        helper.assertTrue(slight.warningTicks() == 40, "Sealing must leave two seconds to respond");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void acceptanceSamplesCoverEveryFamilyStageAndDegree(GameTestHelper helper) {
        for (StigmataConsequence family : StigmataConsequence.values()) {
            for (int stage = 1; stage <= 3; stage++) {
                for (int degree = 0; degree <= 4; degree++) {
                    ToolStack tool = ToolStack.from(StigmataTestCommands.createTestTool(family, stage, degree, "sword"));
                    StigmataData data = StigmataData.read(tool);
                    helper.assertTrue(data.consequence() == family && data.stage() == stage,
                            "Acceptance samples must retain their selected family and stage");
                    int overload = data.overload(tool);
                    helper.assertTrue(degree == 0 ? overload == 0
                            : StigmataOverloadDegree.fromValues(overload, data.burden()).ordinal() + 1 == degree,
                            "Sample overload must match its displayed degree");
                }
            }
        }
        for (String kind : new String[]{"sword", "pickaxe", "bow", "helmet"}) {
            var sample = StigmataTestCommands.createTestTool(StigmataConsequence.JUDGEMENT, 3, 2, kind);
            helper.assertTrue(!sample.isEmpty() && !ToolStack.from(sample).isBroken(),
                    "Each kit tool must be valid and usable: " + kind);
        }
        var command = helper.getLevel().getServer().getCommands().getDispatcher().getRoot()
                .getChild("esotericism_tinker").getChild("stigmata_test");
        for (String verb : new String[]{"kit", "item", "trigger", "pressure", "status", "supplies", "target", "hit", "reset", "cleanup"}) {
            helper.assertTrue(command.getChild(verb) != null, "Acceptance command missing: " + verb);
        }
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void cleaveAndMultishotShareOneActionBudget(GameTestHelper helper) {
        Zombie holder = holder(helper);
        var state = state(StigmataConsequence.JUDGEMENT, 1);
        for (int hit = 0; hit < 30; hit++) StigmataConsequenceEffects.action(state, holder, null, holder.position());
        helper.assertTrue(StigmataConsequenceEffects.pressure(holder, state.consequence()) == 8,
                "Thirty simultaneous hits must count as one family action");
        var other = state(StigmataConsequence.SACRAMENT, 1);
        StigmataConsequenceEffects.action(other, holder, null, holder.position());
        helper.assertTrue(StigmataConsequenceEffects.pressure(holder, other.consequence()) == 8,
                "Different families must retain independent budgets");
        helper.runAfterDelay(10, () -> {
            StigmataConsequenceEffects.action(state, holder, null, holder.position());
            helper.assertTrue(StigmataConsequenceEffects.pressure(holder, state.consequence()) == 16,
                    "A new action after half a second must count again");
            helper.succeed();
        });
    }

    @GameTest(template = "selenic_empty", timeoutTicks = 100)
    public static void warningsPrecedeCloudsAndThenEnterCooldown(GameTestHelper helper) {
        Zombie holder = holder(helper);
        var state = state(StigmataConsequence.EXALTATION, 2);
        StigmataConsequenceEffects.forceWarning(state, holder, holder.position(), null);
        helper.onEachTick(() -> tick(holder, state));
        helper.runAfterDelay(35, () -> helper.assertTrue(helper.getLevel().getEntitiesOfClass(AreaEffectCloud.class,
                holder.getBoundingBox().inflate(3)).isEmpty(), "A cloud must not appear before its warning expires"));
        helper.runAfterDelay(55, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(AreaEffectCloud.class,
                    holder.getBoundingBox().inflate(3)).size() == 1, "The expired warning must create one cloud");
            long cooldown = frame(holder, state).getLong("cooldown");
            helper.assertTrue(cooldown > helper.getLevel().getGameTime(), "Manifestation must enter cooldown");
            StigmataConsequenceEffects.setPressure(holder, state.consequence(), 100);
            StigmataConsequenceEffects.action(state, holder, null, holder.position());
            helper.assertTrue(!frame(holder, state).contains("due"), "New actions must respect manifestation cooldown");
            StigmataConsequenceEffects.clear(holder);
            helper.succeed();
        });
    }

    @GameTest(template = "selenic_empty", timeoutTicks = 80)
    public static void groundingAndCrouchingCanCancelSealing(GameTestHelper helper) {
        Zombie holder = holder(helper);
        holder.setOnGround(true);
        holder.setPose(Pose.CROUCHING);
        var state = state(StigmataConsequence.EXALTATION, 3);
        StigmataConsequenceEffects.forceWarning(state, holder, holder.position(), null);
        helper.onEachTick(() -> tick(holder, state));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(!frame(holder, state).contains("due"), "Grounding must cancel the warning");
            helper.assertTrue(StigmataConsequenceEffects.pressure(holder, state.consequence()) < 40,
                    "The remedy must bring pressure below the cancellation threshold");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(AreaEffectCloud.class,
                    holder.getBoundingBox().inflate(3)).isEmpty(), "Canceled warnings must not manifest later");
            helper.succeed();
        });
    }

    @GameTest(template = "selenic_empty", timeoutTicks = 60)
    public static void unequippingDecaysPressureWithoutResettingIt(GameTestHelper helper) {
        Zombie holder = holder(helper);
        var state = state(StigmataConsequence.JUDGEMENT, 3);
        StigmataConsequenceEffects.touch(state, holder);
        StigmataConsequenceEffects.setPressure(holder, state.consequence(), 60);
        helper.onEachTick(() -> StigmataConsequenceEffects.onLivingTick(new LivingEvent.LivingTickEvent(holder)));
        helper.runAfterDelay(5, () -> helper.assertTrue(StigmataConsequenceEffects.pressure(holder, state.consequence()) > 0,
                "Switching away must not instantly clear pressure"));
        helper.runAfterDelay(45, () -> {
            int remaining = StigmataConsequenceEffects.pressure(holder, state.consequence());
            helper.assertTrue(remaining > 0 && remaining <= 40, "An unequipped family must gradually settle");
            helper.succeed();
        });
    }

    @GameTest(template = "selenic_empty")
    public static void emptyExperienceStillBuildsDebtAndPickupRepaysIt(GameTestHelper helper) {
        Player holder = helper.makeMockPlayer();
        holder.totalExperience = 0;
        var state = state(StigmataConsequence.SACRAMENT, 1);
        StigmataConsequenceEffects.action(state, holder, null, holder.position());
        helper.assertTrue(StigmataConsequenceEffects.pressure(holder, state.consequence()) == 20,
                "Having no experience must add debt instead of bypassing the price");
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), holder.getX(), holder.getY(), holder.getZ(), 5);
        StigmataConsequenceEffects.onExperiencePickup(new PlayerXpEvent.PickupXp(holder, orb));
        helper.assertTrue(StigmataConsequenceEffects.pressure(holder, state.consequence()) == 0,
                "Five collected XP points must repay twenty pressure");
        Player spent = helper.makeMockPlayer();
        spent.totalExperience = 1000;
        spent.experienceLevel = 0;
        spent.experienceProgress = 0;
        StigmataConsequenceEffects.action(state, spent, null, spent.position());
        helper.assertTrue(StigmataConsequenceEffects.pressure(spent, state.consequence()) == 20,
                "An obsolete total-experience counter must not hide a debt after enchanting");
        Player leveled = helper.makeMockPlayer();
        leveled.totalExperience = 0;
        leveled.experienceLevel = 5;
        StigmataConsequenceEffects.action(state, leveled, null, leveled.position());
        helper.assertTrue(StigmataConsequenceEffects.pressure(leveled, state.consequence()) == 8,
                "A player with real levels must be able to pay even if the counter is zero");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void offeringDoesNotRefundInvulnerableDamage(GameTestHelper helper) {
        Player holder = helper.makeMockPlayer();
        holder.getAbilities().invulnerable = true;
        var state = state(StigmataConsequence.OFFERING, 3);
        var tool = ToolStack.from(StigmataTestCommands.createTestTool(state.consequence(), 3, 1, "sword"));
        var modifier = tool.getModifier(EsotericismTinkerModifiers.STIGMATA);
        int amount = modifier.getHook(ModifierHooks.TOOL_DAMAGE).onDamageTool(tool, modifier, 10, holder);
        helper.assertTrue(amount == 10 && holder.getHealth() == holder.getMaxHealth(),
                "Unpaid blood must leave the original durability loss intact");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty", timeoutTicks = 130)
    public static void obsessionHasACapAndAFiniteLifetime(GameTestHelper helper) {
        Zombie holder = holder(helper);
        holder.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        holder.setHealth(1000);
        holder.setNoGravity(true);
        helper.getLevel().addFreshEntity(holder);
        var state = state(StigmataConsequence.OBSESSION, 3);
        StigmataConsequenceEffects.forceWarning(state, holder, holder.position(), null);
        helper.onEachTick(() -> tick(holder, state));
        helper.runAfterDelay(45, () -> {
            var vexes = helper.getLevel().getEntitiesOfClass(Vex.class, holder.getBoundingBox().inflate(10));
            helper.assertTrue(vexes.size() == 1, "Obsession must create one vex");
            vexes.get(0).setNoAi(true);
            vexes.get(0).setTarget(null);
            StigmataConsequenceEffects.onLivingTick(new LivingEvent.LivingTickEvent(vexes.get(0)));
            helper.assertTrue(vexes.get(0).getTarget() == holder, "Summoned vexes must retain their assigned prey");
            StigmataConsequenceEffects.forceWarning(state, holder, holder.position(), null);
        });
        helper.runAfterDelay(90, () -> {
            var vexes = helper.getLevel().getEntitiesOfClass(Vex.class, holder.getBoundingBox().inflate(10));
            helper.assertTrue(vexes.size() == 1, "Repeated manifestations must respect the owner's nearby cap");
            Vex vex = vexes.get(0);
            long expires = vex.getPersistentData().getLong("expires");
            helper.assertTrue(expires > helper.getLevel().getGameTime()
                    && expires <= helper.getLevel().getGameTime() + 400, "A vex must have a finite twenty-second lease");
            vex.getPersistentData().putLong("expires", helper.getLevel().getGameTime());
            StigmataConsequenceEffects.onLivingTick(new LivingEvent.LivingTickEvent(vex));
            helper.assertTrue(vex.isRemoved(), "Expired summons must be discarded");
            StigmataConsequenceEffects.clear(holder);
            helper.succeed();
        });
    }

    @GameTest(template = "selenic_empty", timeoutTicks = 80)
    public static void defilementLeavesTerrainIntactAndResetRemovesZones(GameTestHelper helper) {
        Zombie holder = holder(helper);
        BlockPos floor = holder.blockPosition().below();
        helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        var state = state(StigmataConsequence.DEFILEMENT, 3);
        StigmataConsequenceEffects.forceWarning(state, holder, holder.position(), null);
        helper.onEachTick(() -> tick(holder, state));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(floor).is(Blocks.STONE),
                    "Defilement must never replace the ground with sculk");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(AreaEffectCloud.class,
                    holder.getBoundingBox().inflate(3)).isEmpty(), "Defilement must create its temporary zone");
            StigmataConsequenceEffects.clear(holder);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(AreaEffectCloud.class,
                    holder.getBoundingBox().inflate(3)).isEmpty(), "Reset must remove owned zones immediately");
            helper.succeed();
        });
    }

    @GameTest(template = "selenic_empty", timeoutTicks = 80)
    public static void parasitesHatchOutsideImmediateContactAndExpire(GameTestHelper helper) {
        Zombie holder = holder(helper);
        holder.setNoGravity(true);
        helper.getLevel().addFreshEntity(holder);
        var state = state(StigmataConsequence.INCUBATION, 3);
        StigmataConsequenceEffects.forceWarning(state, holder, holder.position(), null);
        helper.onEachTick(() -> tick(holder, state));
        helper.runAfterDelay(45, () -> {
            var brood = helper.getLevel().getEntitiesOfClass(Silverfish.class, holder.getBoundingBox().inflate(5));
            helper.assertTrue(brood.size() == 2, "Both parasites must be visible before making contact");
            for (Silverfish parasite : brood) {
                helper.assertTrue(parasite.isNoAi(), "Acceptance parasites must not infest or wake ordinary silverfish");
                long expires = parasite.getPersistentData().getLong("expires");
                helper.assertTrue(expires > helper.getLevel().getGameTime()
                        && expires <= helper.getLevel().getGameTime() + 300, "Parasites must expire within fifteen seconds");
                parasite.getPersistentData().putLong("expires", helper.getLevel().getGameTime());
                StigmataConsequenceEffects.onLivingTick(new LivingEvent.LivingTickEvent(parasite));
                helper.assertTrue(parasite.isRemoved(), "Expired parasites must be discarded");
            }
            StigmataConsequenceEffects.clear(holder);
            helper.succeed();
        });
    }

    private static Zombie holder(GameTestHelper helper) {
        Zombie holder = new Zombie(helper.getLevel());
        holder.setNoAi(true);
        holder.setPos(helper.absoluteVec(new Vec3(8, 2, 8)));
        return holder;
    }

    private static ConsequenceState state(StigmataConsequence family, int stage) {
        return new ConsequenceState(family, family.ordinal(), stage, 1, 4);
    }

    private static net.minecraft.nbt.CompoundTag frame(LivingEntity holder, ConsequenceState state) {
        return holder.getPersistentData().getCompound(StigmataConsequenceEffects.RUNTIME_KEY)
                .getCompound(state.consequence().name().toLowerCase(java.util.Locale.ROOT));
    }

    private static void tick(LivingEntity holder, ConsequenceState state) {
        StigmataConsequenceEffects.touch(state, holder);
        StigmataConsequenceEffects.onLivingTick(new LivingEvent.LivingTickEvent(holder));
    }
}
