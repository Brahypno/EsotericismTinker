package org.brahypno.esotericismtinker.library.modifiers.modules.transcendence;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.brahypno.esotericismtinker.transcendence.intrinsic.*;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.*;
import slimeknights.tconstruct.library.modifiers.hook.armor.DamageBlockModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.*;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DisplayNameModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockHarvestModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Applies stored Noumenon allocation, including grouped Elevation paths.
 *
 * TConstruct compatibility:
 * The project currently pins 3.11.2.165. In that release SweepWeaponAttack reads
 * tconstruct:expanded and tconstruct:sweeping_edge, while projectile piercing is
 * tconstruct:impaling. Newer 1.20.1 source branches expose different APIs; verify the
 * locally resolved dependency before changing these IDs.
 */
public record NoumenonModule(
        ModifierCondition<IToolContext> condition
) implements ModifierModule, ConditionalModule<IToolContext>,
        VolatileDataModifierHook, ModifierTraitHook, ValidateModifierHook,
        ModifierRemovalHook, TooltipModifierHook, DisplayNameModifierHook,
        ToolStatsModifierHook, DamageBlockModifierHook, OnAttackedModifierHook,
        MeleeDamageModifierHook, MeleeHitModifierHook, ToolDamageModifierHook,
        BlockHarvestModifierHook.MarkHarvesting {

    private static final ModifierId TCON_EXPANDED =
            new ModifierId(new ResourceLocation("tconstruct", "expanded"));
    private static final ModifierId TCON_SWEEPING =
            new ModifierId(new ResourceLocation("tconstruct", "sweeping_edge"));
    private static final ModifierId TCON_MULTISHOT =
            new ModifierId(new ResourceLocation("tconstruct", "multishot"));
    private static final ModifierId TCON_PIERCING =
            new ModifierId(new ResourceLocation("tconstruct", "impaling"));

    private static final List<ModuleHook<?>> DEFAULT_HOOKS =
            HookProvider.<NoumenonModule>defaultHooks(
                    ModifierHooks.VOLATILE_DATA, ModifierHooks.MODIFIER_TRAITS,
                    ModifierHooks.VALIDATE, ModifierHooks.REMOVE,
                    ModifierHooks.TOOLTIP, ModifierHooks.DISPLAY_NAME,
                    ModifierHooks.TOOL_STATS, ModifierHooks.DAMAGE_BLOCK,
                    ModifierHooks.ON_ATTACKED, ModifierHooks.MELEE_DAMAGE,
                    ModifierHooks.MELEE_HIT, ModifierHooks.TOOL_DAMAGE,
                    ModifierHooks.BLOCK_HARVEST
            );

    public static final RecordLoadable<NoumenonModule> LOADER = RecordLoadable.create(
            ModifierCondition.CONTEXT_FIELD, NoumenonModule::new);
    public static final NoumenonModule INSTANCE =
            new NoumenonModule(ModifierCondition.ANY_CONTEXT);

    @Override
    public Component getDisplayName(IToolStackView tool, ModifierEntry entry,
                                    Component name, @Nullable RegistryAccess access) {
        int storedLevel = tool.getPersistentData().getInt(NoumenonKeys.LEVEL);
        int level = storedLevel == 0 ? entry.getLevel() : storedLevel;
        String key = entry.getModifier().getTranslationKey();
        return name.copy().withStyle(style ->
                style.withColor(ResourceColorManager.getTextColor(key + "." + level)));
    }

    @Override public RecordLoadable<NoumenonModule> getLoader() { return LOADER; }
    @Override public List<ModuleHook<?>> getDefaultHooks() { return DEFAULT_HOOKS; }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
        if (!condition.matches(context, modifier)) return;
        NoumenonData data = NoumenonData.read(context);
        for (Map.Entry<String,Integer> chosen : data.receptionSlots.entrySet()) {
            SlotType type = SlotType.getIfPresent(chosen.getKey());
            if (type != null) volatileData.addSlots(type, chosen.getValue());
        }
        volatileData.putInt(NoumenonKeys.REJECTION, NoumenonLogic.computeRejection(context, data));
    }

    @Override
    public void addTraits(IToolContext context, ModifierEntry modifier,
                          TraitBuilder builder, boolean firstEncounter) {
        if (!condition.matches(context, modifier)) return;
        NoumenonData data = NoumenonData.read(context);

        if (data.hasInvestitureSnapshot()) {
            for (Map.Entry<ResourceLocation,Integer> entry : data.investedTraits.entrySet()) {
                ModifierId id = new ModifierId(entry.getKey());
                if (entry.getValue() > 0 && ModifierManager.INSTANCE.contains(id)) {
                    builder.add(new ModifierEntry(id, entry.getValue()));
                }
            }
        }

        for (Map.Entry<ResourceLocation,Integer> chosen : data.sublimations.entrySet()) {
            NoumenonDatabase.sublimation(chosen.getKey()).ifPresent(entry ->
                    entry.apply(context, modifier, chosen.getValue(), builder));
        }

        addExisting(builder, TCON_EXPANDED, level(data, "sword_range"));
        addExisting(builder, TCON_SWEEPING, level(data, "sword_sweep_damage"));
        addExisting(builder, TCON_MULTISHOT, everyOtherLevel(level(data, "bow_multishot")));
        addExisting(builder, TCON_PIERCING, everyOtherLevel(level(data, "bow_piercing")));
    }

    private static void addExisting(TraitBuilder builder, ModifierId id, int level) {
        if (level > 0 && ModifierManager.INSTANCE.contains(id)) {
            builder.add(new ModifierEntry(id, level));
        }
    }

    private static int everyOtherLevel(int level) {
        return level <= 0 ? 0 : (level + 1) / 2;
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        if (!condition.matches(context, modifier)) return;
        NoumenonData data = NoumenonData.read(context);

        multiply(context, builder, ToolStats.ATTACK_DAMAGE, level(data, "melee_damage"), 0.06f);
        multiply(context, builder, ToolStats.ATTACK_SPEED, level(data, "melee_speed"), 0.05f);
        multiply(context, builder, ToolStats.DRAW_SPEED, level(data, "ranged_draw_speed"), 0.06f);
        multiply(context, builder, ToolStats.VELOCITY, level(data, "ranged_ballistics"), 0.05f);
        multiply(context, builder, ToolStats.MINING_SPEED, level(data, "harvest_speed"), 0.08f);
        multiply(context, builder, ToolStats.BLOCK_AMOUNT, level(data, "shield_guard"), 0.50f);

        add(context, builder, ToolStats.ARMOR_TOUGHNESS, level(data, "armor_toughness"), 2.0f);
        add(context, builder, ToolStats.KNOCKBACK_RESISTANCE, level(data, "armor_stability"), 0.10f);
    }

    private static void multiply(IToolContext context, ModifierStatsBuilder builder,
                                 INumericToolStat<?> stat, int level, float perLevel) {
        if (level > 0 && stat.supports(context.getItem())) {
            stat.multiply(builder, 1.0f + level * perLevel);
        }
    }

    private static void add(IToolContext context, ModifierStatsBuilder builder,
                            INumericToolStat<?> stat, int level, float perLevel) {
        if (level > 0 && stat.supports(context.getItem())) {
            stat.add(builder, level * perLevel);
        }
    }

    @Override
    public boolean isDamageBlocked(IToolStackView tool, ModifierEntry modifier,
                                   EquipmentContext context, EquipmentSlot slotType,
                                   DamageSource source, float amount) {
        if (!condition.matches(tool, modifier)) return false;
        int level = level(tool, "armor_threshold");
        return level > 0 && amount < (1.0f + 0.75f * level);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier,
                           EquipmentContext context, EquipmentSlot slotType,
                           DamageSource source, float amount, boolean isDirectDamage) {
        if (!condition.matches(tool, modifier)) return;
        int level = level(tool, "shield_counter");
        if (level <= 0 || !isDirectDamage || !context.getEntity().isBlocking()) return;

        if (source.getEntity() instanceof LivingEntity attacker
                && attacker != context.getEntity()) {
            attacker.hurt(context.getEntity().damageSources().thorns(context.getEntity()),
                    2.0f * level);
        }
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier,
                                ToolAttackContext context, float baseDamage, float damage) {
        if (!condition.matches(tool, modifier)) return damage;
        CompoundTag sublimations = tool.getPersistentData().getCompound(NoumenonKeys.SUBLIMATIONS);

        int reap = level(sublimations, "scythe_reap");
        if (reap > 0 && context.isExtraAttack()) damage *= 1.0f + 0.10f * reap;

        LivingEntity target = context.getLivingTarget();
        int execute = level(sublimations, "axe_execute");
        if (execute > 0 && target != null && target.getMaxHealth() > 0
                && target.getHealth() / target.getMaxHealth() <= 0.30f) {
            damage *= 1.0f + 0.10f * execute;
        }

        int heavy = level(sublimations, "axe_heavy");
        if (heavy > 0 && context.isFullyCharged()) damage *= 1.0f + 0.07f * heavy;

        int crush = level(sublimations, "hammer_crush");
        if (crush > 0 && target != null) {
            damage += (float) target.getAttributeValue(Attributes.ARMOR) * 0.05f * crush;
        }
        return damage;
    }

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier,
                                ToolAttackContext context, float damage,
                                float baseKnockback, float knockback) {
        if (!condition.matches(tool, modifier)) return knockback;
        int level = level(tool, "hammer_knockback");
        return level <= 0 ? knockback : knockback + 0.3f * level;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier,
                              ToolAttackContext context, float damageDealt) {
        if (!condition.matches(tool, modifier) || !context.isExtraAttack()) return;
        int level = level(tool, "scythe_sustain");
        if (level > 0 && damageDealt > 0) {
            context.getAttacker().heal(damageDealt * 0.015f * level);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int onDamageTool(IToolStackView tool, ModifierEntry modifier,
                            int amount, @Nullable LivingEntity holder) {
        if (!condition.matches(tool, modifier)) return amount;
        int level = level(tool, "harvest_endurance");
        if (level <= 0 || !BlockHarvestModifierHook.MarkHarvesting.isHarvesting(tool)) return amount;

        float expected = amount * Math.max(0.15f, 1.0f - 0.10f * level);
        int result = (int)Math.floor(expected);
        float fraction = expected - result;
        if (fraction > 0 && holder != null && holder.getRandom().nextFloat() < fraction) result++;
        return result;
    }

    @Nullable
    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        if (!condition.matches(tool, modifier)) return null;
        NoumenonData data = NoumenonData.read(tool);
        if (!data.isValid()) {
            return Component.translatable("modifier.esotericism_tinker.noumenon_core.invalid_points");
        }
        return NoumenonSublimationLogic.validateSelections(tool, data);
    }

    @Override
    public Component onRemoved(IToolStackView tool, Modifier modifier) {
        return Component.translatable("modifier.esotericism_tinker.noumenon_core.cannot_remove");
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier,
                           @Nullable Player player, List<Component> tooltip,
                           TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (!condition.matches(tool, modifier) || !tooltipKey.isShiftOrUnknown()) return;
        NoumenonData data = NoumenonData.read(tool);

        for (Map.Entry<String,Integer> chosen : data.receptionSlots.entrySet()) {
            SlotType type = SlotType.getIfPresent(chosen.getKey());
            if (type != null) {
                tooltip.add(Component.literal(chosen.getValue() + " × ")
                        .append(type.getDisplayName()));
            }
        }
        for (Map.Entry<ResourceLocation,Integer> chosen : data.sublimations.entrySet()) {
            if (chosen.getValue() <= 0) continue;
            NoumenonDatabase.sublimation(chosen.getKey()).ifPresent(entry ->
                    tooltip.add(Component.literal(chosen.getValue() + " × ")
                            .append(entry.display().name())));
        }
    }

    private static int level(NoumenonData data, String path) {
        return Math.max(0, data.sublimations.getOrDefault(NoumenonKeys.id(path), 0));
    }

    private static int level(IToolContext tool, String path) {
        return level(tool.getPersistentData().getCompound(NoumenonKeys.SUBLIMATIONS), path);
    }

    private static int level(CompoundTag sublimations, String path) {
        return Math.max(0, sublimations.getInt(NoumenonKeys.MOD_ID + ":" + path));
    }
}
