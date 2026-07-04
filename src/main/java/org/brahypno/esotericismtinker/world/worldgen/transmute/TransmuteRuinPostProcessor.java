package org.brahypno.esotericismtinker.world.worldgen.transmute;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.brahypno.esotericismtinker.world.worldgen.selenic.SelenicRuinNoise;

import java.util.HashSet;
import java.util.Set;

public final class TransmuteRuinPostProcessor {

    private TransmuteRuinPostProcessor() {}

    public static void process(WorldGenLevel level, BoundingBox placedBox, BoundingBox generationBox, RandomSource random, TransmuteRuinKind kind, long ruinSeed, StructureTemplate template, StructurePlaceSettings settings, BlockPos placePos) {
        BoundingBox processBox = intersect(placedBox, generationBox);
        if (processBox == null){
            return;
        }

        Set<BlockPos> templateBlocks = collectTemplateSolidPositions(template, settings, placePos, processBox);

        fillLootContainers(level, processBox, random);
        randomizeFunctionalAshenBlocks(level, processBox, random, kind);
        damageTransmuteControllers(level, processBox, random, kind);

        Set<BlockPos> trapProtected = processTntTrapGroups(level, processBox);
        erodeStructure(level, placedBox, processBox, kind, ruinSeed, trapProtected, templateBlocks);

        TransmuteSavedEntityPlacer.spawnSavedEntities(level, processBox, template, settings, placePos);
    }


    private static void fillLootContainers(
            WorldGenLevel level,
            BoundingBox box,
            RandomSource random
    ) {
        for (BlockPos cursor : BlockPos.betweenClosed(
                box.minX(),
                box.minY(),
                box.minZ(),
                box.maxX(),
                box.maxY(),
                box.maxZ()
        )) {
            BlockPos pos = cursor.immutable();
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            if (isNotLootContainer(block)){
                continue;
            }

            RandomizableContainerBlockEntity.setLootTable(
                    level,
                    random,
                    pos,
                    BuiltInLootTables.END_CITY_TREASURE
            );
        }
    }

    private static boolean isNotLootContainer(Block block) {
        return !(block instanceof ChestBlock)
               && !(block instanceof BarrelBlock)
               && !(block instanceof ShulkerBoxBlock);
    }

    private static void randomizeFunctionalAshenBlocks(
            WorldGenLevel level,
            BoundingBox box,
            RandomSource random,
            TransmuteRuinKind kind
    ) {
        for (BlockPos cursor : BlockPos.betweenClosed(
                box.minX(),
                box.minY(),
                box.minZ(),
                box.maxX(),
                box.maxY(),
                box.maxZ()
        )) {
            BlockPos pos = cursor.immutable();
            BlockState state = level.getBlockState(pos);

            if (!isFunctionalAshenBlock(state)){
                continue;
            }

            if (random.nextFloat() >= kind.functionalReplaceChance()){
                continue;
            }

            level.setBlock(pos, randomAshenBlock(random), Block.UPDATE_CLIENTS);
        }
    }

    private static Set<BlockPos> collectTemplateSolidPositions(StructureTemplate template, StructurePlaceSettings settings, BlockPos placePos, BoundingBox processBox) {
        Set<BlockPos> positions = new HashSet<>();

        if (template.palettes.isEmpty()){
            return positions;
        }

        StructureTemplate.Palette palette = settings.getRandomPalette(template.palettes, placePos);

        for (StructureTemplate.StructureBlockInfo info : palette.blocks()) {
            if (!isRealTemplateBlock(info.state())){
                continue;
            }

            BlockPos worldPos = StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(placePos);
            if (isInside(processBox, worldPos)){
                positions.add(worldPos.immutable());
            }
        }

        return positions;
    }

    private static boolean isRealTemplateBlock(BlockState state) {
        return !state.isAir() && !state.is(Blocks.STRUCTURE_VOID) && !state.is(Blocks.STRUCTURE_BLOCK) && !state.is(Blocks.JIGSAW);
    }

    private static boolean isFunctionalAshenBlock(BlockState state) {
        return state.is(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_HEATER)
               || state.is(EsotericismTinkerTagKeys.Blocks.TRANSMUTE_ACCEL);
    }

    private static BlockState randomAshenBlock(RandomSource random) {
        return BuiltInRegistries.BLOCK
                .getTag(EsotericismTinkerTagKeys.Blocks.ASHEN_BLOCKS)
                .flatMap(tag -> tag.getRandomElement(random))
                .map(holder -> holder.value().defaultBlockState())
                .orElse(EsotericismTinkerSmeltery.ashenStone.get().defaultBlockState());
    }

