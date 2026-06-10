package org.brahypno.esotericismtinker.common.data.loot;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class EsotericismTinkerLootTableProvider extends LootTableProvider {
    public EsotericismTinkerLootTableProvider(PackOutput packOutput) {
        super(packOutput, Set.<ResourceLocation>of(), List.of(
                new LootTableProvider.SubProviderEntry(BlockLootTableProvider::new, LootContextParamSets.BLOCK)));
    }
}
