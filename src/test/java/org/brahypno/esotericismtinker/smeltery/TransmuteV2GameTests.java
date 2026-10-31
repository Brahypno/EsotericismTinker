package org.brahypno.esotericismtinker.smeltery;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.smeltery.block.component.AshenButtonBlock;
import org.brahypno.esotericismtinker.smeltery.block.entity.component.TransmuteComponentBlockEntity;
import org.brahypno.esotericismtinker.smeltery.block.entity.controller.TransmuteBlockEntity;
import org.brahypno.esotericismtinker.smeltery.block.entity.module.SuperByproductMeltingModuleInventory;
import org.brahypno.esotericismtinker.smeltery.block.entity.module.SuperByproductMeltingModuleInventory.MeltType;
import org.brahypno.esotericismtinker.smeltery.data.TransmuteMeltingRecipeBuilder;
import org.brahypno.esotericismtinker.smeltery.recipe.melting.TransmuteMeltingRecipe;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeLookup;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.component.SearedBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("esotericism_tinker")
@PrefixGameTestTemplate(false)
public final class TransmuteV2GameTests {
    @GameTest(template = "selenic_empty", timeoutTicks = 160)
    public static void formedFurnaceUsesAllFourSwitchModes(GameTestHelper helper) {
        helper.assertTrue(AshenButtonBlock.Function_Set.getPossibleValues().size() == 4,
                "The switch must expose exactly four states");
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(3, 3, 3))) {
            if (!pos.equals(new BlockPos(2, 2, 2))) {
                helper.setBlock(pos, EsotericismTinkerSmeltery.ashenBricks.get());
            }
        }
        BlockPos controllerPos = new BlockPos(2, 2, 1);
        BlockPos switchPos = new BlockPos(3, 2, 2);
        helper.setBlock(controllerPos, EsotericismTinkerSmeltery.transmuteController.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(switchPos, EsotericismTinkerSmeltery.ashenMeltSwitch.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(2, 2, 3), EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.FUEL_TANK));

        var sequence = helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(helper.getBlockState(controllerPos).getValue(SearedBlock.IN_STRUCTURE),
                    "The furnace must form with a melt switch in its wall");
            helper.assertTrue(helper.getBlockEntity(switchPos) instanceof TransmuteComponentBlockEntity,
                    "The switch must use a valid Transmute component entity");
        });
        for (int step = 1; step <= 4; step++) {
            final int expectedMode = step % 4;
            sequence.thenExecute(() -> {
                BlockPos absolutePos = helper.absolutePos(switchPos);
                var state = helper.getBlockState(switchPos);
                state.use(helper.getLevel(), FakePlayerFactory.getMinecraft(helper.getLevel()), InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.EAST, absolutePos, false));
                helper.assertTrue(helper.getBlockState(switchPos).getValue(AshenButtonBlock.Function_Set) == expectedMode,
                        "Right click must select mode " + expectedMode);
            }).thenExecuteAfter(5, () -> {
                var furnace = (TransmuteBlockEntity) helper.getBlockEntity(controllerPos);
                var inventory = inventory(furnace);
                helper.assertTrue(inventory.writeToTag().getInt("melt_type") == expectedMode,
                        "The formed furnace must receive mode " + expectedMode + " from the switch");
                if (expectedMode == 3) {
                    inventory.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
                    melt(inventory);
                    helper.assertTrue(amount(furnace.getTank(), Fluids.WATER) == 90,
                            "The fourth switch mode must actually process the special v2 recipe");
                    helper.assertTrue(amount(furnace.getTank(), TinkerFluids.moltenGold.get()) == 0,
                            "The fourth switch mode must not finish the ordinary gold recipe");
                }
            });
        }
        sequence.thenSucceed();
    }

    @GameTest(template = "selenic_empty")
    public static void specialRecipeWinsAndKeepsAllOutputAmounts(GameTestHelper helper) {
        TransmuteBlockEntity furnace = furnace(helper);
        SuperByproductMeltingModuleInventory inventory = inventory(furnace);
        inventory.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 2));
        int standardTemperature = inventory.getRequiredTemp(0);
        inventory.getModule(0).set(0, 9999);
        inventory.updateMeltType(MeltType.Transmute_v2);
        helper.assertTrue(inventory.getCurrentTime(0) == 0, "Entering v2 must reset old progress");
        helper.assertTrue(inventory.getRequiredTemp(0) == 800 && inventory.getRequiredTime(0) == 600,
                "The special recipe must override the cached standard recipe's temperature and time");
        inventory.heatItems(799, 10000);
        helper.assertTrue(inventory.getCurrentTime(0) == 0, "V2 must respect its required temperature");
        melt(inventory);
        helper.assertTrue(inventory.getStackInSlot(0).isEmpty(), "V2 must consume the input stack");
        helper.assertTrue(amount(furnace.getTank(), Fluids.WATER) == 180, "The main output must not be inverted");
        helper.assertTrue(amount(furnace.getTank(), Fluids.LAVA) == 20, "The first byproduct must scale with input count");
        helper.assertTrue(amount(furnace.getTank(), TinkerFluids.moltenCopper.get()) == 40,
                "Every byproduct must retain its original amount");
        helper.assertTrue(amount(furnace.getTank(), TinkerFluids.moltenGold.get()) == 0,
                "A matching special recipe must suppress the standard gold output");
        inventory.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        inventory.updateMeltType(MeltType.Foundry);
        helper.assertTrue(inventory.getRequiredTemp(0) == standardTemperature,
                "Leaving v2 must restore the standard recipe requirements");
        melt(inventory);
        helper.assertTrue(amount(furnace.getTank(), TinkerFluids.moltenGold.get()) == 90,
                "The standard gold recipe must remain available outside v2");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void missingSpecialRecipeFallsBackToTinkers(GameTestHelper helper) {
        TransmuteBlockEntity furnace = furnace(helper);
        SuperByproductMeltingModuleInventory inventory = inventory(furnace);
        inventory.updateMeltType(MeltType.Transmute_v2);
        inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        var standard = helper.getLevel().getRecipeManager().getRecipeFor(
                TinkerRecipeTypes.MELTING.get(), inventory.getModule(0), helper.getLevel()).orElseThrow();
        var expected = standard.getOutput(inventory.getModule(0));
        helper.assertTrue(inventory.getRequiredTime(0) == standard.getTime(inventory.getModule(0)) * 10,
                "Fallback must retain the standard recipe's time");
        melt(inventory);
        helper.assertTrue(inventory.getStackInSlot(0).isEmpty(), "An ordinary item must still melt in v2");
        helper.assertTrue(amount(furnace.getTank(), expected.getFluid()) == expected.getAmount(),
                "Fallback must produce the original Tinkers output");
        inventory.setStackInSlot(0, new ItemStack(Items.DEBUG_STICK));
        melt(inventory);
        helper.assertTrue(amount(furnace.getTank(), Fluids.WATER) == 120 && amount(furnace.getTank(), Fluids.LAVA) == 30,
                "Fallback recipes must also retain their main and byproduct amounts without inversion");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void tinkersFurnaceAndModifierLookupStayUnchanged(GameTestHelper helper) {
        SmelteryBlockEntity smeltery = new SmelteryBlockEntity(helper.absolutePos(BlockPos.ZERO),
                TinkerSmeltery.smelteryController.get().defaultBlockState());
        smeltery.setLevel(helper.getLevel());
        smeltery.getTank().setCapacity(4000);
        MeltingModuleInventory inventory = smeltery.getMeltingInventory();
        inventory.resize(1, stack -> {});
        inventory.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        melt(inventory);
        helper.assertTrue(amount(smeltery.getTank(), TinkerFluids.moltenGold.get()) == 90,
                "A Tinkers smeltery must still melt gold into molten gold");
        helper.assertTrue(amount(smeltery.getTank(), Fluids.WATER) == 0,
                "Special v2 recipes must not leak into Tinkers smelteries");
        helper.assertTrue(MeltingRecipeLookup.findResult(Items.GOLD_INGOT, 10000).getFluid() == TinkerFluids.moltenGold.get(),
                "Special recipes must not change the modifier melting lookup");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void savedV2ProgressRestoresTheSameRecipe(GameTestHelper helper) {
        TransmuteBlockEntity original = furnace(helper);
        SuperByproductMeltingModuleInventory inventory = inventory(original);
        inventory.updateMeltType(MeltType.Transmute_v2);
        inventory.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        inventory.heatItems(10000, 300);
        CompoundTag saved = inventory.writeToTag();

        TransmuteBlockEntity restored = furnace(helper);
        SuperByproductMeltingModuleInventory restoredInventory = inventory(restored);
        restoredInventory.readFromTag(saved);
        helper.assertTrue(restoredInventory.isTransmuteV2() && restoredInventory.getCurrentTime(0) == 300,
                "Saving must retain the selected recipe type and progress");
        melt(restoredInventory);
        helper.assertTrue(amount(restored.getTank(), Fluids.WATER) == 90,
                "Restored v2 progress must finish the special recipe");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void blockedV2OutputRetriesDuringCooling(GameTestHelper helper) {
        TransmuteBlockEntity furnace = furnace(helper);
        SuperByproductMeltingModuleInventory inventory = inventory(furnace);
        inventory.updateMeltType(MeltType.Transmute_v2);
        furnace.getTank().setCapacity(0);
        inventory.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        melt(inventory);
        helper.assertTrue(inventory.getStackInSlot(0).is(Items.GOLD_INGOT) && inventory.getCurrentTime(0) == -1,
                "A full tank must keep the input and wait for output space");
        furnace.getTank().setCapacity(4000);
        inventory.coolItems();
        helper.assertTrue(inventory.getStackInSlot(0).isEmpty() && amount(furnace.getTank(), Fluids.WATER) == 90,
                "A completed v2 recipe must retry its own output even without further heating");
        helper.assertTrue(amount(furnace.getTank(), TinkerFluids.moltenGold.get()) == 0,
                "Output retries must not finish a standard recipe instead");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void coolingAndStackChangesRetainV2Requirements(GameTestHelper helper) {
        TransmuteBlockEntity furnace = furnace(helper);
        SuperByproductMeltingModuleInventory inventory = inventory(furnace);
        inventory.updateMeltType(MeltType.Transmute_v2);
        inventory.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        inventory.heatItems(800, 100);
        inventory.coolItems();
        helper.assertTrue(inventory.getCurrentTime(0) == 95, "V2 must retain normal cooling behavior");
        inventory.insertItem(0, new ItemStack(Items.GOLD_INGOT), false);
        inventory.extractItem(0, 1, false);
        helper.assertTrue(inventory.getCurrentTime(0) == 95 && inventory.getRequiredTemp(0) == 800
                          && inventory.getRequiredTime(0) == 600,
                "Inserting and extracting matching items must retain v2 requirements and progress");
        melt(inventory);
        helper.assertTrue(amount(furnace.getTank(), Fluids.WATER) == 90,
                "The remaining item must still finish the special recipe");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void lookupCachesFallbackButRechecksNbtAndReload(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        List<Recipe<?>> originalRecipes = List.copyOf(manager.getRecipes());
        CountingTransmuteRecipe special = new CountingTransmuteRecipe();
        List<Recipe<?>> recipes = new ArrayList<>(originalRecipes);
        recipes.add(special);
        manager.replaceRecipes(recipes);
        try {
            TransmuteBlockEntity furnace = furnace(helper);
            SuperByproductMeltingModuleInventory inventory = inventory(furnace);
            inventory.updateMeltType(MeltType.Transmute_v2);
            furnace.getTank().setCapacity(0);
            inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            int specialScans = special.matchCalls;
            helper.assertTrue(specialScans > 0, "The initial lookup must check special recipes");
            melt(inventory);
            for (int retry = 0; retry < 5; retry++) {
                inventory.coolItems();
            }
            helper.assertTrue(special.matchCalls == specialScans,
                    "Cached fallback output retries must not rescan the special recipe list");

            special.enabled = true;
            ItemStack markedIron = new ItemStack(Items.IRON_INGOT);
            markedIron.getOrCreateTag().putBoolean("v2_test", true);
            inventory.setStackInSlot(0, markedIron);
            helper.assertTrue(special.matchCalls > specialScans && inventory.getRequiredTemp(0) == 800,
                    "Different NBT must recheck special recipes instead of reusing a cached fallback");

            SuperByproductMeltingModuleInventory otherInventory = inventory(furnace(helper));
            otherInventory.updateMeltType(MeltType.Transmute_v2);
            otherInventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            CountingTransmuteRecipe replacement = new CountingTransmuteRecipe();
            replacement.enabled = true;
            replacement.requiresMarker = false;
            recipes = new ArrayList<>(originalRecipes);
            recipes.add(replacement);
            manager.replaceRecipes(recipes);
            SuperByproductMeltingModuleInventory.clearTransmuteV2RecipeCaches();
            otherInventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            helper.assertTrue(replacement.matchCalls > 0 && otherInventory.getRequiredTemp(0) == 800,
                    "Recipe reload invalidation must let a new special recipe replace a cached fallback");
        } finally {
            manager.replaceRecipes(originalRecipes);
            SuperByproductMeltingModuleInventory.clearTransmuteV2RecipeCaches();
        }
        helper.succeed();
    }

    private static final class CountingTransmuteRecipe extends TransmuteMeltingRecipe {
        private int matchCalls;
        private boolean enabled;
        private boolean requiresMarker = true;

        private CountingTransmuteRecipe() {
            super(new ResourceLocation("esotericism_tinker", "test/transmute_v2_cache"), "",
                    Ingredient.of(Items.IRON_INGOT), FluidOutput.fromStack(new FluidStack(Fluids.WATER, 90)),
                    800, 60, List.of());
        }

        @Override
        public boolean matches(IMeltingContainer container, Level level) {
            matchCalls++;
            return enabled && (!requiresMarker || container.getStack().hasTag()
                    && container.getStack().getTag().getBoolean("v2_test")) && super.matches(container, level);
        }
    }

    @GameTest(template = "selenic_empty")
    public static void datagenRecipesRoundTripThroughV2Serializer(GameTestHelper helper) {
        List<FinishedRecipe> generated = new ArrayList<>();
        ResourceLocation id = new ResourceLocation("example", "transmute_v2/gold_to_water");
        FluidStack output = new FluidStack(Fluids.WATER, 90);
        output.getOrCreateTag().putString("source", "datagen");
        TransmuteMeltingRecipeBuilder.melting(Ingredient.of(Items.GOLD_INGOT), output, 800, 60)
                .addByproduct(new FluidStack(Fluids.LAVA, 10))
                .addByproduct(FluidOutput.fromStack(new FluidStack(TinkerFluids.moltenCopper.get(), 20)))
                .group("example:transmute_v2")
                .save(generated::add, id);
        ResourceLocation simpleId = new ResourceLocation("example", "transmute_v2/iron_to_water");
        TransmuteMeltingRecipeBuilder.melting(Ingredient.of(Items.IRON_INGOT),
                FluidOutput.fromStack(new FluidStack(Fluids.WATER, 30)), 0, 1)
                .save(generated::add, simpleId);

        var serializer = EsotericismTinkerRecipeTypes.TRANSMUTE_V2_SERIALIZER.get();
        FinishedRecipe finished = generated.get(0);
        var json = finished.serializeRecipe();
        helper.assertTrue(finished.getId().equals(id)
                          && json.get("type").getAsString().equals("esotericism_tinker:transmute_v2"),
                "Datagen must preserve the caller's recipe ID and emit the v2 serializer");
        TransmuteMeltingRecipe decoded = serializer.fromJson(id, json);
        helper.assertTrue(decoded.getInput().test(new ItemStack(Items.GOLD_INGOT))
                          && decoded.getGroup().equals("example:transmute_v2")
                          && decoded.getTemperature() == 800 && decoded.getTime() == 60,
                "Generated JSON must retain the ingredient, group and heating requirements");
        var outputs = decoded.getOutputWithByproducts();
        helper.assertTrue(outputs.size() == 3 && outputs.get(0).get(0).isFluidStackIdentical(output)
                          && outputs.get(1).get(0).isFluidStackIdentical(new FluidStack(Fluids.LAVA, 10))
                          && outputs.get(2).get(0).isFluidStackIdentical(new FluidStack(TinkerFluids.moltenCopper.get(), 20)),
                "Generated JSON must retain the main output NBT and every byproduct amount");
        helper.assertTrue(finished.serializeAdvancement() == null,
                "Datagen must support recipes without unlock criteria");
        TransmuteMeltingRecipe simple = serializer.fromJson(simpleId, generated.get(1).serializeRecipe());
        helper.assertTrue(simple.getType() == EsotericismTinkerRecipeTypes.TRANSMUTE_V2_TYPE.get()
                          && simple.getOutputWithByproducts().size() == 1
                          && simple.getTemperature() == 0 && simple.getTime() == 1,
                "A recipe without byproducts must load with the minimum valid heating requirements");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void specialRecipeNetworkRoundTripRetainsByproducts(GameTestHelper helper) {
        var recipe = (TransmuteMeltingRecipe) helper.getLevel().getRecipeManager().byKey(
                new ResourceLocation("esotericism_tinker", "test/transmute_v2_gold")).orElseThrow();
        var serializer = EsotericismTinkerRecipeTypes.TRANSMUTE_V2_SERIALIZER.get();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            serializer.toNetwork(buffer, recipe);
            TransmuteMeltingRecipe decoded = serializer.fromNetwork(recipe.getId(), buffer);
            helper.assertTrue(decoded != null && decoded.getType() == EsotericismTinkerRecipeTypes.TRANSMUTE_V2_TYPE.get(),
                    "Network sync must preserve the independent recipe type");
            helper.assertTrue(decoded.getOutputWithByproducts().size() == 3,
                    "Network sync must retain every byproduct");
            helper.assertTrue(decoded.getTemperature() == 800 && decoded.getTime() == 60,
                    "Network sync must retain heating requirements");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static TransmuteBlockEntity furnace(GameTestHelper helper) {
        TransmuteBlockEntity furnace = new TransmuteBlockEntity(helper.absolutePos(BlockPos.ZERO),
                EsotericismTinkerSmeltery.transmuteController.get().defaultBlockState());
        furnace.setLevel(helper.getLevel());
        furnace.getTank().setCapacity(4000);
        furnace.getMeltingInventory().resize(1, stack -> {});
        return furnace;
    }

    private static SuperByproductMeltingModuleInventory inventory(TransmuteBlockEntity furnace) {
        return (SuperByproductMeltingModuleInventory) furnace.getMeltingInventory();
    }

    private static void melt(MeltingModuleInventory inventory) {
        inventory.heatItems(10000, 10000);
        inventory.heatItems(10000, 10000);
    }

    private static int amount(IFluidHandler tank, Fluid fluid) {
        int amount = 0;
        for (int index = 0; index < tank.getTanks(); index++) {
            var stack = tank.getFluidInTank(index);
            if (stack.getFluid() == fluid) {
                amount += stack.getAmount();
            }
        }
        return amount;
    }
}
