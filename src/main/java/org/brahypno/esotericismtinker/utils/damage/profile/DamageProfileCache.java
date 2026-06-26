package org.brahypno.esotericismtinker.utils.damage.profile;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageProfileCache {
    private static final Map<String, DamageProfile> CACHE = new ConcurrentHashMap<>();

    private DamageProfileCache() {}

    public static DamageProfile profileFor(LivingEntity victim) {
        return CACHE.computeIfAbsent(keyFor(victim), DamageProfile::new);
    }

    public static String keyFor(LivingEntity victim) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        return id + "|" + victim.getClass().getName();
    }

    public static void clear() {
        CACHE.clear();
    }
}
