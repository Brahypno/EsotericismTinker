package org.brahypno.esotericismtinker.selenic;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.EsotericismTinkerModule;
import org.brahypno.esotericismtinker.selenic.block.component.ArmillaryCrownBlock;
import org.brahypno.esotericismtinker.selenic.block.component.AstrolabeSpineBlock;
import org.brahypno.esotericismtinker.selenic.block.component.LunarFontBlock;
import org.brahypno.esotericismtinker.selenic.block.component.TestimonyStandBlock;
import org.brahypno.esotericismtinker.selenic.block.entity.ArmillaryCrownBlockEntity;
import org.brahypno.esotericismtinker.selenic.block.entity.LunarFontBlockEntity;
import org.brahypno.esotericismtinker.selenic.block.entity.TestimonyStandBlockEntity;
import slimeknights.mantle.registration.object.ItemObject;

public class EsotericismTinkerSelenic extends EsotericismTinkerModule {
    public static final ResourceKey<CreativeModeTab> SELENIC_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, EsotericismTinker.getLocation("selenic"));

    public static final RegistryObject<CreativeModeTab> tabSelenic = TABS.register(
            "selenic",
            () -> CreativeModeTab.builder()
                                 .title(EsotericismTinker.makeTranslation("itemGroup", "selenic"))
                                 .icon(() -> new ItemStack(EsotericismTinkerSelenic.lunarFont.get()))
                                 .displayItems(EsotericismTinkerSelenic::addTabItems)
                                 .build());

    public static final ItemObject<LunarFontBlock> lunarFont =
            BLOCKS.register("lunar_font", () -> new LunarFontBlock(selenicProps(2).lightLevel(LunarFontBlock.LIGHT)), TOOLTIP_BLOCK_ITEM);

    public static final ItemObject<AstrolabeSpineBlock> astrolabeSpine =
            BLOCKS.register("astrolabe_spine", () -> new AstrolabeSpineBlock(selenicProps(1)), TOOLTIP_BLOCK_ITEM);

    public static final ItemObject<ArmillaryCrownBlock> armillaryCrown =
            BLOCKS.register("armillary_crown", () -> new ArmillaryCrownBlock(selenicProps(1).lightLevel(ArmillaryCrownBlock.LIGHT)), TOOLTIP_BLOCK_ITEM);

    public static final ItemObject<TestimonyStandBlock> testimonyStand =
            BLOCKS.register("testimony_stand", () -> new TestimonyStandBlock(selenicProps(1)), TOOLTIP_BLOCK_ITEM);

    public static final RegistryObject<BlockEntityType<LunarFontBlockEntity>> lunarFontBE =
            BLOCK_ENTITIES.register("lunar_font", LunarFontBlockEntity::new, lunarFont);

    public static final RegistryObject<BlockEntityType<TestimonyStandBlockEntity>> testimonyStandBE =
            BLOCK_ENTITIES.register("testimony_stand", TestimonyStandBlockEntity::new, testimonyStand);

    public static final RegistryObject<BlockEntityType<ArmillaryCrownBlockEntity>> armillaryCrownBE =
            BLOCK_ENTITIES.register("armillary_crown", ArmillaryCrownBlockEntity::new, armillaryCrown);

    private static void addTabItems(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(lunarFont);
        output.accept(astrolabeSpine);
        output.accept(armillaryCrown);
        output.accept(testimonyStand);
    }

    private static Properties selenicProps(int factor) {
        return builder(MapColor.COLOR_PURPLE, SoundType.AMETHYST)
                .instrument(NoteBlockInstrument.CHIME)
                .requiresCorrectToolForDrops()
                .strength(3.0F * factor, 9.0F * factor)
                .noOcclusion();
    }
}