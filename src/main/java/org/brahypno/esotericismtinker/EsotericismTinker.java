package org.brahypno.esotericismtinker;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.common.EsotericismTinkerCommon;
import org.brahypno.esotericismtinker.common.data.EsotericismTinkerRecipeProvider;
import org.brahypno.esotericismtinker.common.data.loot.EsotericismTinkerLootTableProvider;
import org.brahypno.esotericismtinker.common.data.model.EsotericismTinkerBlockStateProvider;
import org.brahypno.esotericismtinker.common.data.model.EsotericismTinkerItemModelProvider;
import org.brahypno.esotericismtinker.common.data.render.RenderFluidProvider;
import org.brahypno.esotericismtinker.common.data.tags.BlockTagProvider;
import org.brahypno.esotericismtinker.common.data.tags.FluidTagProvider;
import org.brahypno.esotericismtinker.common.data.tags.ItemTagProvider;
import org.brahypno.esotericismtinker.common.json.ETConfigEnabledCondition;
import org.brahypno.esotericismtinker.fluids.EsotericismTinkerFluids;
import org.brahypno.esotericismtinker.fluids.data.EsotericismTinkerFluidTextureProvider;
import org.brahypno.esotericismtinker.fluids.data.FluidTooltipProvider;
import org.brahypno.esotericismtinker.library.compact.ars_nouveau.NovaRegistry;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerTools;
import org.brahypno.esotericismtinker.tools.data.EsotericismTinkerFluidEffectProvider;
import org.slf4j.Logger;
import slimeknights.tconstruct.fluids.data.FluidBlockstateModelProvider;
import slimeknights.tconstruct.fluids.data.FluidBucketModelProvider;
import slimeknights.tconstruct.library.utils.Util;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mod(EsotericismTinker.MODID)
public class EsotericismTinker {
    public static final String MODID = "esotericism_tinker";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static List<? extends String> compact_config;
    private static Boolean compactRestriction;

    @SuppressWarnings({"removal"})
    public EsotericismTinker() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.register(new EsotericismTinkerCommon());
        modEventBus.register(new EsotericismTinkerFluids());
        modEventBus.register(new EsotericismTinkerSmeltery());
        modEventBus.register(new EsotericismTinkerTools());
        modEventBus.register(new EsotericismTinkerModifiers());
        EsotericismTinkerModule.initRegisters(modEventBus);
        modEventBus.addListener(this::gatherData);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (ModList.get().isLoaded("ars_nouveau")){
            NovaRegistry.init(modEventBus);
        }
        CraftingHelper.register(ETConfigEnabledCondition.SERIALIZER);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
        });
    }

    public static ResourceLocation getLocation(String name) {
        return new ResourceLocation(MODID, name);
    }

    public static String makeTranslationKey(String base, String name) {
        return Util.makeTranslationKey(base, getLocation(name));
    }

    public static MutableComponent makeTranslation(String base, String name) {
        return Component.translatable(makeTranslationKey(base, name));
    }

    public static TagKey<Block> forgeBlockTag(String name) {
        return TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(), new ResourceLocation("forge", name));
    }

    public static TagKey<Item> forgeItemTag(String name) {
        return TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), new ResourceLocation("forge", name));
    }

    public static boolean configCompactDisabled(String modId) {
        if (null == compact_config)
            compact_config = Config.ModCompactBlackList.get();
        compactRestriction = Config.MOD_COMPACT_MATERIALS_CONFIG.get();
        return compactRestriction && compact_config.contains(modId);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    public void gatherData(final GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        BlockTagProvider blockTags = new BlockTagProvider(output, lookupProvider, MODID, helper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new FluidTagProvider(output, lookupProvider, MODID, helper));
        generator.addProvider(event.includeServer(), new ItemTagProvider(output, lookupProvider, blockTags.contentsGetter(), MODID, helper));
        generator.addProvider(event.includeServer(), new EsotericismTinkerRecipeProvider(output));
        generator.addProvider(event.includeServer(), new EsotericismTinkerLootTableProvider(output));
        generator.addProvider(event.includeServer(), new EsotericismTinkerFluidEffectProvider(output));
        generator.addProvider(event.includeClient(), new EsotericismTinkerBlockStateProvider(output, helper));
        generator.addProvider(event.includeClient(), new EsotericismTinkerItemModelProvider(output, helper));
        generator.addProvider(event.includeClient(), new FluidTooltipProvider(output));
        generator.addProvider(event.includeClient(), new EsotericismTinkerFluidTextureProvider(output));
        generator.addProvider(event.includeClient(), new FluidBucketModelProvider(output, MODID));
        generator.addProvider(event.includeClient(), new FluidBlockstateModelProvider(output, MODID));
        generator.addProvider(event.includeClient(), new RenderFluidProvider(output));
    }
}
