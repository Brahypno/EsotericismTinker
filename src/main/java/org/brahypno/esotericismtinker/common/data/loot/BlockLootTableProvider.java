package org.brahypno.esotericismtinker.common.data.loot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.loot.function.RetexturedLootFunction;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.tconstruct.library.utils.NBTTags;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BlockLootTableProvider extends BlockLootSubProvider {
    protected BlockLootTableProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return BuiltInRegistries.BLOCK.stream()
                                      .filter(block -> EsotericismTinker.MODID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace()))
                                      .collect(Collectors.toList());
    }

    @Override
    protected void generate() {
        dropSelf(EsotericismTinkerSmeltery.ashenStone.get());
        dropSelf(EsotericismTinkerSmeltery.polishedAshenStone.get());
        registerFenceBuildingLootTables(EsotericismTinkerSmeltery.ashenBricks);
        dropSelf(EsotericismTinkerSmeltery.chiseledAshenBricks.get());
        registerBuildingLootTables(EsotericismTinkerSmeltery.ashenRoad);
        dropSelf(EsotericismTinkerSmeltery.ashenHeater.get());
        dropSelf(EsotericismTinkerSmeltery.ashenAccel.get());
        dropSelf(EsotericismTinkerSmeltery.ashenLadder.get());
        dropSelf(EsotericismTinkerSmeltery.ashenGlass.get());
        dropSelf(EsotericismTinkerSmeltery.ashenSoulGlass.get());
        dropSelf(EsotericismTinkerSmeltery.ashenTintedGlass.get());
        dropSelf(EsotericismTinkerSmeltery.ashenGlassPane.get());
        dropSelf(EsotericismTinkerSmeltery.ashenSoulGlassPane.get());
        dropTable(EsotericismTinkerSmeltery.ashenDrain.get());
        dropTable(EsotericismTinkerSmeltery.ashenChute.get());
        dropTable(EsotericismTinkerSmeltery.ashenDuct.get());
        Function<Block, LootTable.Builder> dropTank = block -> droppingWithFunctions(block, builder ->
                builder.apply(COPY_NAME)
                       .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY).copy(NBTTags.TANK, NBTTags.TANK)));
        EsotericismTinkerSmeltery.ashenTank.forEach(block -> add(block, dropTank));
        dropSelf(EsotericismTinkerSmeltery.ashenLamp.get());
        dropSelf(EsotericismTinkerSmeltery.enderMortar.get());
        dropTable(EsotericismTinkerSmeltery.transmuteController.get());
        dropSelf(EsotericismTinkerSmeltery.ashenAlloySwitch.get());
        dropSelf(EsotericismTinkerSmeltery.ashenMeltSwitch.get());
    }

    private void registerFenceBuildingLootTables(FenceBuildingBlockObject object) {
        registerBuildingLootTables(object);
        dropSelf(object.getFence());
    }

    private void registerBuildingLootTables(BuildingBlockObject object) {
        dropSelf(object.get());
        add(object.getSlab(), this::createSlabItemTable);
        dropSelf(object.getStairs());
    }

    private LootTable.Builder droppingWithFunctions(Block block, Function<LootItem.Builder<?>, LootItem.Builder<?>> mapping) {
        return LootTable.lootTable().withPool(
                applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(mapping.apply(LootItem.lootTableItem(block)))));
    }

    private final LootItemFunction.Builder COPY_NAME = CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY);
    private final Function<Block, LootTable.Builder> ADD_TABLE = block -> droppingWithFunctions(block, builder ->
            builder.apply(COPY_NAME).apply(RetexturedLootFunction::new));

    private void dropTable(Block table) {
        add(table, ADD_TABLE);
    }
}
