package org.brahypno.esotericismtinker;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

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

    static final ForgeConfigSpec SPEC = BUILDER.pop().build();

    public static int transmuteHeaterTemperature;
    public static int transmuteAcceleratorTemperature;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        transmuteHeaterTemperature = TRANSMUTE_HEATER_TEMPERATURE.get();
        transmuteAcceleratorTemperature = TRANSMUTE_FUEL_ACCELERATOR.get();
    }
}
