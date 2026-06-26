package org.brahypno.esotericismtinker.world.worldgen.selenic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.Tags;
import org.brahypno.esotericismtinker.common.data.loot.ETSelenicChestLoot;
import org.brahypno.esotericismtinker.selenic.EsotericismTinkerSelenic;
import org.brahypno.esotericismtinker.selenic.block.component.SelenicBlockStates;
import org.brahypno.esotericismtinker.selenic.block.component.TestimonyStandBlock;

import javax.annotation.Nullable;

public final class SelenicAstrolabeRuinPlacer {
    private SelenicAstrolabeRuinPlacer() {}

    public static boolean place(
            WorldGenLevel level,
            BlockPos origin,
            RandomSource random,
            SelenicAstrolabeRuinConfiguration config
    ) {
        if (!isInHeightRange(origin, config)){
            return false;
        }

        if (!hasOrRepairGroundSupport(level, origin, random)){
            return false;
        }

        float completenessNoise = SelenicRuinNoise.value2D(origin, 1103, 192.0D);
        float heightNoise = SelenicRuinNoise.value2D(origin, 2207, 128.0D);
        float testimonyNoise = SelenicRuinNoise.value2D(origin, 3301, 96.0D);

        float completeChance = SelenicRuinNoise.influence(
                config.variant().completeChance(),
                completenessNoise,
                config.variant().noiseInfluence()
        );

        float crownChance = SelenicRuinNoise.influence(
                config.variant().crownChance(),
                completenessNoise,
                config.variant().noiseInfluence() * 0.5F
        );

        float fontChance = SelenicRuinNoise.influence(
                config.variant().fontChance(),
                completenessNoise,
                config.variant().noiseInfluence() * 0.5F
        );

        float testimonyChance = SelenicRuinNoise.influence(
                config.variant().testimonyChance(),
                testimonyNoise,
                config.variant().noiseInfluence()
        );

        boolean complete = random.nextFloat() < completeChance;

        int lowerSpines = randomBetween(
                random,
                config.structure().minSpinesBelow(),
                config.structure().maxSpinesBelow()
        );

        int upperSpines = randomBetween(
                random,
                config.structure().minSpinesAbove(),
                config.structure().maxSpinesAbove()
        );

        lowerSpines = adjustLowerSpines(lowerSpines, config, heightNoise, random);
        upperSpines = adjustUpperSpines(upperSpines, config, heightNoise, random);

        if (complete && lowerSpines + upperSpines <= 0){
            upperSpines = 1;
        }

        BlockPos basePos = origin;
        BlockPos fontPos = basePos.above(lowerSpines);
        BlockPos crownPos = fontPos.above(upperSpines + 2);

        if (!canReplaceCoreColumn(level, basePos, crownPos)){
            return false;
        }

        int openSkyY = findCarvableOpenSkyY(level, crownPos.above());
        if (openSkyY < 0){
            return false;
        }

        carveOpenShaft(level, basePos, crownPos, config, openSkyY);
        prepareFoundation(level, basePos, config, random);

        boolean placeFont = complete || random.nextFloat() < fontChance;
        boolean placeCrown = complete || random.nextFloat() < crownChance;

        placeSpines(level, basePos, fontPos, lowerSpines, upperSpines, complete, random);

        if (placeFont){
            placeLunarFont(level, fontPos);
        }

        if (placeCrown){
            placeArmillaryCrown(level, crownPos);
        }

        int stands = randomBetween(
                random,
                config.structure().minStands(),
                config.structure().maxStands()
        );

        stands = adjustStands(stands, config, testimonyNoise, random);

        if (complete){
            stands = Math.max(stands, Math.min(3, config.structure().maxStands()));
        }

        placeTestimonyStands(level, fontPos, random, stands, upperSpines, testimonyChance);
        placeLootChests(level, fontPos, random, config);
        tryPlaceBuriedNetheriteBlock(level, fontPos, random, config);

        return true;
    }

