package org.brahypno.esotericismtinker.fluids.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.fluid.tooltip.AbstractFluidTooltipProvider;
import slimeknights.tconstruct.TConstruct;

public class FluidTooltipProvider extends AbstractFluidTooltipProvider {
    public FluidTooltipProvider(PackOutput packOutput) {
        super(packOutput, EsotericismTinker.MODID);
    }

    @Override
    protected void addFluids() {
        addRedirect(EsotericismTinkerSmeltery.Transmute.getId(), new ResourceLocation(TConstruct.MOD_ID, "ingots"));
    }

    @Override
    public @NotNull String getName() {
        return "EsotericismTinker Fluid Tooltip Provider";
    }
}
