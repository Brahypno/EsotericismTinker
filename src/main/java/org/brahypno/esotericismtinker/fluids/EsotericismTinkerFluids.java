package org.brahypno.esotericismtinker.fluids;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.EsotericismTinkerModule;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerTools;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.tconstruct.fluids.block.BurningLiquidBlock;

import java.util.function.Function;
import java.util.function.Supplier;

public class EsotericismTinkerFluids extends EsotericismTinkerModule {
    public static final RegistryObject<CreativeModeTab> tabFluids = TABS.register(
            "fluids", () -> CreativeModeTab.builder().title(EsotericismTinker.makeTranslation("itemGroup", "fluids"))
                                           .icon(() -> new ItemStack(EsotericismTinkerFluids.blood_soul))
                                           .displayItems(EsotericismTinkerFluids::addTabItems)
                                           .withTabsBefore(EsotericismTinkerTools.TOOL.getId())
                                           .withSearchBar()
                                           .build());
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_ender_ash =
            registerFluid(FLUIDS, "molten_ender_ash", 1800, 1000, 10000, 10,
                          supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_PURPLE, 2), 0, 0) {});
    public static final FlowingFluidObject<ForgeFlowingFluid> blood_soul =
            registerFluid(FLUIDS, "blood_soul", 37, 100, 10, 7,
                          supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.CRIMSON_NYLIUM, 7), 0, 0) {});

    public static FluidType.Properties createFluidType(int temperature, int lightLevel, int viscosity, int density) {
        return FluidType.Properties.create()
                                   .temperature(temperature)
                                   .lightLevel(lightLevel)
                                   .viscosity(viscosity)
                                   .density(density)
                                   .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                                   .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA);
    }

    private static FlowingFluidObject<ForgeFlowingFluid> registerFluid(FluidDeferredRegister register, String name, int temp, int viscosity, int density, int lightLevel, Function<Supplier<? extends FlowingFluid>, LiquidBlock> blockFunction) {
        return register.register(name).type(createFluidType(temp, lightLevel, viscosity, density)).block(blockFunction).bucket().flowing();
    }

    private static void addTabItems(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
        output.accept(molten_ender_ash);
        output.accept(blood_soul);
    }
}
