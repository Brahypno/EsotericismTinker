package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.List;
import java.util.Set;

public final class NoumenonEntries {
    public static final TagKey<Item> ELEVATION_AXES =
            TagKey.create(Registries.ITEM, id("elevation/axe"));
    public static final TagKey<Item> ELEVATION_HAMMERS =
            TagKey.create(Registries.ITEM, id("elevation/hammer"));

    private NoumenonEntries() {}

    public static void register() {
        registerReceptions();
        registerSublimationGroups();
        registerSublimations();
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

    private static void registerSublimationGroups() {
        group("melee", TinkerTags.Items.MELEE_WEAPON, Set.of(), Items.IRON_SWORD);
        group("ranged", TinkerTags.Items.RANGED, Set.of(), Items.ARROW);
        group("bow", TinkerTags.Items.BOWS, Set.of(id("ranged")), Items.BOW);
        group("shield", TinkerTags.Items.SHIELDS, Set.of(), Items.SHIELD);
        group("armor", TinkerTags.Items.WORN_ARMOR, Set.of(), Items.DIAMOND_CHESTPLATE);
        group("harvest", TinkerTags.Items.HARVEST, Set.of(), Items.DIAMOND_PICKAXE);
        group("sword", TinkerTags.Items.SWORD, Set.of(id("melee")), Items.DIAMOND_SWORD);
        group("scythe", TinkerTags.Items.SCYTHES, Set.of(id("melee")), Items.IRON_HOE);
        group("axe", ELEVATION_AXES, Set.of(id("melee")), Items.DIAMOND_AXE);
        group("hammer", ELEVATION_HAMMERS, Set.of(id("melee")), Items.IRON_PICKAXE);
    }

    private static void registerSublimations() {
        path("melee_damage", "melee", TinkerTags.Items.MELEE_WEAPON, Items.IRON_SWORD);
        path("melee_speed", "melee", TinkerTags.Items.MELEE_WEAPON, Items.FEATHER);

        path("ranged_draw_speed", "ranged", TinkerTags.Items.RANGED, Items.CLOCK);
        path("ranged_ballistics", "ranged", TinkerTags.Items.RANGED, Items.ARROW);

        path("bow_multishot", "bow", TinkerTags.Items.BOWS, Items.SPECTRAL_ARROW);
        path("bow_piercing", "bow", TinkerTags.Items.BOWS, Items.TIPPED_ARROW);

        path("shield_guard", "shield", TinkerTags.Items.SHIELDS, Items.SHIELD);
        path("shield_counter", "shield", TinkerTags.Items.SHIELDS, Items.IRON_SWORD);

        path("armor_toughness", "armor", TinkerTags.Items.WORN_ARMOR, Items.DIAMOND_CHESTPLATE);
        path("armor_stability", "armor", TinkerTags.Items.WORN_ARMOR, Items.IRON_BOOTS);
        path("armor_threshold", "armor", TinkerTags.Items.WORN_ARMOR, Items.OBSIDIAN);

        path("harvest_speed", "harvest", TinkerTags.Items.HARVEST, Items.GOLDEN_PICKAXE);
        path("harvest_endurance", "harvest", TinkerTags.Items.HARVEST, Items.ANVIL);

        path("sword_range", "sword", TinkerTags.Items.SWORD, Items.IRON_SWORD);
        path("sword_sweep_damage", "sword", TinkerTags.Items.SWORD, Items.DIAMOND_SWORD);

        path("scythe_reap", "scythe", TinkerTags.Items.SCYTHES, Items.IRON_HOE);
        path("scythe_sustain", "scythe", TinkerTags.Items.SCYTHES, Items.GHAST_TEAR);

        path("axe_execute", "axe", ELEVATION_AXES, Items.DIAMOND_AXE);
        path("axe_heavy", "axe", ELEVATION_AXES, Items.IRON_AXE);

        path("hammer_knockback", "hammer", ELEVATION_HAMMERS, Items.PISTON);
        path("hammer_crush", "hammer", ELEVATION_HAMMERS, Items.ANVIL);
    }

    private static void registerInvestitures() {
        NoumenonDatabase.registerInvestitureDefinition(new NoumenonInvestitureDefinitionEntry(
                id("example_source_tool_definition"), 2, 2, 4,
                display("example_source_tool_definition", "investiture", Items.SMITHING_TABLE)));
    }

    private static void group(String path, TagKey<Item> tag,
                              Set<ResourceLocation> replaces, Item icon) {
        NoumenonDatabase.registerSublimationGroup(new NoumenonSublimationGroup(
                id(path),
                new NoumenonRequirement(List.of(tag), List.of(), List.of()),
                replaces,
                display("group." + path, "sublimation_group", icon)
        ));
    }

    private static void path(String path, String group, TagKey<Item> tag, Item icon) {
        NoumenonDatabase.registerSublimation(new NoumenonSublimationEntry(
                id(path), id(group), 1, 5, 1,
                new NoumenonRequirement(List.of(tag), List.of(), List.of()),
                (context, source, level, builder) -> {},
                display(path, "sublimation", icon)
        ));
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
