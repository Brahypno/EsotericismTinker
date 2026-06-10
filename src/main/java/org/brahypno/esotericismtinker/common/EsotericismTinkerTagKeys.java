package org.brahypno.esotericismtinker.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.EsotericismTinker;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierManager;

public class EsotericismTinkerTagKeys {
    public static class Items {
        private static TagKey<Item> local(String name) {
            return TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), EsotericismTinker.getLocation(name));
        }

        private static TagKey<Item> minecraft(String name) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(name));
        }

        public static final TagKey<Item> EXPLORATION_ORES = local("exploration/ores");
        public static final TagKey<Item> EXPLORATION_RITUAL_PRODUCTS = local("exploration/ritual_products");

        public static final TagKey<Item> ASHEN_BLOCKS = local("ashen_blocks");
        public static final TagKey<Item> ASHEN_TANKS = local("ashen_tanks");
        public static final TagKey<Item> TRANSMUTE_BLOCKS = local("transmute_blocks");
        public static final TagKey<Item> TRANSMUTE = local("transmute");
        public static final TagKey<Item> TRANSMUTE_HEATER = local("transmute_heater");
        public static final TagKey<Item> TRANSMUTE_ACCEL = local("transmute_accelerator");

        public static final TagKey<Item> EsotericismTinkerTools = local("my_tools");//Must be IModifiableDisplay

        public static final TagKey<Item> Doors = minecraft("doors");
        public static final TagKey<Item> CURIOS = local("my_curios");

    }

    public static class Blocks {
        private static TagKey<Block> local(String name) {
            return TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(), EsotericismTinker.getLocation(name));
        }

        private static TagKey<Block> minecraft(String name) {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(name));
        }

        public static final TagKey<Block> ASHEN_BLOCKS = local("ashen_blocks");
        public static final TagKey<Block> TRANSMUTE_BLOCKS = local("transmute_blocks");
        public static final TagKey<Block> ASHEN_TANKS = local("ashen_tanks");
        public static final TagKey<Block> TRANSMUTE_HEATER = local("transmute_heater");
        public static final TagKey<Block> TRANSMUTE_ACCEL = local("transmute_accelerator");
        public static final TagKey<Block> TRANSMUTE_ALLOY_SWITCH = local("transmute_alloyer_switch");
        public static final TagKey<Block> TRANSMUTE_MELTING_SWITCH = local("transmute_melting_switch");
        public static final TagKey<Block> TRANSMUTE = local("transmute");
        public static final TagKey<Block> TRANSMUTE_TANKS = local("transmute/tanks");
        public static final TagKey<Block> TRANSMUTE_FLOOR = local("transmute/floor");
        public static final TagKey<Block> TRANSMUTE_WALL = local("transmute/wall");
        public static final TagKey<Block> TRANSMUTE_CEILING = local("transmute/ceiling");
    }

    public static class Modifiers {
        private static TagKey<Modifier> local(String name) {
            return ModifierManager.getTag(EsotericismTinker.getLocation(name));
        }

        public static final TagKey<Modifier> DELUSIONS = local("delusions");
        public static final TagKey<Modifier> GENERAL_DELUSIONS = local("delusions/general");
        public static final TagKey<Modifier> MELEE_DELUSIONS = local("delusions/melee");
        public static final TagKey<Modifier> DAMAGE_DELUSIONS = local("delusions/damage");
        public static final TagKey<Modifier> HARVEST_DELUSIONS = local("delusions/harvest");
        public static final TagKey<Modifier> RANGED_DELUSIONS = local("delusions/ranged");
        // armor delusions
        public static final TagKey<Modifier> ARMOR_DELUSIONS = local("delusions/armor");
        public static final TagKey<Modifier> GENERAL_ARMOR_DELUSIONS = local("delusions/armor/general");
        public static final TagKey<Modifier> HELMET_DELUSIONS = local("delusions/armor/helmet");
        public static final TagKey<Modifier> CHESTPLATE_DELUSIONS = local("delusions/armor/chestplate");
        public static final TagKey<Modifier> LEGGING_DELUSIONS = local("delusions/armor/leggings");
        public static final TagKey<Modifier> BOOT_DELUSIONS = local("delusions/armor/boots");
    }
}
