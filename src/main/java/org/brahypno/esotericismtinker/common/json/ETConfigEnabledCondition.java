package org.brahypno.esotericismtinker.common.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class ETConfigEnabledCondition implements ICondition {
    public static final ResourceLocation ID = EsotericismTinker.getLocation("mod_compact_config");
    public static final ConfigSerializer SERIALIZER = new ConfigSerializer();
    /* Map of config names to condition cache */
    private static final Map<String, ETConfigEnabledCondition> PROPS = new HashMap<>();

    private final String configName;

    public ETConfigEnabledCondition(String modid) {
        configName = modid;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return !EsotericismTinker.configCompactDisabled(configName);
    }

    private static class ConfigSerializer implements Serializer<ETConfigEnabledCondition>, IConditionSerializer<ETConfigEnabledCondition> {
        @Override
        public ResourceLocation getID() {
            return ID;
        }

        @Override
        public void write(JsonObject json, ETConfigEnabledCondition value) {
            json.addProperty("modid", value.configName);
        }

        @Override
        public ETConfigEnabledCondition read(JsonObject json) {
            String modid = GsonHelper.getAsString(json, "modid");
            return new ETConfigEnabledCondition(modid);
        }

        @Override
        public void serialize(JsonObject json, ETConfigEnabledCondition condition, JsonSerializationContext context) {
            write(json, condition);
        }

        @Override
        public @NotNull ETConfigEnabledCondition deserialize(JsonObject json, JsonDeserializationContext context) {
            return read(json);
        }
    }

    /**
     * Adds a condition
     *
     * @param modid modiderty name
     * @return Added condition
     */
    public static ETConfigEnabledCondition add(String modid) {
        ETConfigEnabledCondition conf = new ETConfigEnabledCondition(modid);
        PROPS.put(modid.toLowerCase(Locale.ROOT), conf);
        return conf;
    }


    @Override
    public String toString() {
        return "config_setting_enabled(\"" + this.configName + "\")";
    }

}
