package org.brahypno.esotericismtinker.fluids.data;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.data.PackOutput;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.fluids.EsotericismTinkerFluids;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.fluid.texture.AbstractFluidTextureProvider;
import slimeknights.mantle.fluid.texture.FluidTexture;
import slimeknights.mantle.registration.object.FluidObject;

import java.util.Objects;

import static slimeknights.tconstruct.TConstruct.getResource;

public class EsotericismTinkerFluidTextureProvider extends AbstractFluidTextureProvider {
    public EsotericismTinkerFluidTextureProvider(PackOutput packOutput) {
        super(packOutput, EsotericismTinker.MODID);
    }

    @Override
    public void addTextures() {
        tintedStone(EsotericismTinkerFluids.molten_ender_ash).color(0xFFAA87CD);
        commonFluid(EsotericismTinkerFluids.blood_soul.getType());
    }

    private FluidTexture.Builder tintedStone(FluidObject<?> fluid) {
        return texture(fluid).root(getResource("fluid/molten/stone/"))
                             .still().flowing().camera().calculateFogColor(true).fog(FogShape.SPHERE, 0.25f, 2);
    }

    public void commonFluid(FluidType fluid) {
        super.texture(fluid)
             .textures(EsotericismTinker.getLocation("fluid/" + Objects.requireNonNull(ForgeRegistries.FLUID_TYPES.get().getKey(fluid)).getPath() + "/"), false,
                       false);
    }

    @Override
    public @NotNull String getName() {
        return "EsotericismTinker Fluid Texture Provider";
    }
}
