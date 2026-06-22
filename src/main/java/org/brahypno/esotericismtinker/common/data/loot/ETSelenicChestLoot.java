package org.brahypno.esotericismtinker.common.data.loot;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

import static net.minecraft.world.level.storage.loot.BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON;
import static net.minecraft.world.level.storage.loot.BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE;
import static org.brahypno.esotericismtinker.EsotericismTinker.MODID;

public class ETSelenicChestLoot implements net.minecraft.data.loot.LootTableSubProvider {
    public static final ResourceLocation TRAIL_RUINS_CHEST =
            new ResourceLocation(MODID, "chests/trail_ruins_chest");

    @Override
    public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
        output.accept(TRAIL_RUINS_CHEST, LootTable.lootTable()
                                                  .withPool(LootPool.lootPool()
                                                                    .setRolls(UniformGenerator.between(5.0F, 8.0F))
                                                                    .add(LootTableReference.lootTableReference(TRAIL_RUINS_ARCHAEOLOGY_COMMON))
                                                  )
                                                  .withPool(LootPool.lootPool()
                                                                    .setRolls(BinomialDistributionGenerator.binomial(6, 0.45F))
                                                                    .add(LootTableReference.lootTableReference(TRAIL_RUINS_ARCHAEOLOGY_RARE))
                                                  )
        );
    }
}
