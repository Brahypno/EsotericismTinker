package org.brahypno.esotericismtinker.tools.data;

import net.minecraft.data.PackOutput;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.fluids.EsotericismTinkerFluids;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.data.tinkering.AbstractFluidEffectProvider;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.fluid.block.BlockInteractFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.DamageFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidMobEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.RandomTeleportFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.TimeAction;
import slimeknights.tconstruct.library.recipe.FluidValues;

public class EsotericismTinkerFluidEffectProvider extends AbstractFluidEffectProvider {
    public EsotericismTinkerFluidEffectProvider(PackOutput packOutput) {
        super(packOutput, EsotericismTinker.MODID);
    }

    @Override
    protected void addFluids() {
        addFluid(EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK)
                .addDamage(2.0f, new DamageFluidEffect.DamageTypePair(DamageTypes.FELL_OUT_OF_WORLD, DamageTypes.FELL_OUT_OF_WORLD))
                .addEntityEffect(new RandomTeleportFluidEffect(LevelingInt.eachLevel(6), LevelingInt.eachLevel(5)))
                .addBlockEffect(BlockInteractFluidEffect.INSTANCE);
        addFluid(EsotericismTinkerFluids.blood_soul, 20)
                .addEntityEffects(FluidMobEffect.builder().effect(MobEffects.HEAL, 100, 2).buildEntity(TimeAction.ADD));
    }

    @Override
    public @NotNull String getName() {
        return "Esotericism Fluid Texture Provider";
    }
}
