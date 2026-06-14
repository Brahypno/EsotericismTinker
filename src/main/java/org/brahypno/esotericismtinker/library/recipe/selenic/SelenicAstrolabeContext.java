package org.brahypno.esotericismtinker.library.recipe.selenic;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.library.recipe.MoonPhase;

import java.util.List;

public record SelenicAstrolabeContext(
        int elevation,
        MoonPhase moonPhase,
        boolean night,
        List<ItemStack> crownInputs,
        List<ItemStack> testimonyInputs,
        FluidStack medium
) {}