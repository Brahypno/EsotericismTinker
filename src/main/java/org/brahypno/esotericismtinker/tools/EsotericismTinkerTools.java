package org.brahypno.esotericismtinker.tools;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.EsotericismTinkerModule;
import org.brahypno.esotericismtinker.common.EsotericismTinkerCommon;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys;
import org.brahypno.esotericismtinker.tools.data.EsotericismTinkerStationLayout;
import org.brahypno.esotericismtinker.tools.data.EsotericismTinkerToolDefinitionProvider;
import org.brahypno.esotericismtinker.tools.data.EsotericismTinkerToolItemModelProvider;
import org.brahypno.esotericismtinker.tools.data.sprite.EsotericismPartSpriteProvider;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.library.client.data.material.GeneratorPartTextureJsonGenerator;
import slimeknights.tconstruct.library.client.data.material.MaterialPartTextureGenerator;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.tools.data.sprite.TinkerMaterialSpriteProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EsotericismTinkerTools extends EsotericismTinkerModule {
    public static final RegistryObject<CreativeModeTab> TOOL =
            EsotericismTinkerModule.TABS.register("tool", () -> CreativeModeTab.builder().title(EsotericismTinker.makeTranslation("itemGroup", "tool"))
                                                                               .icon(randomFixedDisplayTool(
                                                                                       EsotericismTinkerTagKeys.Items.EsotericismTinkerTools))
                                                                               .displayItems(EsotericismTinkerTools::addTabItems)
                                                                               .withTabsBefore(EsotericismTinkerCommon.ITEM.getId()).withSearchBar().build());

    public static final ItemObject<ModifiableItem> ritual_blade =
            MODI_TOOLS.register("ritual_blade", () -> new ModifiableItem(UNSTACKABLE_PROPS, EsotericismTinkerToolDefinitions.RitualBlade));

    @SubscribeEvent
    void gatherData(final GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(),
                              new MaterialPartTextureGenerator(output, existingFileHelper, new EsotericismPartSpriteProvider(),
                                                               new TinkerMaterialSpriteProvider()/*,
                                                               new EsotericismMaterialSpriteProvider()*/));


        generator.addProvider(event.includeClient(), new EsotericismTinkerToolItemModelProvider(output, existingFileHelper));
        generator.addProvider(event.includeServer(), new EsotericismTinkerToolDefinitionProvider(output));
        generator.addProvider(event.includeServer(), new EsotericismTinkerStationLayout(output));
        generator.addProvider(event.includeClient(),
                              new GeneratorPartTextureJsonGenerator(output, EsotericismTinker.MODID, new EsotericismPartSpriteProvider()));

    }

    /**
     * Adds all relevant items to the creative tab
     */
    private static void addTabItems(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output tab) {
        // start with tools that lack materials
        Consumer<ItemStack> output = tab::accept;

        // small tools
        acceptTool(output, ritual_blade);
        // broad tools

        // ranged tools

        // ancient tools
    }

    /**
     * Adds a tool to the tab
     */
    private static void acceptTool(Consumer<ItemStack> output, Supplier<? extends IModifiable> tool) {
        ToolBuildHandler.addVariants(output, tool.get(), "");
    }

    private static ItemStack cachedRandomEsotericismTinkerTool = ItemStack.EMPTY;

    public static Supplier<ItemStack> randomFixedDisplayTool(TagKey<Item> tag) {
        return () -> {
            if (cachedRandomEsotericismTinkerTool.isEmpty()){
                cachedRandomEsotericismTinkerTool = createRandomDisplayTool(tag);
            }

            // 防止外部修改缓存的 ItemStack
            return cachedRandomEsotericismTinkerTool.copy();
        };
    }

    private static ItemStack createRandomDisplayTool(TagKey<Item> tag) {
        List<Item> items = new ArrayList<>();

        BuiltInRegistries.ITEM.getTag(tag).ifPresent(holders -> {
            holders.forEach(holder -> items.add(holder.value()));
        });

        if (items.isEmpty()){
            return ItemStack.EMPTY;
        }

        Item item = items.get(RandomSource.create().nextInt(items.size()));

        // 比直接 getRenderTool() 更安全
        return IModifiableDisplay.getDisplayStack(item).copy();
    }
}
