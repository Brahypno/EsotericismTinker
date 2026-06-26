package org.brahypno.esotericismtinker.utils.LootHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.brahypno.esotericismtinker.EsotericismTinker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.brahypno.esotericismtinker.utils.LootHelper.LootTableItemScanner.getAllPossibleLootStacksGeneral;
import static org.brahypno.esotericismtinker.utils.LootHelper.LootTableItemScanner.getAllScannedLootStacksMinOne;
import static org.brahypno.esotericismtinker.utils.MethodHandler.findMethod;
import static org.brahypno.esotericismtinker.utils.MethodHandler.findSpecial;

public class LootResolver {

    private static volatile Method DROP_ALL_DEATH_LOOT;
    private static MethodHandle DROP_FROM_LOOT_TABLE;
    private static MethodHandle DROP_CUSTOM_DEATH_LOOT;
    private static MethodHandle DROP_EQUIPMENT;
    private static MethodHandle DROP_EXPERIENCE;


    public static void dropAllDeathLootVanilla(LivingEntity victim, DamageSource source) {
        Entity attacker = source.getEntity();
        int looting = net.minecraftforge.common.ForgeHooks.getLootingLevel(victim, attacker, source);
        if (attacker instanceof Player player)
            victim.setLastHurtByPlayer(player);
        else if (attacker instanceof LivingEntity livingAttacker)
            victim.setLastHurtByMob(livingAttacker);

        Collection<ItemEntity> drops = null;
        List<Throwable> errors = new ArrayList<>();

        victim.captureDrops(new ArrayList<>());

        Collection<ItemEntity> all_drops = new ArrayList<>();
        try {
            if (victim.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)){
                try {
                    invokeLivingDropFromLootTable(victim, source, true);
                }
                catch (Throwable t) {
                    errors.add(new RuntimeException("dropFromLootTable failed", t));
                }

                try {
                    invokeLivingDropCustomDeathLoot(victim, source, looting, true);
                }
                catch (Throwable t) {
                    errors.add(new RuntimeException("dropCustomDeathLoot failed", t));
                }
            }

            try {
                dropAllEquipmentLikeDeath(victim);
            }
            catch (Throwable t) {
                errors.add(new RuntimeException("dropEquipment failed", t));
            }

            try {
                if (attacker instanceof ServerPlayer player)
                    giveDropExperienceToPlayer(victim, player);
                invokeLivingDropExperience(victim);
            }
            catch (Throwable t) {
                errors.add(new RuntimeException("dropExperience failed", t));
            }
        }
        finally {
            try {
                drops = victim.captureDrops(null);
                if ((drops == null || drops.isEmpty()) && victim.level() instanceof ServerLevel level){
                    try {
                        List<ItemStack> forcedStacks =
                                getAllPossibleLootStacksGeneral(level, victim, tableId -> getAllScannedLootStacksMinOne(level, tableId, looting));
                        for (ItemStack stack : forcedStacks) {
                            if (stack.isEmpty())
                                continue;

                            all_drops.add(new ItemEntity(
                                    level,
                                    victim.getX(),
                                    victim.getY(),
                                    victim.getZ(),
                                    stack
                            ));
                        }

                        //Esotericismtinker.LOGGER.info("Forced scanner loot fallback for {} : {} stacks", victim.getType(), forcedStacks.size());
                    }
                    catch (Throwable t) {
                        errors.add(new RuntimeException("scanner fallback loot failed", t));
                    }
                }
            }
            catch (Throwable t) {
                errors.add(new RuntimeException("captureDrops(null) failed", t));
            }
        }


        try {
            net.minecraftforge.common.ForgeHooks.onLivingDrops(victim, source, all_drops, looting, true);
        }
        catch (Throwable t) {
            errors.add(new RuntimeException("onLivingDrops failed", t));
        }
        if (drops != null)
            all_drops.addAll(drops);


        for (ItemEntity drop : all_drops) {
            try {
                victim.level().addFreshEntity(drop);
            }
            catch (Throwable t) {
                errors.add(new RuntimeException("addFreshEntity failed for " + drop.getItem(), t));
            }
        }

