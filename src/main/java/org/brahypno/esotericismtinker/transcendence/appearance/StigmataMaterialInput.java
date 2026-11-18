package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

/**
 * Material identity, tier, and ritual value contributed by one input item.
 */
public record StigmataMaterialInput(MaterialVariantId variant, MaterialId material, int tier, double unitsPerItem) {
    /**
     * Localized material name from Tinkers' Construct, using the same translation key
     * the material uses everywhere else ({@code material.<namespace>.<path>}).
     */
    public Component displayName() {
        return displayName(material);
    }

    public static Component displayName(MaterialId materialId) {
        ResourceLocation location = materialId.getLocation('.');
        return Component.translatable("material." + location.getNamespace() + "." + location.getPath());
    }
}
