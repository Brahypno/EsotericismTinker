package org.brahypno.esotericismtinker.transcendence.table;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinkerModule;
import org.brahypno.esotericismtinker.transcendence.table.block.TranscendenceAnvilBlock;
import org.brahypno.esotericismtinker.transcendence.table.block.TranscendenceAnvilBlockEntity;
import org.brahypno.esotericismtinker.transcendence.table.menu.TranscendenceAnvilMenu;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.tables.item.AnvilBlockItem;
import slimeknights.tconstruct.tools.TinkerToolParts;

public final class EsotericismTinkerTranscendenceTable extends EsotericismTinkerModule {
    public static final ItemObject<TranscendenceAnvilBlock> transcendenceAnvil = BLOCKS.register("transcendence_anvil", () -> new TranscendenceAnvilBlock(builder(MapColor.COLOR_PURPLE, SoundType.ANVIL).pushReaction(PushReaction.BLOCK).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).noOcclusion()), block -> new AnvilBlockItem(block, new Item.Properties(), TinkerToolParts.fakeStorageBlockItem, TinkerTags.Materials.COMPATABILITY_ALLOYS));
    public static final RegistryObject<BlockEntityType<TranscendenceAnvilBlockEntity>> transcendenceAnvilBE = BLOCK_ENTITIES.register("transcendence_anvil", TranscendenceAnvilBlockEntity::new, transcendenceAnvil);
    public static final RegistryObject<MenuType<TranscendenceAnvilMenu>> transcendenceAnvilMenu = MENUS.register("transcendence_anvil", () -> IForgeMenuType.create(TranscendenceAnvilMenu::new));
}