        if (!errors.isEmpty()){
            EsotericismTinker.LOGGER.warn(
                    "dropAllDeathLootVanillaSafe had {} error(s) for {}",
                    errors.size(),
                    victim.getType()
            );
            for (Throwable error : errors) {
                EsotericismTinker.LOGGER.warn("Suppressed forced loot error", error);
            }
        }
    }

    public static void invokeDropAllDeathLoot(LivingEntity entity, DamageSource source) {
        try {
            Method m = DROP_ALL_DEATH_LOOT;
            if (m == null){
                m = findMethod(
                        LivingEntity.class,
                        // 1) 首选：你当前映射下的名字（official/parchment）
                        "dropAllDeathLoot",
                        // 2) 备用：如果你确实在旧版，可把 SRG/obf 名字放这里
                        "m_6668_",  // 示例：自己填你查到的别名
                        new Class<?>[]{DamageSource.class},
                        void.class,
                        /* mustBeInstance */ true
                );
                m.setAccessible(true); // 允许访问 protected/private
                DROP_ALL_DEATH_LOOT = m;
            }
            m.invoke(entity, source);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke dropAllDeathLoot via reflection", e);
        }
    }

    public static void invokeLivingDropFromLootTable(LivingEntity entity, DamageSource source, boolean causedByPlayer) {
        try {
            MethodHandle mh = DROP_FROM_LOOT_TABLE;
            if (mh == null){
                mh = findSpecial(
                        LivingEntity.class,
                        new String[]{"dropFromLootTable", "m_7625_"},
                        MethodType.methodType(void.class, DamageSource.class, boolean.class)
                );
                DROP_FROM_LOOT_TABLE = mh;
            }

            mh.invokeExact(entity, source, causedByPlayer);
            //Esotericismtinker.LOGGER.info("Invoking LivingEntity#dropFromLootTable directly for {}", entity.getType());
        }
        catch (Throwable e) {
            throw new RuntimeException("Failed to invokespecial LivingEntity#dropFromLootTable", e);
        }
    }

    public static void invokeLivingDropCustomDeathLoot(LivingEntity entity, DamageSource source, int lootingLevel, boolean causedByPlayer) {
        try {
            MethodHandle mh = DROP_CUSTOM_DEATH_LOOT;
            if (mh == null){
                mh = findSpecial(
                        LivingEntity.class,
                        new String[]{"dropCustomDeathLoot", "m_7472_"},
                        MethodType.methodType(void.class, DamageSource.class, int.class, boolean.class)
                );
                DROP_CUSTOM_DEATH_LOOT = mh;
            }

            mh.invokeExact(entity, source, lootingLevel, causedByPlayer);
        }
        catch (Throwable e) {
            throw new RuntimeException("Failed to invokespecial LivingEntity#dropCustomDeathLoot", e);
        }
    }

    public static void invokeLivingDropEquipment(LivingEntity entity) {
        try {
            MethodHandle mh = DROP_EQUIPMENT;
            if (mh == null){
                mh = findSpecial(
                        LivingEntity.class,
                        new String[]{"dropEquipment", "m_5907_"},
                        MethodType.methodType(void.class)
                );
                DROP_EQUIPMENT = mh;
            }

            mh.invokeExact(entity);
        }
        catch (Throwable e) {
            throw new RuntimeException("Failed to invokespecial LivingEntity#dropEquipment", e);
        }
    }

    private static volatile MethodHandle EXPERIENCE_REWARD;

    public static void giveDropExperienceToPlayer(LivingEntity entity, ServerPlayer player) {
        try {
            if (entity.level().isClientSide){
                return;
            }

            int xp = invokeGetExperienceReward(entity);
            if (xp <= 0){
                return;
            }

            player.giveExperiencePoints(xp);
        }
        catch (Throwable e) {
            throw new RuntimeException("Failed to give LivingEntity experience directly", e);
        }
    }

    private static int invokeGetExperienceReward(LivingEntity entity) throws Throwable {
        MethodHandle mh = EXPERIENCE_REWARD;
        if (mh == null){
            mh = findSpecial(
                    LivingEntity.class,
                    new String[]{"getExperienceReward", "m_6552_"}, // obf 名需要按你当前 mappings 校准
                    MethodType.methodType(int.class)
            );
            EXPERIENCE_REWARD = mh;
        }

        return (int) mh.invoke(entity);
    }

    public static void invokeLivingDropExperience(LivingEntity entity) {
        try {
            MethodHandle mh = DROP_EXPERIENCE;
            if (mh == null){
                mh = findSpecial(
                        LivingEntity.class,
                        new String[]{"dropExperience", "m_21226_"},
                        MethodType.methodType(void.class)
                );
                DROP_EXPERIENCE = mh;
            }

            mh.invokeExact(entity);
        }
        catch (Throwable e) {
            throw new RuntimeException("Failed to invokespecial LivingEntity#dropExperience", e);
        }
    }

    public static void dropAllEquipmentLikeDeath(LivingEntity e) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = e.getItemBySlot(slot);
            if (stack.isEmpty())
                continue;

            // 可选：尊重“消失诅咒”，死亡会直接消失，这里仿真
            if (net.minecraft.world.item.enchantment.EnchantmentHelper.hasVanishingCurse(stack)){
                e.setItemSlot(slot, ItemStack.EMPTY);
                continue;
            }

            ItemEntity drop = e.spawnAtLocation(stack.copy(), 0.5f);
            if (drop != null){
                drop.setDefaultPickUpDelay();
                drop.setDeltaMovement(drop.getDeltaMovement().add(0.0, 0.2, 0.0));
            }
            e.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

}
