package org.brahypno.esotericismtinker.common.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.brahypno.esotericismtinker.transcendence.table.EsotericismTinkerTranscendenceTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static net.minecraft.tags.BlockTags.*;
import static slimeknights.tconstruct.common.TinkerTags.Blocks.MINEABLE_MELTING_BLACKLIST;

public class BlockTagProvider extends BlockTagsProvider {
    public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        addTransmute();
        addHarvest();
    }

    private void addHarvest() {
        tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_DIAMOND_TOOL, EsotericismTinkerSelenic.armillaryCrown, EsotericismTinkerSelenic.lunarFont,
                  EsotericismTinkerSelenic.astrolabeSpine, EsotericismTinkerSelenic.testimonyStand);
        tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_IRON_TOOL, EsotericismTinkerTranscendenceTable.transcendenceAnvil);
    }

    private void addTransmute() {
        tag(BlockTags.ENDERMAN_HOLDABLE).add(EsotericismTinkerSmeltery.enderMortar.get());
        tagBlocks(MINEABLE_WITH_SHOVEL, EsotericismTinkerSmeltery.enderMortar);
        tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_DIAMOND_TOOL, EsotericismTinkerSmeltery.ashenBricks, EsotericismTinkerSmeltery.ashenRoad,
                  EsotericismTinkerSmeltery.ashenHeater, EsotericismTinkerSmeltery.ashenAccel);
        tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_DIAMOND_TOOL, EsotericismTinkerSmeltery.ashenStone, EsotericismTinkerSmeltery.polishedAshenStone,
                  EsotericismTinkerSmeltery.ashenLadder, EsotericismTinkerSmeltery.ashenLamp, EsotericismTinkerSmeltery.ashenGlass,
                  EsotericismTinkerSmeltery.ashenSoulGlass, EsotericismTinkerSmeltery.ashenTintedGlass, EsotericismTinkerSmeltery.ashenGlassPane,
                  EsotericismTinkerSmeltery.ashenSoulGlassPane, EsotericismTinkerSmeltery.ashenAlloySwitch, EsotericismTinkerSmeltery.ashenMeltSwitch,
                  EsotericismTinkerSmeltery.ashenDrain, EsotericismTinkerSmeltery.ashenChute, EsotericismTinkerSmeltery.ashenDuct,
                  EsotericismTinkerSmeltery.transmuteController);
        tagBlocks(MINEABLE_WITH_PICKAXE, NEEDS_DIAMOND_TOOL, EsotericismTinkerSmeltery.ashenTank);

        tag(EsotericismTinkerTagKeys.Blocks.ASHEN_BLOCKS)
                .add(EsotericismTinkerSmeltery.ashenStone.get(), EsotericismTinkerSmeltery.polishedAshenStone.get(),
                     EsotericismTinkerSmeltery.ashenBricks.get(),
                     EsotericismTinkerSmeltery.ashenRoad.get(), EsotericismTinkerSmeltery.chiseledAshenBricks.get());
        tag(BlockTags.FENCES).add(EsotericismTinkerSmeltery.ashenBricks.getFence());
        EsotericismTinkerSmeltery.ashenTank.values().forEach(tag(EsotericismTinkerTagKeys.Blocks.ASHEN_TANKS)::add);
        tag(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_ACCEL).add(EsotericismTinkerSmeltery.ashenAccel.get());
        tag(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_HEATER).add(EsotericismTinkerSmeltery.ashenHeater.get());
        tag(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_ALLOY_SWITCH).add(EsotericismTinkerSmeltery.ashenAlloySwitch.get());
        tag(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_MELTING_SWITCH).add(EsotericismTinkerSmeltery.ashenMeltSwitch.get());

        tag(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_WALL)
                .addTags(EsotericismTinkerTagKeys.Blocks.ASHEN_BLOCKS, EsotericismTinkerTagKeys.Blocks.TRANSMUTE_ACCEL,
                         EsotericismTinkerTagKeys.Blocks.TRANSMUTE_MELTING_SWITCH, EsotericismTinkerTagKeys.Blocks.ASHEN_TANKS)
                .add(EsotericismTinkerSmeltery.ashenGlass.get(), EsotericismTinkerSmeltery.ashenSoulGlass.get(),
                     EsotericismTinkerSmeltery.ashenTintedGlass.get(),
                     EsotericismTinkerSmeltery.ashenLadder.get(), EsotericismTinkerSmeltery.ashenLamp.get(), EsotericismTinkerSmeltery.ashenDrain.get(),
                     EsotericismTinkerSmeltery.ashenChute.get(), EsotericismTinkerSmeltery.ashenDuct.get());
        tag(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_FLOOR)
                .addTags(EsotericismTinkerTagKeys.Blocks.ASHEN_BLOCKS, EsotericismTinkerTagKeys.Blocks.TRANSMUTE_MELTING_SWITCH,
                         EsotericismTinkerTagKeys.Blocks.TRANSMUTE_HEATER)
                .add(EsotericismTinkerSmeltery.ashenGlass.get(), EsotericismTinkerSmeltery.ashenSoulGlass.get(),
                     EsotericismTinkerSmeltery.ashenTintedGlass.get(),
                     EsotericismTinkerSmeltery.ashenLadder.get(), EsotericismTinkerSmeltery.ashenLamp.get(), EsotericismTinkerSmeltery.ashenDrain.get(),
                     EsotericismTinkerSmeltery.ashenChute.get(), EsotericismTinkerSmeltery.ashenDuct.get());
        tag(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_TANKS).addTag(EsotericismTinkerTagKeys.Blocks.ASHEN_TANKS);
        tag(EsotericismTinkerTagKeys.Blocks.TRANSMUTE)
                .addTags(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_WALL, EsotericismTinkerTagKeys.Blocks.TRANSMUTE_FLOOR,
                         EsotericismTinkerTagKeys.Blocks.TRANSMUTE_TANKS);
        tag(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_BLOCKS).addTag(EsotericismTinkerTagKeys.Blocks.ASHEN_BLOCKS);

        tag(TinkerTags.Blocks.FUEL_TANKS).addTag(EsotericismTinkerTagKeys.Blocks.ASHEN_TANKS);
        tag(TinkerTags.Blocks.ALLOYER_TANKS).addTag(EsotericismTinkerTagKeys.Blocks.ASHEN_TANKS);
        tag(CLIMBABLE).add(EsotericismTinkerSmeltery.ashenLadder.get());
        tag(IMPERMEABLE).add(EsotericismTinkerSmeltery.ashenGlass.get(), EsotericismTinkerSmeltery.ashenSoulGlass.get(),
                             EsotericismTinkerSmeltery.ashenTintedGlass.get());
        tag(BlockTags.SOUL_SPEED_BLOCKS).add(EsotericismTinkerSmeltery.ashenSoulGlass.get(), EsotericismTinkerSmeltery.ashenSoulGlassPane.get());
        tag(BlockTags.SOUL_FIRE_BASE_BLOCKS).add(EsotericismTinkerSmeltery.ashenSoulGlass.get());
        tag(TinkerTags.Blocks.TRANSPARENT_OVERLAY).add(EsotericismTinkerSmeltery.ashenSoulGlass.get(), EsotericismTinkerSmeltery.ashenSoulGlassPane.get());
        tagBlocks(MINEABLE_MELTING_BLACKLIST, EsotericismTinkerSmeltery.transmuteController);
        tagBlocks(MINEABLE_MELTING_BLACKLIST, EsotericismTinkerSmeltery.ashenTank);
    }

    @SafeVarargs
    private void tagBlocks(TagKey<Block> tag, Supplier<? extends Block>... blocks) {
        var appender = tag(tag);
        for (Supplier<? extends Block> block : blocks) {
            appender.add(block.get());
        }
    }

    @SafeVarargs
    private void tagBlocks(TagKey<Block> tag1, TagKey<Block> tag2, Supplier<? extends Block>... blocks) {
        tagBlocks(tag1, blocks);
        tagBlocks(tag2, blocks);
    }

    @SafeVarargs
    private void tagBlocks(TagKey<Block> tag, BuildingBlockObject... blocks) {
        var appender = tag(tag);
        for (BuildingBlockObject block : blocks) {
            block.values().forEach(appender::add);
        }
    }

    @SafeVarargs
    private void tagBlocks(TagKey<Block> tag1, TagKey<Block> tag2, BuildingBlockObject... blocks) {
        tagBlocks(tag1, blocks);
        tagBlocks(tag2, blocks);
    }

    @SafeVarargs
    private void tagBlocks(TagKey<Block> tag1, TagKey<Block> tag2, EnumObject<?, ? extends Block>... blocks) {
        tagBlocks(tag1, blocks);
        tagBlocks(tag2, blocks);
    }

    @SafeVarargs
    private void tagBlocks(TagKey<Block> tag, EnumObject<?, ? extends Block>... blocks) {
        IntrinsicTagAppender<Block> appender = this.tag(tag);
        for (EnumObject<?, ? extends Block> block : blocks) {
            block.forEach(b -> appender.add(b));
        }
    }
}
