package org.brahypno.esotericismtinker.transcendence.intrinsic.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonCostDatabase;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads data-driven Noumenon Crown costs.
 * <p>
 * Folder layout:
 * data/&lt;namespace&gt;/noumenon/reception_slot_costs/*.json
 * data/&lt;namespace&gt;/noumenon/investiture_trait_costs/*.json
 * <p>
 * Register this listener from AddReloadListenerEvent.
 */
public class NoumenonCostJsonLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String FOLDER = "noumenon";

    public NoumenonCostJsonLoader() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, Integer> receptionCosts = new LinkedHashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, Integer>> investitureCosts = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
            ResourceLocation file = entry.getKey();
            String path = file.getPath();
            JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), file.toString());

            try {
                if (path.startsWith("reception_slot_costs/")){
                    readReceptionCost(file, json, receptionCosts);
                }else if (path.startsWith("investiture_trait_costs/")){
                    readInvestitureCosts(file, json, investitureCosts);
                }
            }
            catch (RuntimeException ex) {
                throw new JsonSyntaxException("Failed loading Noumenon cost file " + file + ": " + ex.getMessage(), ex);
            }
        }

        NoumenonCostDatabase.replaceReceptionSlotCosts(receptionCosts);
        NoumenonCostDatabase.replaceInvestitureTraitCosts(investitureCosts);
    }

    private static void readReceptionCost(ResourceLocation file, JsonObject json, Map<String, Integer> receptionCosts) {
        String slotType = GsonHelper.getAsString(json, "slot_type");
        int cost = GsonHelper.getAsInt(json, "cost");
        if (slotType.isBlank())
            throw new JsonSyntaxException("slot_type cannot be blank in " + file);
        if (cost < 0)
            throw new JsonSyntaxException("cost cannot be negative in " + file);
        receptionCosts.put(slotType, cost);
    }

    private static void readInvestitureCosts(
            ResourceLocation file,
            JsonObject json,
            Map<ResourceLocation, Map<ResourceLocation, Integer>> investitureCosts
    ) {
        ResourceLocation toolDefinition = new ResourceLocation(GsonHelper.getAsString(json, "tool_definition"));
        JsonObject traits = GsonHelper.getAsJsonObject(json, "traits");

        Map<ResourceLocation, Integer> map = investitureCosts.computeIfAbsent(toolDefinition, ignored -> new LinkedHashMap<>());
        for (Map.Entry<String, JsonElement> traitEntry : traits.entrySet()) {
            ResourceLocation trait = new ResourceLocation(traitEntry.getKey());
            int cost = GsonHelper.convertToInt(traitEntry.getValue(), traitEntry.getKey());
            if (cost < 0)
                throw new JsonSyntaxException("Trait cost cannot be negative: " + traitEntry.getKey() + " in " + file);
            map.put(trait, cost);
        }
    }
}
