package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.List;

public final class NoumenonEntries {
    private NoumenonEntries() {}

    public static void register() {
        registerReceptions();
        registerSublimations();
        registerTunings();
        registerInvestitures();
    }

    private static void registerReceptions() {
        NoumenonDatabase.registerReception(new NoumenonReceptionEntry(
                id("upgrade_slot"), "upgrades", 1, 64, 1,
                NoumenonRequirement.unrestricted(), display("upgrade_slot", "slot", Items.ANVIL)));
        NoumenonDatabase.registerReception(new NoumenonReceptionEntry(
                id("ability_slot"), "abilities", 3, 16, 2,
                NoumenonRequirement.unrestricted(), display("ability_slot", "slot", Items.NETHER_STAR)));
        NoumenonDatabase.registerReception(new NoumenonReceptionEntry(
                id("trait_slot"), "traits", 4, 16, 3,
                NoumenonRequirement.unrestricted(), display("trait_slot", "slot", Items.ECHO_SHARD)));
    }

    private static void registerSublimations() {
        NoumenonDatabase.registerSublimation(new NoumenonSublimationEntry(
                id("broad_melee_sweep"), 1, 8, 1,
                new NoumenonRequirement(List.of(TinkerTags.Items.BROAD_TOOLS, TinkerTags.Items.MELEE_PRIMARY), List.of(), List.of()),
                (context, source, level) -> {},
                display("broad_melee_sweep", "trait", Items.IRON_SWORD)));
        NoumenonDatabase.registerSublimation(new NoumenonSublimationEntry(
                id("aoe_control"), 1, 8, 1,
                new NoumenonRequirement(List.of(TinkerTags.Items.AOE), List.of(), List.of()),
                (context, source, level) -> {},
                display("aoe_control", "trait", Items.DIAMOND_PICKAXE)));
    }

    private static void registerTunings() {
        NoumenonDatabase.registerTuning(new NoumenonTuningEntry(
                id("softened_rejection"), 1, 8, 1,
                NoumenonRequirement.unrestricted(),
                (context, data, level, rejection) -> rejection - level,
                display("softened_rejection", "tuning", Items.AMETHYST_SHARD)));
    }

    private static void registerInvestitures() {
        NoumenonDatabase.registerInvestitureDefinition(new NoumenonInvestitureDefinitionEntry(
                id("example_source_tool_definition"), 2, 2, 4,
                display("example_source_tool_definition", "investiture", Items.SMITHING_TABLE)));
    }

    private static NoumenonDisplay display(String path, String category, Item icon) {
        return new NoumenonDisplay(
                Component.translatable("noumenon.esotericism_tinker." + path),
                List.of(Component.translatable("noumenon.esotericism_tinker." + path + ".desc")),
                () -> new ItemStack(icon), id(category), 0);
    }

    private static ResourceLocation id(String path) {
        return NoumenonKeys.id(path);
    }
}
