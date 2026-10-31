package org.brahypno.esotericismtinker.smeltery;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.smeltery.block.entity.controller.TransmuteBlockEntity;
import org.brahypno.esotericismtinker.smeltery.block.entity.module.SuperByproductMeltingModuleInventory;
import org.brahypno.esotericismtinker.smeltery.block.entity.module.SuperByproductMeltingModuleInventory.MeltType;
import org.brahypno.esotericismtinker.smeltery.recipe.melting.TransmuteMeltingRecipe;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeLookup;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("esotericism_tinker")
@PrefixGameTestTemplate(false)
public final class TransmuteV2GameTests {
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
