package org.brahypno.esotericismtinker.utils.LootHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Caches resource definitions only. Entity conditions and random rolls stay live.
 * Returned JSON is shared and must be treated as read-only by the scanners.
 */
public final class LootResourceCache {
    private static final int MAX_JSON_ENTRIES = 1024;
    private static final ResourceLocation GLOBAL_MODIFIERS =
            new ResourceLocation("forge", "loot_modifiers/global_loot_modifiers.json");
    private static final Map<ResourceManager, Entry> CACHE = new WeakHashMap<>();

    private LootResourceCache() {}

    @Nullable
    static synchronized JsonObject getJson(ResourceManager manager, ResourceLocation fileId) {
        Entry entry = CACHE.computeIfAbsent(manager, ignored -> new Entry());
        return entry.json.computeIfAbsent(fileId, id -> readJson(manager, id)).orElse(null);
    }

    static synchronized List<ResourceLocation> getGlobalModifierIds(ResourceManager manager) {
        Entry entry = CACHE.computeIfAbsent(manager, ignored -> new Entry());
        if (entry.globalModifierIds == null){
            Set<ResourceLocation> enabled = new LinkedHashSet<>();
            for (Resource resource : manager.getResourceStack(GLOBAL_MODIFIERS)) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);
                    if (LootScanCommon.getBoolean(json, "replace", false)){
                        enabled.clear();
                    }
                    JsonArray entries = json.has("entries") && json.get("entries").isJsonArray()
                                        ? json.getAsJsonArray("entries") : new JsonArray();
                    for (JsonElement element : entries) {
                        if (element.isJsonPrimitive()){
                            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                            if (id != null){
                                enabled.add(id);
                            }
                        }
                    }
                }
                catch (Exception ignored) {}
            }
            entry.globalModifierIds = List.copyOf(enabled);
        }
        return entry.globalModifierIds;
    }

    private static Optional<JsonObject> readJson(ResourceManager manager, ResourceLocation fileId) {
        Optional<Resource> resource = manager.getResource(fileId);
        if (resource.isEmpty()){
            return Optional.empty();
        }
        try (Reader reader = resource.get().openAsReader()) {
            return Optional.of(GsonHelper.parse(reader));
        }
        catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /** Called after resource reload and when the server starts or stops. */
    public static synchronized void clear() {
        CACHE.clear();
    }

    private static final class Entry {
        // Values contain no ResourceManager, Resource, server, or entity references.
        private final Map<ResourceLocation, Optional<JsonObject>> json =
                new LinkedHashMap<>(16, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<ResourceLocation, Optional<JsonObject>> eldest) {
                        return size() > MAX_JSON_ENTRIES;
                    }
                };
        @Nullable
        private List<ResourceLocation> globalModifierIds;
    }
}
