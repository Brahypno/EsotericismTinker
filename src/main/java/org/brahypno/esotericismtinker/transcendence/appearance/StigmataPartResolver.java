package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

/**
 * Converts an actual ToolPartItem stack into the persistent entry used by Stigmata.
 */
public final class StigmataPartResolver {
    private StigmataPartResolver() {}

    public static @Nullable StigmataEntry resolve(ItemStack stack) {
        if (!(stack.getItem() instanceof ToolPartItem part)){
            return null;
        }
        ResourceLocation partId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (partId == null){
            return null;
        }
        return new StigmataEntry(partId, part.getMaterial(stack).getId(), part.getStatType());
    }
}
