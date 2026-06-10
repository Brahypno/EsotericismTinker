package org.brahypno.esotericismtinker.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.EsotericismTinkerModule;
import org.brahypno.esotericismtinker.common.Items.EsotericismBookItem;

import static org.brahypno.esotericismtinker.EsotericismTinker.MODID;

public class EsotericismTinkerCommon extends EsotericismTinkerModule {
    public static final ResourceKey<CreativeModeTab> ITEM_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, EsotericismTinker.getLocation("item"));

    public static final RegistryObject<CreativeModeTab> ITEM =
            TABS.register("item", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup." + MODID + ".item")).icon(() -> new ItemStack(
                    EsotericismTinkerCommon.hypnagogic_transmute.get())).displayItems(EsotericismTinkerCommon::addTabs).build());

    public static final RegistryObject<EsotericismBookItem> hypnagogic_transmute =
            ITEMS.register("hypnagogic_transmute", () -> new EsotericismBookItem(UNSTACKABLE_PROPS, EsotericismBookItem.BookType.HYPNAGOGIC_TRANSMUTE));

    public static void addTabs(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
        output.accept(hypnagogic_transmute.get());
    }
}
