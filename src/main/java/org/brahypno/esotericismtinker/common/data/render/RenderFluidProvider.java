package org.brahypno.esotericismtinker.common.data.render;

import net.minecraft.data.PackOutput;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.data.datamap.BlockStateDataMapProvider;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;

import java.util.List;

public class RenderFluidProvider extends BlockStateDataMapProvider<List<FluidCuboid>> {
    public RenderFluidProvider(PackOutput output) {
        super(output, PackOutput.Target.RESOURCE_PACK, FluidCuboid.REGISTRY, EsotericismTinker.MODID);
    }

    @Override
    protected void addEntries() {
        // tanks
        String tank = "templates/tank";
        /*
        entry(TConstruct.getResource(tank), List.of(
                FluidCuboid.builder()
                           .from(0.08f, 0.08f, 0.08f)
                           .to(15.92f, 15.92f, 15.92f)
                           .build()));

         */
        for (SearedTankBlock.TankType type : SearedTankBlock.TankType.values()) {
            block(EsotericismTinkerSmeltery.ashenTank.get(type)).variant(TConstruct.getResource(tank));
        }
    }

    @Override
    public @NotNull String getName() {
        return "Dream Tinkers' block render fluid provider";
    }
}