    public static boolean isInHeightRange(
            BlockPos origin,
            SelenicAstrolabeRuinConfiguration config
    ) {
        return origin.getY() >= config.placement().minY()
               && origin.getY() <= config.placement().maxY();
    }

    public static boolean canStartAt(
            Structure.GenerationContext context,
            BlockPos origin,
            SelenicAstrolabeRuinConfiguration config
    ) {
        if (!isInHeightRange(origin, config)){
            return false;
        }

        NoiseColumn column = context.chunkGenerator().getBaseColumn(
                origin.getX(),
                origin.getZ(),
                context.heightAccessor(),
                context.randomState()
        );

        int minY = context.heightAccessor().getMinBuildHeight();
        int maxY = context.heightAccessor().getMaxBuildHeight() - 1;

        if (origin.getY() <= minY || origin.getY() >= maxY){
            return false;
        }

        BlockState floor = column.getBlock(origin.getY() - 1);
        BlockState surface = column.getBlock(origin.getY());

        if (!isValidNaturalAnchorFloor(floor)){
            return false;
        }

        if (!isValidSurfaceGap(surface)){
            return false;
        }

        int maxCoreY = getMaxPossibleCoreTop(origin, config);
        if (!canReplaceCoreColumn(column, origin.getY(), maxCoreY, maxY)){
            return false;
        }

        return canCarveToSky(column, maxCoreY + 1, maxY);
    }

    private static int getMaxPossibleCoreTop(
            BlockPos origin,
            SelenicAstrolabeRuinConfiguration config
    ) {
        int maxLowerSpines = config.structure().maxSpinesBelow() + 1;
        int maxUpperSpines = config.structure().maxSpinesAbove() + 2;

        // base + lower spines + font upper gap + upper spines + crown upper half
        return origin.getY() + maxLowerSpines + maxUpperSpines + 3;
    }

    private static boolean canReplaceCoreColumn(
            NoiseColumn column,
            int minY,
            int maxY,
            int worldMaxY
    ) {
        for (int y = minY; y <= Math.min(maxY, worldMaxY); y++) {
            BlockState state = column.getBlock(y);
            if (state.isAir() || state.canBeReplaced() || isCarvableRoofBlock(state)){
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean canCarveToSky(
            NoiseColumn column,
            int startY,
            int worldMaxY
    ) {
        for (int y = startY; y <= worldMaxY; y++) {
            BlockState state = column.getBlock(y);

            if (state.isAir() || state.canBeReplaced()){
                return true;
            }

            if (!isCarvableRoofBlock(state)){
                return false;
            }
        }

        return true;
    }

    private static boolean hasOrRepairGroundSupport(
            WorldGenLevel level,
            BlockPos origin,
            RandomSource random
    ) {
        BlockPos floor = origin.below();
        BlockState floorState = level.getBlockState(floor);
        BlockState surfaceState = level.getBlockState(origin);

        if (floorState.isFaceSturdy(level, floor, Direction.UP)){
            return isValidNaturalAnchorFloor(floorState);
        }

        if (!isRepairableWeakSurfaceFloor(floorState)){
            return false;
        }

        if (!isValidSurfaceGap(surfaceState)){
            return false;
        }

        level.setBlock(floor, randomFoundationBlock(random), Block.UPDATE_CLIENTS);
        return true;
    }

    private static boolean isValidNaturalAnchorFloor(BlockState state) {
        if (state.isAir()){
            return false;
        }
        if (state.getFluidState().is(FluidTags.WATER)){
            return false;
        }
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)){
            return false;
        }

        return state.is(BlockTags.DIRT)
               || state.is(BlockTags.BASE_STONE_OVERWORLD)
               || state.is(BlockTags.SAND)
               || state.is(Blocks.GRAVEL)
               || state.is(Blocks.SNOW_BLOCK)
               || state.is(Blocks.POWDER_SNOW)
               || state.is(Blocks.ICE)
               || state.is(Blocks.PACKED_ICE)
               || state.is(Blocks.BLUE_ICE);
    }

    private static boolean isRepairableWeakSurfaceFloor(BlockState state) {
        if (!isValidNaturalAnchorFloor(state)){
            return false;
        }

        return state.is(Blocks.SNOW)
               || state.is(Blocks.POWDER_SNOW)
               || state.is(Blocks.ICE)
               || state.is(Blocks.PACKED_ICE)
               || state.is(Blocks.BLUE_ICE)
               || state.is(BlockTags.SAND)
               || state.is(Blocks.GRAVEL)
               || state.is(BlockTags.DIRT)
               || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    private static boolean isValidSurfaceGap(BlockState state) {
        if (state.getFluidState().is(FluidTags.WATER)){
            return false;
        }
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)){
            return false;
        }

        return state.isAir() || state.canBeReplaced();
    }

