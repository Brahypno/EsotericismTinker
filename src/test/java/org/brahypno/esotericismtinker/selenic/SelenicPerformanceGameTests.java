package org.brahypno.esotericismtinker.selenic;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import org.brahypno.esotericismtinker.selenic.block.component.SelenicBlockStates;
import org.brahypno.esotericismtinker.selenic.block.entity.ArmillaryCrownBlockEntity;
import org.brahypno.esotericismtinker.selenic.block.entity.LunarFontBlockEntity;
import org.brahypno.esotericismtinker.selenic.block.entity.TestimonyStandBlockEntity;
import org.brahypno.esotericismtinker.world.worldgen.selenic.SelenicAstrolabeRuinConfiguration;
import org.brahypno.esotericismtinker.world.worldgen.selenic.SelenicAstrolabeRuinPlacer;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("esotericism_tinker")
@PrefixGameTestTemplate(false)
public final class SelenicPerformanceGameTests {
    @GameTest(template = "selenic_empty")
    public static void idleSkipsWitnessesButActivationReadsThem(GameTestHelper helper) {
        LunarFontBlockEntity font = placeFigure(helper);
        BlockPos witnessPos = font.getBlockPos().east();
        BlockState state = EsotericismTinkerSelenic.testimonyStand.get().defaultBlockState();
        helper.getLevel().setBlock(witnessPos, state, Block.UPDATE_CLIENTS);
        CountingStand witness = new CountingStand(witnessPos, state);
        witness.setTestimony(new ItemStack(Items.STICK));
        helper.getLevel().setBlockEntity(witness);

        font.scanFigureAndTrim();
        helper.assertTrue(witness.reads == 0, "Idle fonts must not inspect witness inventories");
        helper.assertTrue(font.getActiveItemSlots() == 2, "Idle scans must still refresh lower-axis capacity");

        font.tryActivate(null);
        helper.assertTrue(witness.reads > 0, "Activation must read current witness contents");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty", timeoutTicks = 40)
    public static void idleScansAreSpreadAcrossTicks(GameTestHelper helper) {
        List<TrackingFont> fonts = new ArrayList<>();
        for (int x : new int[]{2, 6, 10, 14}) {
            BlockPos pos = helper.absolutePos(new BlockPos(x, 2, 2));
            placeDoubleBlock(helper, pos, EsotericismTinkerSelenic.lunarFont.get().defaultBlockState());
            TrackingFont font = new TrackingFont(pos, helper.getLevel().getBlockState(pos));
            helper.getLevel().setBlockEntity(font);
            fonts.add(font);
        }

        helper.runAfterDelay(20, () -> {
            for (TrackingFont font : fonts) {
                helper.assertTrue(font.scanTimes.size() == 1, "Each idle font must scan once per 20 ticks");
            }
            long distinctTicks = fonts.stream().map(font -> font.scanTimes.get(0)).distinct().count();
            helper.assertTrue(distinctTicks > 1, "Idle fonts must not all scan on the same tick");
            helper.succeed();
        });
    }

    @GameTest(template = "selenic_empty")
    public static void removingSpinesStillTrimsStoredOutputsAndInputs(GameTestHelper helper) {
        LunarFontBlockEntity font = placeFigure(helper);
        font.scanFigureAndTrim();
        ItemStackHandler output = new ItemStackHandler(8);
        output.setStackInSlot(1, new ItemStack(Items.DIAMOND));
        CompoundTag fontTag = font.saveWithoutMetadata();
        fontTag.put("OutputItems", output.serializeNBT());
        fontTag.put("OutputTank", new FluidStack(Fluids.WATER, 2000).writeToNBT(new CompoundTag()));
        font.load(fontTag);

        helper.getLevel().setBlock(font.getBlockPos().below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        font.scanFigureAndTrim();
        CompoundTag trimmedFont = font.saveWithoutMetadata();
        output.deserializeNBT(trimmedFont.getCompound("OutputItems"));
        helper.assertTrue(output.getStackInSlot(1).isEmpty(), "Removed lower spines must release excess items");
        helper.assertTrue(FluidStack.loadFluidStackFromNBT(trimmedFont.getCompound("OutputTank")).getAmount() == 1000,
                "Removed lower spines must reduce output fluid to the new capacity");

        BlockPos crownPos = font.getBlockPos().above(4);
        ArmillaryCrownBlockEntity crown = (ArmillaryCrownBlockEntity) helper.getLevel().getBlockEntity(crownPos);
        ItemStackHandler inputs = new ItemStackHandler(8);
        inputs.setStackInSlot(2, new ItemStack(Items.EMERALD));
        CompoundTag crownTag = crown.saveWithoutMetadata();
        crownTag.put("Items", inputs.serializeNBT());
        crownTag.put("Tank", new FluidStack(Fluids.WATER, 3000).writeToNBT(new CompoundTag()));
        crown.load(crownTag);
        helper.getLevel().setBlock(crownPos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        crown.trimToCurrentCapacity();
        CompoundTag trimmedCrown = crown.saveWithoutMetadata();
        inputs.deserializeNBT(trimmedCrown.getCompound("Items"));
        helper.assertTrue(inputs.getStackInSlot(2).isEmpty(), "Disconnected crowns must release excess inputs");
        helper.assertTrue(FluidStack.loadFluidStackFromNBT(trimmedCrown.getCompound("Tank")).getAmount() == 1000,
                "Disconnected crowns must reduce input fluid to the base capacity");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void peripheralPlacementDoesNotReadCoreBlocks(GameTestHelper helper) {
        AtomicInteger reads = new AtomicInteger();
        WorldGenLevel level = (WorldGenLevel) Proxy.newProxyInstance(
                WorldGenLevel.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBlockState")) {
                        reads.incrementAndGet();
                        return Blocks.AIR.defaultBlockState();
                    }
                    throw new AssertionError("Unexpected world access for peripheral placement: " + method.getName());
                });
        SelenicAstrolabeRuinConfiguration config = new SelenicAstrolabeRuinConfiguration(
                new SelenicAstrolabeRuinConfiguration.Variant(1, 1, 1, 1, 0),
                new SelenicAstrolabeRuinConfiguration.Structure(1, 1, 1, 1, 1, 1),
                new SelenicAstrolabeRuinConfiguration.Placement(80, 256, 5, 4),
                new SelenicAstrolabeRuinConfiguration.Rewards(0, 0, 0, 8, 0, 180));
        BlockPos origin = new BlockPos(8, 150, 8);
        SelenicAstrolabeRuinPlacer.place(level, origin, 1234L, config, new BoundingBox(16, -64, 0, 31, 319, 15));
        helper.assertTrue(reads.get() == 0, "A peripheral chunk with no loot must not inspect the core");
        helper.succeed();
    }

    private static LunarFontBlockEntity placeFigure(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(5, 3, 5));
        helper.getLevel().setBlock(pos.below(), EsotericismTinkerSelenic.astrolabeSpine.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        placeDoubleBlock(helper, pos, EsotericismTinkerSelenic.lunarFont.get().defaultBlockState());
        helper.getLevel().setBlock(pos.above(2), EsotericismTinkerSelenic.astrolabeSpine.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(pos.above(3), EsotericismTinkerSelenic.astrolabeSpine.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        placeDoubleBlock(helper, pos.above(4), EsotericismTinkerSelenic.armillaryCrown.get().defaultBlockState());
        return (LunarFontBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static void placeDoubleBlock(GameTestHelper helper, BlockPos pos, BlockState state) {
        helper.getLevel().setBlock(pos, state.setValue(SelenicBlockStates.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(pos.above(), state.setValue(SelenicBlockStates.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
    }

    private static final class CountingStand extends TestimonyStandBlockEntity {
        private int reads;

        private CountingStand(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public ItemStack getTestimony() {
            reads++;
            return super.getTestimony();
        }
    }

    private static final class TrackingFont extends LunarFontBlockEntity {
        private final List<Long> scanTimes = new ArrayList<>();

        private TrackingFont(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public void scanFigureAndTrim() {
            scanTimes.add(getLevel().getGameTime());
            super.scanFigureAndTrim();
        }
    }
}
