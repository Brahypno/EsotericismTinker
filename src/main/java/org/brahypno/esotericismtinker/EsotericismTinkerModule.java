package org.brahypno.esotericismtinker;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.mantle.registration.deferred.BlockEntityTypeDeferredRegister;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.deferred.SynchronizedDeferredRegister;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.tconstruct.common.registration.BlockDeferredRegisterExtension;
import slimeknights.tconstruct.common.registration.ItemDeferredRegisterExtension;

import java.util.function.Function;

import static org.brahypno.esotericismtinker.EsotericismTinker.MODID;

public abstract class EsotericismTinkerModule {
    protected static final Item.Properties UNSTACKABLE_PROPS = new Item.Properties().stacksTo(1);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final ItemDeferredRegisterExtension MODI_TOOLS = new ItemDeferredRegisterExtension(MODID);
    public static final BlockDeferredRegisterExtension BLOCKS = new BlockDeferredRegisterExtension(MODID);
    protected static final BlockEntityTypeDeferredRegister BLOCK_ENTITIES = new BlockEntityTypeDeferredRegister(MODID);
    public static final FluidDeferredRegister FLUIDS = new FluidDeferredRegister(MODID);
    public static final SynchronizedDeferredRegister<CreativeModeTab> TABS = SynchronizedDeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    protected static final Function<Block, ? extends BlockItem> TOOLTIP_BLOCK_ITEM = b -> new BlockTooltipItem(b, new Item.Properties());

    public static void initRegisters(IEventBus bus) {
        ITEMS.register(bus);
        MODI_TOOLS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        FLUIDS.register(bus);
        TABS.register(bus);
    }

    protected static BlockBehaviour.Properties builder(SoundType soundType) {
        return Block.Properties.of().sound(soundType);
    }

    protected static BlockBehaviour.Properties builder(MapColor color, SoundType soundType) {
        return builder(soundType).mapColor(color);
    }

    protected static void accept(CreativeModeTab.Output output, BuildingBlockObject object) {
        object.forEach(output::accept);
    }
}