    private static boolean canReplaceCoreColumn(
            WorldGenLevel level,
            BlockPos basePos,
            BlockPos crownPos
    ) {
        for (BlockPos pos : BlockPos.betweenClosed(basePos, crownPos.above())) {
            BlockState state = level.getBlockState(pos);

            if (state.isAir() || state.canBeReplaced() || isCarvableRoofBlock(state)){
                continue;
            }

            return false;
        }

        return true;
    }

    private static int findCarvableOpenSkyY(WorldGenLevel level, BlockPos pos) {
        int maxY = level.getMaxBuildHeight() - 1;

        for (int y = pos.getY(); y <= maxY; y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());

            if (level.canSeeSky(checkPos)){
                return y;
            }

            BlockState state = level.getBlockState(checkPos);
            if (state.isAir() || state.canBeReplaced() || isCarvableRoofBlock(state)){
                continue;
            }

            return -1;
        }

        return maxY;
    }

    private static void carveOpenShaft(WorldGenLevel level, BlockPos basePos, BlockPos crownPos, SelenicAstrolabeRuinConfiguration config, int openSkyY) {
        int radius = Math.min(config.placement().reserveRadius(), 2);

        BlockPos min = basePos.offset(-1, 0, -1);
        BlockPos max = new BlockPos(crownPos.getX(), openSkyY, crownPos.getZ()).offset(1, 0, 1);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);

            if (state.isAir()){
                continue;
            }

            if (state.canBeReplaced() || isCarvableRoofBlock(state)){
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        carveTopOpening(level, crownPos, radius, Math.max(0, openSkyY - crownPos.getY()));
    }

    private static void carveTopOpening(
            WorldGenLevel level,
            BlockPos crownPos,
            int radius,
            int height
    ) {
        BlockPos start = crownPos.above();

        for (int y = 0; y <= height; y++) {
            int currentRadius = y > 2 ? radius : 1;

            for (int dx = -currentRadius; dx <= currentRadius; dx++) {
                for (int dz = -currentRadius; dz <= currentRadius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > currentRadius + 1){
                        continue;
                    }

                    BlockPos pos = start.offset(dx, y, dz);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir() || state.canBeReplaced()){
                        continue;
                    }

                    if (isCarvableRoofBlock(state)){
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static boolean isCarvableRoofBlock(BlockState state) {
        return state.is(Blocks.STONE)
               || state.is(Blocks.DEEPSLATE)
               || state.is(Blocks.TUFF)
               || state.is(Blocks.CALCITE)
               || state.is(Blocks.DIRT)
               || state.is(Blocks.GRASS_BLOCK)
               || state.is(Blocks.SNOW_BLOCK)
               || state.is(Blocks.SNOW)
               || state.is(Blocks.POWDER_SNOW)
               || state.is(Blocks.ICE)
               || state.is(Blocks.PACKED_ICE)
               || state.is(Blocks.BLUE_ICE);
    }

    private static void prepareFoundation(
            WorldGenLevel level,
            BlockPos basePos,
            SelenicAstrolabeRuinConfiguration config,
            RandomSource random
    ) {
        int radius = Math.min(config.placement().reserveRadius() + 2, 7);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distance = Math.abs(dx) + Math.abs(dz);

                if (distance > radius + 1){
                    continue;
                }

                if (distance > 2 && random.nextFloat() < 0.35F){
                    continue;
                }

                BlockPos surface = findSurface(level, basePos.offset(dx, 0, dz));

                if (surface == null){
                    continue;
                }

                if (Math.abs(surface.getY() - basePos.getY()) > 4){
                    continue;
                }

                placeFoundationPatchBlock(level, surface, distance, random);
            }
        }
    }

    private static void placeFoundationPatchBlock(
            WorldGenLevel level,
            BlockPos surface,
            int distance,
            RandomSource random
    ) {
        BlockPos floor = surface.below();

        if (!canBecomeRuinFloor(level, floor)){
            return;
        }

        level.setBlock(floor, randomFoundationBlock(random), Block.UPDATE_CLIENTS);

        if (distance <= 2){
            return;
        }

        if (random.nextFloat() > 0.18F){
            return;
        }

        if (!canPlaceLooseRubble(level, surface)){
            return;
        }

        level.setBlock(surface, randomFoundationBlock(random), Block.UPDATE_CLIENTS);
    }

    private static boolean canBecomeRuinFloor(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.getFluidState().is(FluidTags.WATER)){
            return false;
        }

        return state.is(BlockTags.DIRT)
               || state.is(BlockTags.BASE_STONE_OVERWORLD)
               || state.is(Blocks.SNOW_BLOCK)
               || state.is(Blocks.POWDER_SNOW)
               || state.is(Blocks.ICE)
               || state.is(Blocks.PACKED_ICE)
               || state.is(Blocks.BLUE_ICE);
    }

    private static boolean canPlaceLooseRubble(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!state.canBeReplaced()){
            return false;
        }

        if (state.getFluidState().is(FluidTags.WATER)){
            return false;
        }

        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private static BlockState randomFoundationBlock(RandomSource random) {
        int roll = random.nextInt(5);

        return switch (roll) {
            case 0 -> Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            case 1 -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case 2 -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case 3 -> Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
            default -> Blocks.TUFF.defaultBlockState();
        };
    }

    private static int adjustUpperSpines(
            int upperSpines,
            SelenicAstrolabeRuinConfiguration config,
            float heightNoise,
            RandomSource random
    ) {
        if (heightNoise > 0.72F){
            upperSpines += 1 + random.nextInt(2);
        }

        if (heightNoise > 0.88F){
            upperSpines += 1;
        }

        if (heightNoise < 0.22F){
            upperSpines -= 1;
        }

        return clamp(
                upperSpines,
                config.structure().minSpinesAbove(),
                config.structure().maxSpinesAbove() + 2
        );
    }

    private static int adjustLowerSpines(
            int lowerSpines,
            SelenicAstrolabeRuinConfiguration config,
            float heightNoise,
            RandomSource random
    ) {
        if (heightNoise > 0.65F){
            lowerSpines += random.nextInt(2);
        }

        if (heightNoise < 0.25F){
            lowerSpines -= 1;
        }

        return clamp(
                lowerSpines,
                config.structure().minSpinesBelow(),
                config.structure().maxSpinesBelow() + 1
        );
    }

    private static int adjustStands(
            int stands,
            SelenicAstrolabeRuinConfiguration config,
            float testimonyNoise,
            RandomSource random
    ) {
        if (testimonyNoise > 0.70F){
            stands += 1 + random.nextInt(2);
        }

        if (testimonyNoise > 0.88F){
            stands += 1;
        }

        if (testimonyNoise < 0.25F){
            stands -= 1 + random.nextInt(2);
        }

        return clamp(stands, 0, Math.min(8, config.structure().maxStands() + 2));
    }

    private static void placeSpines(
            WorldGenLevel level,
            BlockPos basePos,
            BlockPos fontPos,
            int lowerSpines,
            int upperSpines,
            boolean complete,
            RandomSource random
    ) {
        BlockState spine = EsotericismTinkerSelenic.astrolabeSpine.get().defaultBlockState();

        for (int i = 0; i < lowerSpines; i++) {
            if (complete || random.nextFloat() < 0.85F){
                level.setBlock(basePos.above(i), spine, Block.UPDATE_CLIENTS);
            }
        }

        for (int i = 1; i <= upperSpines; i++) {
            if (complete || random.nextFloat() < 0.85F){
                level.setBlock(fontPos.above(i + 1), spine, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void placeLunarFont(WorldGenLevel level, BlockPos lowerPos) {
        BlockState lower = EsotericismTinkerSelenic.lunarFont.get()
                                                             .defaultBlockState()
                                                             .setValue(SelenicBlockStates.HALF, DoubleBlockHalf.LOWER)
                                                             .setValue(SelenicBlockStates.ACTIVE, false)
                                                             .setValue(SelenicBlockStates.SIGNALING, false);

        BlockState upper = lower.setValue(SelenicBlockStates.HALF, DoubleBlockHalf.UPPER);

        level.setBlock(lowerPos, lower, Block.UPDATE_CLIENTS);
        level.setBlock(lowerPos.above(), upper, Block.UPDATE_CLIENTS);
    }

    private static void placeArmillaryCrown(WorldGenLevel level, BlockPos lowerPos) {
        BlockState lower = EsotericismTinkerSelenic.armillaryCrown.get()
                                                                  .defaultBlockState()
                                                                  .setValue(SelenicBlockStates.HALF, DoubleBlockHalf.LOWER)
                                                                  .setValue(SelenicBlockStates.ACTIVE, false);

        BlockState upper = lower.setValue(SelenicBlockStates.HALF, DoubleBlockHalf.UPPER);

        level.setBlock(lowerPos, lower, Block.UPDATE_CLIENTS);
        level.setBlock(lowerPos.above(), upper, Block.UPDATE_CLIENTS);
    }

    private static void placeTestimonyStands(
            WorldGenLevel level,
            BlockPos fontPos,
            RandomSource random,
            int count,
            int upperSpines,
            float testimonyChance
    ) {
        int placed = 0;

        for (int[] offset : shuffledOffsets(random)) {
            if (placed >= count){
                return;
            }

            if (random.nextFloat() > testimonyChance){
                continue;
            }

            int yOffset = random.nextInt(Math.max(1, upperSpines + 1));
            BlockPos pos = fontPos.offset(offset[0], yOffset, offset[1]);

            if (!level.getBlockState(pos).canBeReplaced()){
                continue;
            }

            BlockPos below = pos.below();
            boolean floating = !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);

            BlockState stand = EsotericismTinkerSelenic.testimonyStand.get()
                                                                      .defaultBlockState()
                                                                      .setValue(TestimonyStandBlock.FLOATING, floating);

            level.setBlock(pos, stand, Block.UPDATE_CLIENTS);
            placed++;
        }
    }

    private static void placeLootChests(
            WorldGenLevel level,
            BlockPos center,
            RandomSource random,
            SelenicAstrolabeRuinConfiguration config
    ) {
        int candidateCount = randomBetween(
                random,
                config.rewards().minLootChests(),
                config.rewards().maxLootChests()
        );

        if (candidateCount <= 0){
            return;
        }

        int radius = config.rewards().lootChestRadius();
        int placementAttemptsPerChest = 8;

        for (int i = 0; i < candidateCount; i++) {
            if (random.nextFloat() > config.rewards().lootChestChance()){
                continue;
            }

            tryPlaceLootChest(level, center, random, radius, placementAttemptsPerChest);
        }
    }

    private static void tryPlaceLootChest(
            WorldGenLevel level,
            BlockPos center,
            RandomSource random,
            int radius,
            int attempts
    ) {
        for (int i = 0; i < attempts; i++) {
            int dx = randomOffset(random, radius);
            int dz = randomOffset(random, radius);

            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1){
                continue;
            }

            BlockPos surface = findSurface(level, center.offset(dx, 0, dz));

            if (surface == null || !canPlaceLootChest(level, surface)){
                continue;
            }

            BlockState chestState = Blocks.CHEST.defaultBlockState()
                                                .setValue(ChestBlock.FACING, randomHorizontal(random));

            level.setBlock(surface, chestState, Block.UPDATE_CLIENTS);

            ResourceLocation lootTable = ETSelenicChestLoot.TRAIL_RUINS_CHEST;
            RandomizableContainerBlockEntity.setLootTable(level, random, surface, lootTable);

            return;
        }

    }

    @Nullable
    private static BlockPos findSurface(WorldGenLevel level, BlockPos pos) {
        int y = level.getHeight(
                Heightmap.Types.WORLD_SURFACE_WG,
                pos.getX(),
                pos.getZ()
        );

        BlockPos surface = new BlockPos(pos.getX(), y, pos.getZ());

        if (surface.getY() <= level.getMinBuildHeight()
            || surface.getY() >= level.getMaxBuildHeight()){
            return null;
        }

        return surface;
    }

    private static boolean canPlaceLootChest(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos below = pos.below();

        return state.canBeReplaced()
               && !state.getFluidState().is(FluidTags.WATER)
               && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private static void tryPlaceBuriedNetheriteBlock(
            WorldGenLevel level,
            BlockPos origin,
            RandomSource random,
            SelenicAstrolabeRuinConfiguration config
    ) {
        if (random.nextFloat() > config.rewards().netheriteBlockChance()){
            return;
        }

        BlockPos target = findFirstBuriedBaseStoneBelow(
                level,
                origin,
                config.rewards().netheriteSearchDepth()
        );

        if (target != null){
            level.setBlock(target, Blocks.NETHERITE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    private static BlockPos findFirstBuriedBaseStoneBelow(
            WorldGenLevel level,
            BlockPos origin,
            int depth
    ) {
        int minY = Math.max(level.getMinBuildHeight() + 1, origin.getY() - depth);

        for (int y = origin.getY() - 5; y >= minY; y--) {
            BlockPos pos = new BlockPos(origin.getX(), y, origin.getZ());

            if (isBuriedBaseStone(level, pos)){
                return pos;
            }
        }

        return null;
    }

    private static boolean isBuriedBaseStone(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!state.is(BlockTags.BASE_STONE_OVERWORLD)
            && !state.is(Tags.Blocks.STONE)){
            return false;
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighbor = level.getBlockState(neighborPos);

            if (neighbor.isAir()){
                return false;
            }

            if (neighbor.getFluidState().is(FluidTags.WATER)){
                return false;
            }
        }

        return true;
    }

    private static int[][] shuffledOffsets(RandomSource random) {
        int[][] offsets = {
                {-1, 0},
                {0, -1},
                {1, 0},
                {0, 1},
                {-1, -1},
                {1, -1},
                {-1, 1},
                {1, 1}
        };

        for (int i = offsets.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] temp = offsets[i];
            offsets[i] = offsets[j];
            offsets[j] = temp;
        }

        return offsets;
    }

    private static Direction randomHorizontal(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    private static int randomOffset(RandomSource random, int radius) {
        return random.nextInt(radius * 2 + 1) - radius;
    }

    private static int randomBetween(RandomSource random, int min, int max) {
        if (max <= min){
            return min;
        }

        return min + random.nextInt(max - min + 1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}