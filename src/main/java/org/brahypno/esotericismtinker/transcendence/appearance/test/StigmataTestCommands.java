package org.brahypno.esotericismtinker.transcendence.appearance.test;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects;
import org.brahypno.esotericismtinker.library.modifiers.modules.transcendence.StigmataConsequenceEffects.ConsequenceState;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataConsequence;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataEntry;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataStage;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Temporary acceptance fixtures. No recipes, creative-tab entries, or new item registry IDs. */
@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID)
public final class StigmataTestCommands {
    public static final String DUMMY_OWNER = "esotericism_tinker_stigmata_dummy_owner";
    private static final List<String> KINDS = List.of("sword", "pickaxe", "bow", "helmet");

    private StigmataTestCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(EsotericismTinker.MODID)
                .then(Commands.literal("stigmata_test").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("kit").then(familyArgument()
                                .then(Commands.argument("stage", IntegerArgumentType.integer(1, 3))
                                        .then(Commands.argument("degree", IntegerArgumentType.integer(0, 4))
                                                .executes(ctx -> kit(ctx.getSource(), family(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "family")),
                                                        IntegerArgumentType.getInteger(ctx, "stage"),
                                                        IntegerArgumentType.getInteger(ctx, "degree")))))))
                        .then(Commands.literal("item").then(familyArgument()
                                .then(Commands.argument("stage", IntegerArgumentType.integer(1, 3))
                                        .then(Commands.argument("degree", IntegerArgumentType.integer(0, 4))
                                                .then(Commands.argument("kind", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(KINDS, builder))
                                                        .executes(ctx -> item(ctx.getSource(), family(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "family")),
                                                                IntegerArgumentType.getInteger(ctx, "stage"),
                                                                IntegerArgumentType.getInteger(ctx, "degree"),
                                                                StringArgumentType.getString(ctx, "kind"))))))))
                        .then(Commands.literal("pressure").then(familyArgument()
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> pressure(ctx.getSource(), family(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "family")),
                                                IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("trigger").then(familyArgument()
                                .executes(ctx -> trigger(ctx.getSource(), family(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "family"))))))
                        .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                        .then(Commands.literal("supplies").executes(ctx -> supplies(ctx.getSource())))
                        .then(Commands.literal("target").executes(ctx -> target(ctx.getSource())))
                        .then(Commands.literal("hit").executes(ctx -> hit(ctx.getSource())))
                        .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource(), false)))
                        .then(Commands.literal("cleanup").executes(ctx -> reset(ctx.getSource(), true)))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> familyArgument() {
        return Commands.argument("family", StringArgumentType.word()).suggests((ctx, builder) ->
                SharedSuggestionProvider.suggest(Arrays.stream(StigmataConsequence.values())
                        .map(value -> value.name().toLowerCase(Locale.ROOT)), builder));
    }

    @Nullable
    private static StigmataConsequence family(CommandSourceStack source, String value) {
        try {
            return StigmataConsequence.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.translatable("command.esotericism_tinker.stigmata_test.unknown", value));
            return null;
        }
    }

    public static ItemStack createTestTool(StigmataConsequence family, int stage, int degree, String kind) {
        String path = switch (kind) {
            case "sword" -> "sword";
            case "pickaxe" -> "pickaxe";
            case "bow" -> "longbow";
            case "helmet" -> "travelers_helmet";
            default -> throw new IllegalArgumentException("Unknown test tool kind " + kind);
        };
        IModifiable item = (IModifiable) BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct", path));
        ItemStack stack = ToolBuildHandler.createSingleMaterial(item, MaterialVariant.of(MaterialRegistry.getMaterial(
                new MaterialId(kind.equals("helmet") ? "tconstruct:rose_gold" : "tconstruct:wood"))));
        if (stack.isEmpty()) throw new IllegalStateException("No valid materials for " + path);
        ToolStack tool = ToolStack.from(stack);
        CompoundTag tag = new CompoundTag();
        tag.putInt("consequence_seed", family.ordinal());
        StigmataData data = StigmataData.deserialize(tag);
        for (StigmataStage chosen : StigmataStage.values()) {
            if (chosen.index() > stage) break;
            data.set(chosen, new StigmataEntry(new ResourceLocation("tconstruct", "small_blade"),
                    new MaterialId("tconstruct:manyullyn"), new MaterialStatsId("tconstruct:head")));
        }
        data.write(tool);
        tool.getPersistentData().putInt(StigmataData.TEST_OVERLOAD, (int) Math.ceil(data.burden() * degree / 4.0D));
        tool.addModifier(EsotericismTinkerModifiers.STIGMATA, stage);
        stack = tool.createStack();
        stack.setHoverName(Component.translatable("command.esotericism_tinker.stigmata_test.item_name",
                family.displayName(), StigmataStage.byIndex(stage).index(), degree, kind).withStyle(ChatFormatting.LIGHT_PURPLE));
        return stack;
    }

    private static int kit(CommandSourceStack source, @Nullable StigmataConsequence family, int stage, int degree)
            throws CommandSyntaxException {
        if (family == null) return 0;
        ServerPlayer player = source.getPlayerOrException();
        for (String kind : KINDS) give(player, createTestTool(family, stage, degree, kind));
        source.sendSuccess(() -> Component.translatable("command.esotericism_tinker.stigmata_test.kit",
                family.displayName(), stage, degree), false);
        return 4;
    }

    private static int item(CommandSourceStack source, @Nullable StigmataConsequence family, int stage, int degree,
                            String kind) throws CommandSyntaxException {
        if (family == null) return 0;
        if (!KINDS.contains(kind)) {
            source.sendFailure(Component.translatable("command.esotericism_tinker.stigmata_test.unknown", kind));
            return 0;
        }
        give(source.getPlayerOrException(), createTestTool(family, stage, degree, kind));
        return 1;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    @Nullable
    private static ConsequenceState equipped(ServerPlayer player, StigmataConsequence family) {
        EquipmentContext context = new EquipmentContext(player);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            var tool = context.getValidTool(slot);
            if (tool == null || tool.isBroken() || tool.getModifierLevel(EsotericismTinkerModifiers.STIGMATA) <= 0) continue;
            StigmataData data = StigmataData.read(tool);
            if (data.hasConsequenceSeed() && data.consequence() == family && data.overload(tool) > 0) {
                return ConsequenceState.of(tool, data);
            }
        }
        return null;
    }

    private static int pressure(CommandSourceStack source, @Nullable StigmataConsequence family, int value)
            throws CommandSyntaxException {
        if (family == null) return 0;
        ServerPlayer player = source.getPlayerOrException();
        ConsequenceState state = equipped(player, family);
        if (state == null) return missing(source);
        StigmataConsequenceEffects.touch(state, player);
        StigmataConsequenceEffects.setPressure(player, family, value);
        return status(source);
    }

    private static int trigger(CommandSourceStack source, @Nullable StigmataConsequence family)
            throws CommandSyntaxException {
        if (family == null) return 0;
        ServerPlayer player = source.getPlayerOrException();
        ConsequenceState state = equipped(player, family);
        if (state == null) return missing(source);
        Vec3 point = state.stage() == 1 ? player.position().add(player.getDirection().getStepX() * 4, 0,
                player.getDirection().getStepZ() * 4) : player.position();
        LivingEntity target = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(8), entity -> entity.getPersistentData().getBoolean(StigmataConsequenceEffects.TEST_DUMMY))
                .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(point))).orElse(null);
        StigmataConsequenceEffects.forceWarning(state, player, point, target);
        return 1;
    }

    private static int missing(CommandSourceStack source) {
        source.sendFailure(Component.translatable("command.esotericism_tinker.stigmata_test.equip"));
        return 0;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CompoundTag runtime = player.getPersistentData().getCompound(StigmataConsequenceEffects.RUNTIME_KEY);
        for (StigmataConsequence family : StigmataConsequence.values()) {
            CompoundTag frame = runtime.getCompound(family.name().toLowerCase(Locale.ROOT));
            if (frame.isEmpty()) continue;
            source.sendSuccess(() -> Component.translatable("command.esotericism_tinker.stigmata_test.status",
                    family.displayName(), frame.getInt("pressure"), frame.getInt("stage"),
                    Math.max(0, frame.getLong("due") - player.level().getGameTime())), false);
        }
        return 1;
    }

    private static int supplies(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        give(player, new ItemStack(Items.BREAD, 32));
        give(player, new ItemStack(Items.ARROW, 64));
        give(player, new ItemStack(Items.EXPERIENCE_BOTTLE, 32));
        give(player, new ItemStack(Items.WATER_BUCKET));
        give(player, new ItemStack(Items.MILK_BUCKET));
        give(player, new ItemStack(Items.SADDLE));
        return 6;
    }

    private static int target(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        // Undead targets ignore poison, so use a living illager for all effect checks.
        Vindicator dummy = EntityType.VINDICATOR.create(player.serverLevel());
        if (dummy == null) return 0;
        dummy.setPos(player.position().add(player.getDirection().getStepX() * 4, 0,
                player.getDirection().getStepZ() * 4));
        dummy.setNoAi(true);
        dummy.setPersistenceRequired();
        dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        dummy.setHealth(200);
        dummy.setCustomName(Component.translatable("command.esotericism_tinker.stigmata_test.dummy"));
        dummy.setCustomNameVisible(true);
        dummy.getPersistentData().putBoolean(StigmataConsequenceEffects.TEST_DUMMY, true);
        dummy.getPersistentData().putUUID(DUMMY_OWNER, player.getUUID());
        player.serverLevel().addFreshEntity(dummy);
        return 1;
    }

    private static int hit(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return player.hurt(player.damageSources().generic(), 2) ? 1 : 0;
    }

    private static int reset(CommandSourceStack source, boolean cleanup) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int removed = StigmataConsequenceEffects.clear(player);
        if (cleanup) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.getItem() instanceof IModifiable
                    && ToolStack.from(stack).getPersistentData().contains(StigmataData.TEST_OVERLOAD)) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                    removed++;
                }
            }
            for (Entity entity : player.serverLevel().getEntities(player, player.getBoundingBox().inflate(64),
                    entity -> entity.getPersistentData().hasUUID(DUMMY_OWNER)
                              && player.getUUID().equals(entity.getPersistentData().getUUID(DUMMY_OWNER)))) {
                entity.discard();
                removed++;
            }
        }
        int count = removed;
        source.sendSuccess(() -> Component.translatable("command.esotericism_tinker.stigmata_test.reset", count), false);
        return 1;
    }
}
