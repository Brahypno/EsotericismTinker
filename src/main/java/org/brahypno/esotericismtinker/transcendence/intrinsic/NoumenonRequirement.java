package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

import java.util.List;

public record NoumenonRequirement(
        List<TagKey<Item>> all,
        List<TagKey<Item>> any,
        List<TagKey<Item>> none
) {
    public static NoumenonRequirement unrestricted() {
        return new NoumenonRequirement(List.of(), List.of(), List.of());
    }

    public boolean matches(IToolContext context) {
        for (TagKey<Item> tag : all) {
            if (!context.hasTag(tag))
                return false;
        }
        if (!any.isEmpty()){
            boolean found = false;
            for (TagKey<Item> tag : any) {
                if (context.hasTag(tag)){
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        for (TagKey<Item> tag : none) {
            if (context.hasTag(tag))
                return false;
        }
        return true;
    }
}