    private static void damageTransmuteControllers(
            WorldGenLevel level,
            BoundingBox box,
            RandomSource random,
            TransmuteRuinKind kind
    ) {
        for (BlockPos cursor : BlockPos.betweenClosed(
                box.minX(),
                box.minY(),
                box.minZ(),
                box.maxX(),
                box.maxY(),
                box.maxZ()
        )) {
            BlockPos pos = cursor.immutable();
            BlockState state = level.getBlockState(pos);

            if (!state.is(EsotericismTinkerSmeltery.transmuteController.get())){
                continue;
            }

            if (random.nextFloat() < kind.controllerAirChance()){
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                continue;
            }

            if (random.nextFloat() < kind.controllerLoseItemsChance()){
                clearControllerItems(level, pos);
            }
        }
    }

    private static void clearControllerItems(WorldGenLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null){
            return;
        }

        boolean[] changed = {false};
        be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            if (!(handler instanceof IItemHandlerModifiable modifiable)){
                return;
            }

            for (int i = 0; i < modifiable.getSlots(); i++) {
                modifiable.setStackInSlot(i, ItemStack.EMPTY);
            }
            changed[0] = true;
        });

        if (changed[0]){
            be.setChanged();
            return;
        }

        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static void erodeStructure(WorldGenLevel level, BoundingBox placedBox, BoundingBox processBox, TransmuteRuinKind kind, long ruinSeed, Set<BlockPos> trapProtected, Set<BlockPos> templateBlocks) {
        if (kind == TransmuteRuinKind.COMPLETE){
            return;
        }

        int coarseSalt = seedSalt(ruinSeed, 73127);
        int fineSalt = seedSalt(ruinSeed, 9137);
        int collapseSalt = seedSalt(ruinSeed, 44021);

        for (BlockPos cursor : BlockPos.betweenClosed(processBox.minX(), processBox.minY(), processBox.minZ(), processBox.maxX(), processBox.maxY(),
                                                      processBox.maxZ())) {
            BlockPos pos = cursor.immutable();

            if (!templateBlocks.contains(pos) || trapProtected.contains(pos)){
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!canWeather(state)){
                continue;
            }

            float chance = erosionChance(level, placedBox, pos, kind, coarseSalt, fineSalt);
            chance = Math.max(chance, collapseChance(placedBox, pos, kind, collapseSalt));

            if (stableFloat(pos, ruinSeed, 812873) < chance){
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static float erosionChance(WorldGenLevel level, BoundingBox box, BlockPos pos, TransmuteRuinKind kind, int coarseSalt, int fineSalt) {
        float chance = kind.eraseChance();

        float coarse = SelenicRuinNoise.value2D(pos, coarseSalt, 13.0D);
        float fine = SelenicRuinNoise.value2D(pos, fineSalt, 4.0D);

        if (coarse > 0.62F){
            chance += kind == TransmuteRuinKind.RUINED ? 0.18F : 0.10F;
        }else if (coarse < 0.24F){
            chance -= kind == TransmuteRuinKind.RUINED ? 0.14F : 0.08F;
        }

        if (fine > 0.78F){
            chance += 0.08F;
        }

        if (!hasSupport(level, pos)){
            chance += kind.unsupportedExtraChance();
        }

        double height = normalize(pos.getY(), box.minY(), box.maxY());
        if (height > 0.62D){
            chance += kind == TransmuteRuinKind.RUINED ? 0.10F : 0.05F;
        }

        return Mth.clamp(chance, 0.0F, 0.95F);
    }

    private static int seedSalt(long seed, int salt) {
        long value = seed;
        value ^= (long) salt * 0x9E3779B97F4A7C15L;

        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;

        return (int) value;
    }

    private static double normalize(int value, int min, int max) {
        if (max <= min){
            return 0.0D;
        }
        return (value - min) / (double) (max - min);
    }

    private static boolean canWeather(BlockState state) {
        if (state.isAir() || state.is(Blocks.TNT) || isStoneProtected(state) || !state.getFluidState().isEmpty() || state.hasBlockEntity()){
            return false;
        }

        Block block = state.getBlock();
        return !(block instanceof BasePressurePlateBlock) && isNotLootContainer(block);
    }

    private static boolean isStoneProtected(BlockState state) {
        return state.is(Blocks.STONE);
    }

    private static boolean hasSupport(WorldGenLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private static BoundingBox intersect(BoundingBox first, BoundingBox second) {
        int minX = Math.max(first.minX(), second.minX());
        int minY = Math.max(first.minY(), second.minY());
        int minZ = Math.max(first.minZ(), second.minZ());
        int maxX = Math.min(first.maxX(), second.maxX());
        int maxY = Math.min(first.maxY(), second.maxY());
        int maxZ = Math.min(first.maxZ(), second.maxZ());

        if (minX > maxX || minY > maxY || minZ > maxZ){
            return null;
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Set<BlockPos> processTntTrapGroups(WorldGenLevel level, BoundingBox processBox) {
        Set<BlockPos> protectedPositions = new HashSet<>();

        for (TntTrapGroup group : collectTntTrapGroups(level, processBox)) {
            for (BlockPos pos : group.positions()) {
                if (isInside(processBox, pos)){
                    protectedPositions.add(pos);
                }
            }
        }

        return protectedPositions;
    }

    private static Set<TntTrapGroup> collectTntTrapGroups(
            WorldGenLevel level,
            BoundingBox box
    ) {
        Set<TntTrapGroup> groups = new HashSet<>();

        for (BlockPos cursor : BlockPos.betweenClosed(
                box.minX(),
                box.minY(),
                box.minZ(),
                box.maxX(),
                box.maxY(),
                box.maxZ()
        )) {
            BlockPos pos = cursor.immutable();

            if (!level.getBlockState(pos).is(Blocks.TNT)){
                continue;
            }

            groups.add(new TntTrapGroup(pos, collectTntTrapPositions(level, pos)));
        }

        return groups;
    }

    private static Set<BlockPos> collectTntTrapPositions(
            WorldGenLevel level,
            BlockPos tntPos
    ) {
        Set<BlockPos> positions = new HashSet<>();
        positions.add(tntPos);

        for (int offset = 1; offset <= 3; offset++) {
            BlockPos pos = tntPos.above(offset);
            BlockState state = level.getBlockState(pos);

            if (state.isAir()){
                break;
            }

            positions.add(pos);

            if (state.getBlock() instanceof BasePressurePlateBlock){
                break;
            }
        }

        return positions;
    }

    private static boolean isInside(BoundingBox box, BlockPos pos) {
        return pos.getX() >= box.minX()
               && pos.getX() <= box.maxX()
               && pos.getY() >= box.minY()
               && pos.getY() <= box.maxY()
               && pos.getZ() >= box.minZ()
               && pos.getZ() <= box.maxZ();
    }

    private static float stableFloat(
            BlockPos pos,
            long seed,
            int salt
    ) {
        long value = seed;
        value ^= (long) pos.getX() * 341873128712L;
        value ^= (long) pos.getY() * 132897987541L;
        value ^= (long) pos.getZ() * 42317861L;
        value ^= (long) salt * 92837111L;

        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;

        return (value >>> 40) / (float) (1 << 24);
    }

    private record TntTrapGroup(BlockPos anchor, Set<BlockPos> positions) {}

    private static float collapseChance(BoundingBox box, BlockPos pos, TransmuteRuinKind kind, int salt) {
        if (kind == TransmuteRuinKind.COMPLETE){
            return 0.0F;
        }

        double y = normalize(pos.getY(), box.minY(), box.maxY());
        int cellSize = kind == TransmuteRuinKind.RUINED ? 6 : 8;

        int cellX = Math.floorDiv(pos.getX() - box.minX(), cellSize);
        int cellZ = Math.floorDiv(pos.getZ() - box.minZ(), cellSize);
        int cellY = Math.floorDiv(pos.getY() - box.minY(), kind == TransmuteRuinKind.RUINED ? 5 : 7);

        float cell = stableCellFloat(cellX, cellY, cellZ, salt);
        float threshold = kind == TransmuteRuinKind.RUINED ? 0.44F : 0.20F;

        if (y > 0.55D){
            threshold += kind == TransmuteRuinKind.RUINED ? 0.22F : 0.12F;
        }
        if (y > 0.75D){
            threshold += kind == TransmuteRuinKind.RUINED ? 0.18F : 0.08F;
        }

        if (cell > threshold){
            return 0.0F;
        }

        float edge = edgeExposure(box, pos);
        float base = kind == TransmuteRuinKind.RUINED ? 0.92F : 0.68F;
        return Mth.clamp(base + edge * 0.10F, 0.0F, 0.98F);
    }

    private static float edgeExposure(BoundingBox box, BlockPos pos) {
        double x = normalize(pos.getX(), box.minX(), box.maxX());
        double z = normalize(pos.getZ(), box.minZ(), box.maxZ());
        double edge = Math.min(Math.min(x, 1.0D - x), Math.min(z, 1.0D - z));
        return Mth.clamp((float) (1.0D - edge * 2.0D), 0.0F, 1.0F);
    }

    private static float stableCellFloat(int x, int y, int z, int salt) {
        long value = salt;
        value ^= (long) x * 341873128712L;
        value ^= (long) y * 132897987541L;
        value ^= (long) z * 42317861L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (value >>> 40) / (float) (1 << 24);
    }
}