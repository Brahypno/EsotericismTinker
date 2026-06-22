package org.brahypno.esotericismtinker.common.data.loot;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.minecraft.world.level.storage.loot.BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON;
import static net.minecraft.world.level.storage.loot.BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE;

public class EsotericismTinkerLootTableProvider extends LootTableProvider {
    public EsotericismTinkerLootTableProvider(PackOutput packOutput) {
        super(packOutput, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(BlockLootTableProvider::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(ETSelenicChestLoot::new, LootContextParamSets.CHEST)));
    }

    private static final Set<ResourceLocation> ALLOWED_EXTERNAL_TABLES = Set.of(
            TRAIL_RUINS_ARCHAEOLOGY_COMMON,
            TRAIL_RUINS_ARCHAEOLOGY_RARE
    );

    private static final LootTable DUMMY_EXTERNAL_TABLE = LootTable.lootTable()
                                                                   .setParamSet(LootContextParamSets.CHEST)
                                                                   .build();

    @Override
    protected void validate(
            Map<ResourceLocation, LootTable> tables,
            ValidationContext context
    ) {
        ValidationContext patchedContext = new ValidationContext(
                LootContextParamSets.ALL_PARAMS,
                new LootDataResolver() {
                    @Nullable
                    @Override
                    public <T> T getElement(LootDataId<T> id) {
                        if (id.type() != LootDataType.TABLE){
                            return null;
                        }

                        LootTable generated = tables.get(id.location());
                        if (generated != null){
                            return cast(generated);
                        }

                        if (ALLOWED_EXTERNAL_TABLES.contains(id.location())){
                            return cast(DUMMY_EXTERNAL_TABLE);
                        }

                        return null;
                    }
                }
        );

        tables.forEach((id, table) -> table.validate(
                patchedContext
                        .setParams(table.getParamSet())
                        .enterElement(
                                "{" + id + "}",
                                new LootDataId<>(LootDataType.TABLE, id)
                        )
        ));
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object object) {
        return (T) object;
    }
}
