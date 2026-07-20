package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;

/**
 * One stage's selected tool-part identity, material and material-stat type.
 * <p>
 * partId is the registry ID of the ToolPartItem. It is intentionally retained
 * separately from statType because multiple part items may share one stat type.
 */
public record StigmataEntry(ResourceLocation partId, MaterialId materialId, MaterialStatsId statType) {
    private static final String PART = "part";
    private static final String MATERIAL = "material";
    private static final String STAT = "stat";

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString(PART, partId.toString());
        tag.putString(MATERIAL, materialId.toString());
        tag.putString(STAT, statType.toString());
        return tag;
    }

    public static @Nullable StigmataEntry deserialize(CompoundTag tag) {
        ResourceLocation part = ResourceLocation.tryParse(tag.getString(PART));
        MaterialId material = MaterialId.tryParse(tag.getString(MATERIAL));
        MaterialStatsId stat = MaterialStatsId.tryParse(tag.getString(STAT));
        if (part == null || material == null || stat == null){
            return null;
        }
        return new StigmataEntry(part, material, stat);
    }
}
