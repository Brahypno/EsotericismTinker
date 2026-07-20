package org.brahypno.esotericismtinker.transcendence.intrinsic.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonData;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonAllocationLogic;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonDatabase;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonInvestitureLogic;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonKeys;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class NoumenonTestCommands {
    private static final Dynamic2CommandExceptionType ERROR_UNKNOWN_SLOT_TYPE =
            new Dynamic2CommandExceptionType((slot, available) -> Component.translatable(
                    "command.esotericism_tinker.noumenon_test.unknown_slot_type", slot, available));
    private static final Dynamic2CommandExceptionType ERROR_INSUFFICIENT_SUBSTRATE =
            new Dynamic2CommandExceptionType((required, available) -> Component.translatable(
                    "command.esotericism_tinker.noumenon_test.insufficient_substrate", required, available));
    private static final Dynamic3CommandExceptionType ERROR_INSUFFICIENT_RECEPTION_SLOTS =
            new Dynamic3CommandExceptionType((slot, requested, current) -> Component.translatable(
                    "command.esotericism_tinker.noumenon_test.insufficient_reception_slots", slot, requested, current));

    private NoumenonTestCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NoumenonKeys.MOD_ID)
                                    .then(Commands.literal("noumenon_test")
                                                  .requires(source -> source.hasPermission(2))
                                                  .then(Commands.literal("dump").executes(ctx -> dump(ctx.getSource())))
                                                  .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource())))
                                                  .then(Commands.literal("rebuild").executes(ctx -> rebuild(ctx.getSource())))
                                                  .then(Commands.literal("level")
                                                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                                              .executes(ctx -> setLevel(ctx.getSource(),
                                                                                                        IntegerArgumentType.getInteger(ctx, "level")))))
                                                  .then(Commands.literal("points")
                                                                .then(pointAdjustment("add", 1))
                                                                .then(pointAdjustment("remove", -1)))
                                                  .then(Commands.literal("reception")
                                                                .then(receptionAdjustment("add", 1))
                                                                .then(receptionAdjustment("remove", -1)))
                                                  .then(Commands.literal("sublimation")
                                                                .then(Commands.argument("id", StringArgumentType.string())
                                                                              .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                                                            .executes(ctx -> setSublimation(ctx.getSource(),
                                                                                                                            StringArgumentType.getString(ctx,
                                                                                                                                                         "id"),
                                                                                                                            IntegerArgumentType.getInteger(ctx,
                                                                                                                                                           "level"))))))
                                                  .then(Commands.literal("tuning")
                                                                .then(Commands.argument("id", StringArgumentType.string())
                                                                              .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                                                            .executes(ctx -> setTuning(ctx.getSource(),
                                                                                                                       StringArgumentType.getString(ctx, "id"),
                                                                                                                       IntegerArgumentType.getInteger(ctx,
                                                                                                                                                      "level"))))))
                                                  .then(Commands.literal("investiture_list_offhand")
                                                                .executes(ctx -> listInvestitureFromOffhand(ctx.getSource())))
                                                  .then(Commands.literal("investiture_from_offhand")
                                                                .then(Commands.argument("force", BoolArgumentType.bool())
                                                                              .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                                                            .executes(ctx -> setInvestitureFromOffhand(
                                                                                                    ctx.getSource(),
                                                                                                    BoolArgumentType.getBool(ctx, "force"),
                                                                                                    IntegerArgumentType.getInteger(ctx, "index"))))))
                                                  .then(Commands.literal("investiture_all_from_offhand")
                                                                .then(Commands.argument("force", BoolArgumentType.bool())
                                                                              .executes(ctx -> setAllInvestitureFromOffhand(ctx.getSource(),
                                                                                                                            BoolArgumentType.getBool(ctx,
                                                                                                                                                     "force")))))
                                                  .then(Commands.literal("preset")
                                                                .then(Commands.literal("basic").executes(ctx -> presetBasic(ctx.getSource())))
                                                                .then(Commands.literal("broad_melee").executes(ctx -> presetBroadMelee(ctx.getSource()))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> pointAdjustment(String operation, int direction) {
        return Commands.literal(operation)
                       .then(Commands.argument("substrate", IntegerArgumentType.integer(0))
                                     .then(Commands.argument("elevation", IntegerArgumentType.integer(0))
                                                   .executes(ctx -> adjustPoints(
                                                           ctx.getSource(),
                                                           direction,
                                                           IntegerArgumentType.getInteger(ctx, "substrate"),
                                                           IntegerArgumentType.getInteger(ctx, "elevation")))));
    }

    private static int dump(CommandSourceStack source) {
        return withTool(source, tool -> {
            NoumenonData data = NoumenonData.read(tool);
            message(source, Component.translatable(
                    "command.esotericism_tinker.noumenon_test.dump.points",
                    data.level,
                    data.remainingSubstratePoints(), data.baseSubstratePoints(), data.debugSubstratePoints,
                    data.substratePoints, data.usedReceptionSlotPoints(), data.usedTuningPoints(),
                    data.remainingElevationPoints(), data.baseElevationPoints(), data.debugElevationPoints,
                    data.elevationPoints, data.usedSublimationPoints(), data.usedInvestiturePoints()
            ));
            message(source, Component.translatable("command.esotericism_tinker.noumenon_test.dump.reception", data.receptionSlots));
            message(source, Component.translatable("command.esotericism_tinker.noumenon_test.dump.sublimation", data.sublimations));
            message(source, Component.translatable("command.esotericism_tinker.noumenon_test.dump.tuning", data.tunings));
            message(source, Component.translatable(
                    "command.esotericism_tinker.noumenon_test.dump.investiture",
                    definitionDisplayName(data.investedDefinition),
                    Component.translatable(data.investitureLocked
                                           ? "command.esotericism_tinker.noumenon_test.state.locked"
                                           : "command.esotericism_tinker.noumenon_test.state.unlocked"),
                    data.investitureRejection
            ));
            message(source, Component.translatable(
                    "command.esotericism_tinker.noumenon_test.dump.invested_traits",
                    formatInvestedTraits(data)));
            message(source, Component.translatable(
                    "command.esotericism_tinker.noumenon_test.dump.valid",
                    Component.translatable(data.isValid()
                                           ? "command.esotericism_tinker.noumenon_test.state.valid"
                                           : "command.esotericism_tinker.noumenon_test.state.invalid")
            ));
            return 1;
        });
    }

    private static int clear(CommandSourceStack source) {
        return mutate(source, data -> {
            data.level = 0;
            data.debugSubstratePoints = 0;
            data.debugElevationPoints = 0;
            data.receptionSlots.clear();
            data.sublimations.clear();
            data.tunings.clear();
            data.clearInvestiture();
        }, "cleared");
    }

    private static int rebuild(CommandSourceStack source) {
        return withTool(source, tool -> {
            tool.rebuildStats();
            message(source, "rebuildStats() called");
            return 1;
        });
    }

    private static int setLevel(CommandSourceStack source, int level) {
        return mutate(source, data -> data.level = level, "level=" + level);
    }

    private static int adjustPoints(CommandSourceStack source, int direction, int substrate, int elevation) {
        return mutate(source, data -> {
            data.debugSubstratePoints = addClamped(data.debugSubstratePoints, (long) direction * substrate);
            data.debugElevationPoints = addClamped(data.debugElevationPoints, (long) direction * elevation);
        }, "debug points " + (direction > 0 ? "added" : "removed") + " substrate=" + substrate + " elevation=" + elevation);
    }

    private static CompletableFuture<Suggestions> suggestSlotTypes(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        SlotType.getAllSlotTypes().stream()
                .map(SlotType::getName)
                .sorted()
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> receptionAdjustment(String operation, int direction) {
        return Commands.literal(operation)
                       .then(Commands.argument("slot_type", StringArgumentType.word())
                                     .suggests(NoumenonTestCommands::suggestSlotTypes)
                                     .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                   .executes(ctx -> adjustReceptionSlot(
                                                           ctx.getSource(),
                                                           StringArgumentType.getString(ctx, "slot_type"),
                                                           IntegerArgumentType.getInteger(ctx, "count"),
                                                           direction))));
    }

    private static String getAvailableSlotTypesText() {
        return String.join(", ", SlotType.getAllSlotTypes().stream()
                                         .map(SlotType::getName)
                                         .sorted()
                                         .toList());
    }

    private static int adjustReceptionSlot(CommandSourceStack source, String slotTypeKey, int count, int direction)
            throws CommandSyntaxException {
        SlotType slotType = SlotType.getIfPresent(slotTypeKey);
        if (slotType == null){
            throw ERROR_UNKNOWN_SLOT_TYPE.create(slotTypeKey, getAvailableSlotTypesText());
        }

        Player player = source.getPlayerOrException();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        ToolStack tool = ToolStack.from(stack);
        NoumenonData data = NoumenonData.read(tool);
        String name = slotType.getName();
        int current = data.receptionSlots.getOrDefault(name, 0);

        if (direction > 0){
            int cost = NoumenonDatabase.getReceptionCost(name);
            long required = (long) cost * count;
            int available = data.remainingSubstratePoints();
            if (required > available){
                throw ERROR_INSUFFICIENT_SUBSTRATE.create(required, available);
            }
            data.receptionSlots.put(name, Math.addExact(current, count));
        }else {
            if (count > current){
                throw ERROR_INSUFFICIENT_RECEPTION_SLOTS.create(slotType.getDisplayName(), count, current);
            }
            putOrRemove(data.receptionSlots, name, current - count);
        }

        Component validation = NoumenonAllocationLogic.validateAndApply(tool, changed -> {
            int changedCurrent = changed.receptionSlots.getOrDefault(name, 0);
            putOrRemove(changed.receptionSlots, name, changedCurrent + direction * count);
        });
        if (validation != null){
            source.sendFailure(validation);
            return 0;
        }
        data = NoumenonData.read(tool);
        message(source, Component.translatable(
                direction > 0
                ? "command.esotericism_tinker.noumenon_test.reception.added"
                : "command.esotericism_tinker.noumenon_test.reception.removed",
                slotType.getDisplayName(),
                count,
                data.remainingSubstratePoints()));
        return 1;
    }

    private static int setSublimation(CommandSourceStack source, String id, int level) {
        return withTool(source, tool -> {
            ResourceLocation sublimationId = parse(id);
            Component validation = NoumenonAllocationLogic.validateAndApply(
                    tool, data -> putOrRemove(data.sublimations, sublimationId, level));
            if (validation != null){
                source.sendFailure(validation);
                return 0;
            }
            message(source, "sublimation " + id + "=" + level);
            return 1;
        });
    }

    private static int setTuning(CommandSourceStack source, String id, int level) {
        return mutate(source, data -> putOrRemove(data.tunings, parse(id), level), "tuning " + id + "=" + level);
    }

    private static int listInvestitureFromOffhand(CommandSourceStack source) {
        try {
            ToolStack sourceTool = offhandTool(source);
            message(source, NoumenonInvestitureLogic.formatDefinitionTraits(sourceTool));
            return 1;
        }
        catch (Exception e) {
            source.sendFailure(Component.literal(e.getMessage() == null ? e.toString() : e.getMessage()));
            return 0;
        }
    }

    private static int setInvestitureFromOffhand(CommandSourceStack source, boolean force, int index) {
        try {
            Player player = source.getPlayerOrException();
            ToolStack targetTool = ToolStack.from(player.getItemInHand(InteractionHand.MAIN_HAND));
            ToolStack sourceTool = offhandTool(source);
            Component validation = NoumenonInvestitureLogic.captureOneTraitFromSourceTool(targetTool, sourceTool, force, index);
            if (validation != null){
                source.sendFailure(validation);
                return 0;
            }
            NoumenonData data = NoumenonData.read(targetTool);
            message(source, "investiture captured: " + data.investedDefinition + " traits=" + data.investedTraits);
            return 1;
        }
        catch (Exception e) {
            source.sendFailure(Component.literal(e.getMessage() == null ? e.toString() : e.getMessage()));
            return 0;
        }
    }

    private static int setAllInvestitureFromOffhand(CommandSourceStack source, boolean force) {
        try {
            Player player = source.getPlayerOrException();
            ToolStack targetTool = ToolStack.from(player.getItemInHand(InteractionHand.MAIN_HAND));
            ToolStack sourceTool = offhandTool(source);
            Component validation = NoumenonInvestitureLogic.captureAllTraitsFromSourceTool(targetTool, sourceTool, force);
            if (validation != null){
                source.sendFailure(validation);
                return 0;
            }
            NoumenonData data = NoumenonData.read(targetTool);
            message(source, "test investiture captured all: " + data.investedDefinition + " traits=" + data.investedTraits);
            return 1;
        }
        catch (Exception e) {
            source.sendFailure(Component.literal(e.getMessage() == null ? e.toString() : e.getMessage()));
            return 0;
        }
    }

    private static int presetBasic(CommandSourceStack source) {
        return mutate(source, data -> {
            data.level = 2;
            data.receptionSlots.clear();
            data.sublimations.clear();
            data.tunings.clear();
            data.receptionSlots.put("upgrades", 2);
            data.receptionSlots.put("abilities", 1);
            data.tunings.put(NoumenonKeys.id("softened_rejection"), 2);
        }, "preset basic");
    }

    private static int presetBroadMelee(CommandSourceStack source) {
        return mutate(source, data -> {
            data.level = 3;
            data.receptionSlots.clear();
            data.sublimations.clear();
            data.tunings.clear();
            data.receptionSlots.put("upgrades", 2);
            data.receptionSlots.put("abilities", 1);
            data.sublimations.put(NoumenonKeys.id("broad_melee_sweep"), 3);
            data.tunings.put(NoumenonKeys.id("softened_rejection"), 2);
        }, "preset broad_melee");
    }

    private static ToolStack offhandTool(CommandSourceStack source) throws Exception {
        Player player = source.getPlayerOrException();
        ItemStack sourceStack = player.getItemInHand(InteractionHand.OFF_HAND);
        if (sourceStack.isEmpty())
            throw new IllegalArgumentException("Offhand source tool is required");
        return ToolStack.from(sourceStack);
    }

    private static int mutate(CommandSourceStack source, Mutator mutator, String success) {
        return withTool(source, tool -> {
            NoumenonData data = NoumenonData.read(tool);
            mutator.accept(data);
            ToolDataNBT persistent = tool.getPersistentData();
            data.write(persistent);
            tool.rebuildStats();
            message(source, success);
            return 1;
        });
    }

    private static int withTool(CommandSourceStack source, ToolAction action) {
        try {
            Player player = source.getPlayerOrException();
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty()){
                message(source, "Main hand is empty");
                return 0;
            }
            ToolStack tool = ToolStack.from(stack);
            return action.run(tool);
        }
        catch (Exception e) {
            source.sendFailure(Component.literal(e.getMessage() == null ? e.toString() : e.getMessage()));
            return 0;
        }
    }

    private static <K> void putOrRemove(Map<K, Integer> map, K id, int level) {
        if (level <= 0)
            map.remove(id);
        else
            map.put(id, level);
    }

    private static int addClamped(int current, long delta) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) current + delta));
    }

    private static ResourceLocation parse(String raw) {
        return ResourceLocation.tryParse(raw) == null ? NoumenonKeys.id(raw) : new ResourceLocation(raw);
    }

    /**
     * Resolves a tool definition ID to the localized name of its matching registered item.
     */
    private static Component definitionDisplayName(ResourceLocation definitionId) {
        if (definitionId == null)
            return Component.literal("-");
        return BuiltInRegistries.ITEM.getOptional(definitionId)
                                     .map(item -> item.getDescription())
                                     .orElseGet(() -> Component.literal(definitionId.toString()));
    }

    /**
     * Formats invested modifier snapshots without losing their localized display names.
     */
    private static Component formatInvestedTraits(NoumenonData data) {
        MutableComponent result = Component.literal("{");
        boolean first = true;
        for (Map.Entry<ResourceLocation, Integer> trait : data.investedTraits.entrySet()) {
            if (!first)
                result.append(Component.literal(", "));
            ModifierEntry entry = new ModifierEntry(new ModifierId(trait.getKey()), trait.getValue());
            result.append(entry.getDisplayName());
            result.append(Component.literal("=" + trait.getValue()));
            first = false;
        }
        return result.append(Component.literal("}"));
    }

    private static void message(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static void message(CommandSourceStack source, Component text) {
        source.sendSuccess(() -> text, false);
    }

    @FunctionalInterface
    private interface Mutator {
        void accept(NoumenonData data);
    }

    @FunctionalInterface
    private interface ToolAction {
        int run(ToolStack tool);
    }
}
