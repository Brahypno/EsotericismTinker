package org.brahypno.esotericismtinker;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;

import java.util.List;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER =
            new ForgeConfigSpec.Builder().comment("Configuration to almost all data in this mod. Take your own risk modify it!!!").push("Mod Compat");

    public static final ForgeConfigSpec.BooleanValue MOD_COMPAT_MATERIALS_CONFIG =
            BUILDER.comment("Enable this means enable conditional mod compact config").define("MOD_COMPACT_MATERIALS_CONFIG", false);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ModCompatBlackList =
            BUILDER.comment("Optional Blacklist for MOD Compat, not only for materials").defineList("ModCompatBlackList",
                                                                                                    List.of(""),
                                                                                                    o -> o instanceof String);

    static {
        BUILDER.pop();
        BUILDER.push("Transmute Configuration");
    }

    public static final ForgeConfigSpec.IntValue TRANSMUTE_HEATER_TEMPERATURE =
            BUILDER.comment("The default temperature provided by each heater block").defineInRange("TransmuteHeaterTemperature", 500, 0, 10000);

    public static final ForgeConfigSpec.IntValue TRANSMUTE_FUEL_ACCELERATOR =
            BUILDER.comment("The accelerator fuel rate provided by each block").defineInRange("TransmuteFuelAccelerator", 15, 0, 20000);

    public static final OreRate TRANSMUTE_SMELTERY_ORE_RATE;

    static {
        BUILDER.comment("Ore rates used by the transmute smeltery mode").push("SmelteryModeOreRate");
        // TConstruct smeltery defaults are 12 nuggets and 8 shards.
        TRANSMUTE_SMELTERY_ORE_RATE = new OreRate(BUILDER, 24, 16);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec.IntValue BYPRODUCT_ENTITY_MELTING_MULTIPLIER =
            BUILDER.comment("Multiplier applied to normal TConstruct entity melting outputs in the transmute")
                   .defineInRange("ByproductEntityMeltingMultiplier", 2, 1, 100);

    static {
        BUILDER.pop();
        BUILDER.push("World Generation");
    }

    public static final ForgeConfigSpec.BooleanValue GENERATE_TRANSMUTE_RUINS = BUILDER
            .comment("Whether transmute ruins naturally generate in new chunks.")
            .define("GenerateTransmuteRuins", true);

    static final ForgeConfigSpec SPEC = BUILDER.pop().build();

    public static int transmuteHeaterTemperature;
    public static int transmuteAcceleratorTemperature;
    public static boolean generateTransmuteRuins;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        transmuteHeaterTemperature = TRANSMUTE_HEATER_TEMPERATURE.get();
        transmuteAcceleratorTemperature = TRANSMUTE_FUEL_ACCELERATOR.get();
        generateTransmuteRuins = GENERATE_TRANSMUTE_RUINS.get();
    }

    /**
     * Configurable ore-rate implementation equivalent to TConstruct's ore-rate config.
     */
    public static final class OreRate implements IMeltingContainer.IOreRate {
        private final ForgeConfigSpec.ConfigValue<Integer> nuggetsPerMetal;
        private final ForgeConfigSpec.ConfigValue<Integer> shardsPerGem;

        private OreRate(ForgeConfigSpec.Builder builder, int defaultNuggets, int defaultShards) {
            nuggetsPerMetal = builder
                    .comment("Number of nuggets produced per metal ore unit melted. 9 nuggets equals 1 ingot")
                    .defineInRange("NuggetsPerMetal", defaultNuggets, 1, 90);
            shardsPerGem = builder
                    .comment("Number of gem shards produced per gem ore unit melted. 4 shards equals 1 gem")
                    .defineInRange("ShardsPerGem", defaultShards, 1, 40);
        }

        @Override
        public int applyOreBoost(OreRateType rate, int amount) {
            return switch (rate) {
                case METAL -> amount * nuggetsPerMetal.get() / 9;
                case GEM -> amount * shardsPerGem.get() / 4;
                default -> amount;
            };
        }
    }
}
