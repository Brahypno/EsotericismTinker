package org.brahypno.esotericismtinker.common.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.esotericismtinker.common.EsotericismTinkerCommon;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys.Blocks;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys.Items;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerTools;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.concurrent.CompletableFuture;

import static slimeknights.tconstruct.common.TinkerTags.Items.*;

public class ItemTagProvider extends ItemTagsProvider {
    public ItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, String modId, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(TinkerTags.Items.TINKERS_GUIDES)
            .add(EsotericismTinkerCommon.hypnagogic_transmute.get());

        tag(Tags.Items.INGOTS).add(EsotericismTinkerSmeltery.ashenBrick.get());
        copy(EsotericismTinkerTagKeys.Blocks.ASHEN_BLOCKS, EsotericismTinkerTagKeys.Items.ASHEN_BLOCKS);
        copy(Blocks.ASHEN_TANKS, Items.ASHEN_TANKS);
        copy(Blocks.TRANSMUTE_BLOCKS, Items.TRANSMUTE_BLOCKS);
        copy(Blocks.TRANSMUTE_HEATER, Items.TRANSMUTE_HEATER);
        copy(Blocks.TRANSMUTE_ACCEL, Items.TRANSMUTE_ACCEL);
        tag(Items.TRANSMUTE)
                .addTag(Items.ASHEN_BLOCKS)
                .addTag(Items.ASHEN_TANKS)
                .addTag(Items.TRANSMUTE_HEATER)
                .addTag(Items.TRANSMUTE_ACCEL)
                .add(EsotericismTinkerSmeltery.transmuteController.asItem(), EsotericismTinkerSmeltery.ashenLadder.asItem(),
                     EsotericismTinkerSmeltery.ashenDrain.asItem(), EsotericismTinkerSmeltery.ashenChute.asItem(), EsotericismTinkerSmeltery.ashenDuct.asItem(),
                     EsotericismTinkerSmeltery.ashenGlass.asItem(), EsotericismTinkerSmeltery.ashenSoulGlass.asItem(),
                     EsotericismTinkerSmeltery.ashenTintedGlass.asItem());
        addItemsTags(EsotericismTinkerTools.ritual_blade, MULTIPART_TOOL, DURABILITY, HARVEST, MELEE_PRIMARY, INTERACTABLE_RIGHT, SMALL_TOOLS, BONUS_SLOTS,
                     ItemTags.SWORDS, Items.EsotericismTinkerTools);
    }

    @SafeVarargs
    private void addItemsTags(ItemLike tool, TagKey<Item>... tags) {
        Item item = tool.asItem();
        for (TagKey<Item> tag : tags) {
            this.tag(tag).add(item);
        }
    }
}
