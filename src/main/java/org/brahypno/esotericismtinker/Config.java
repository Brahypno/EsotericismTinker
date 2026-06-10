package org.brahypno.esotericismtinker;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = EsotericismTinker.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue TRANSMUTE_HEATER_TEMPERATURE =
            BUILDER.comment("The default temperature provided by each heater block").defineInRange("TransmuteHeaterTemperature", 500, 0, 10000);

    public static final ForgeConfigSpec.IntValue TRANSMUTE_FUEL_ACCELERATOR =
            BUILDER.comment("The accelerator fuel rate provided by each block").defineInRange("TransmuteFuelAccelerator", 15, 0, 20000);

    static final ForgeConfigSpec SPEC = BUILDER.build();

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
