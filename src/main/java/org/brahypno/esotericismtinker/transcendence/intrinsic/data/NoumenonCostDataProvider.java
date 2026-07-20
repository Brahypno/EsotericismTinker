package org.brahypno.esotericismtinker.transcendence.intrinsic.data;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonKeys;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Datagen helper for default Noumenon Crown cost JSON.
 * <p>
 * Runtime should load JSON through NoumenonCostJsonLoader. This provider only
 * writes the mod's default data files, so pack authors can override them.
 */
public class NoumenonCostDataProvider implements DataProvider {
    private final PackOutput output;

    public NoumenonCostDataProvider(PackOutput output) {
        this.output = output;
    }

    /**
     * Legacy constructor shape, if your generator setup still passes DataGenerator.
     */
    public NoumenonCostDataProvider(DataGenerator generator) {
        this(generator.getPackOutput());
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Map<ResourceLocation, JsonObject> files = new LinkedHashMap<>();

        addDefaults(new Writer() {
            @Override
            public void reception(String path, String slotType, int cost) {
                JsonObject json = new JsonObject();
                json.addProperty("slot_type", slotType);
                json.addProperty("cost", cost);
                files.put(NoumenonKeys.id("noumenon/reception_slot_costs/" + path), json);
            }

            @Override
            public void investiture(String path, ResourceLocation toolDefinition, Map<ResourceLocation, Integer> traitCosts) {
                JsonObject json = new JsonObject();
                json.addProperty("tool_definition", toolDefinition.toString());
                JsonObject traits = new JsonObject();
                traitCosts.forEach((trait, cost) -> traits.addProperty(trait.toString(), cost));
                json.add("traits", traits);
                files.put(NoumenonKeys.id("noumenon/investiture_trait_costs/" + path), json);
            }
        });

        return CompletableFuture.allOf(files.entrySet().stream()
                                            .map(entry -> DataProvider.saveStable(cache, entry.getValue(), path(entry.getKey())))
                                            .toArray(CompletableFuture[]::new));
    }

    /**
     * Put default JSON data here.
     */
    protected void addDefaults(Writer writer) {
        writer.reception("delusions", "delusions", 3);
        writer.reception("upgrades", "upgrades", 2);
        writer.reception("defense", "defense", 2);
        writer.reception("abilities", "abilities", 4);
        writer.reception("souls", "souls", 1);
    }

    private Path path(ResourceLocation id) {
        return output.getOutputFolder(PackOutput.Target.DATA_PACK)
                     .resolve(id.getNamespace())
                     .resolve(id.getPath() + ".json");
    }

    @Override
    public String getName() {
        return "Noumenon Crown Cost Data";
    }

    public interface Writer {
        void reception(String path, String slotType, int cost);

        void investiture(String path, ResourceLocation toolDefinition, Map<ResourceLocation, Integer> traitCosts);
    }
}
