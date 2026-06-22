package org.brahypno.esotericismtinker.world.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.world.worldgen.EsotericismTinkerWorldGen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class EsotericismTinkerDataPackProvider extends DatapackBuiltinEntriesProvider {
    public EsotericismTinkerDataPackProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registries
    ) {
        super(
                output,
                registries,
                EsotericismTinkerWorldGen.build(),
                Set.of(EsotericismTinker.MODID)
        );
    }
}