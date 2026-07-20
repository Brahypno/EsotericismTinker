package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public record NoumenonDisplay(
        Component name,
        List<Component> description,
        Supplier<ItemStack> icon,
        ResourceLocation category,
        int order
) {}
