package org.brahypno.esotericismtinker.world.worldgen.transmute;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class TransmuteSavedEntityPlacer {

    private TransmuteSavedEntityPlacer() {}

    public static void spawnSavedEntities(
            WorldGenLevel level,
            BoundingBox processBox,
            StructureTemplate template,
            StructurePlaceSettings settings,
            BlockPos placePos
    ) {
        List<StructureTemplate.StructureEntityInfo> entities = template.entityInfoList;

        for (StructureTemplate.StructureEntityInfo info : entities) {
            CompoundTag rawNbt = info.nbt;
            String id = rawNbt.getString("id");

            if (isSupportedFrame(id)){
                spawnSavedFrame(level, processBox, info, settings, placePos);
                continue;
            }

            if (id.equals("minecraft:zombie_villager")){
                spawnNaturalZombieVillager(level, processBox, info, settings, placePos);
            }
        }
    }

    private static boolean isSupportedFrame(String id) {
        return id.equals("minecraft:item_frame")
               || id.equals("minecraft:glow_item_frame")
               || id.equals("tconstruct:fancy_item_frame");
    }

    private static void spawnSavedFrame(
            WorldGenLevel level,
            BoundingBox processBox,
            StructureTemplate.StructureEntityInfo info,
            StructurePlaceSettings settings,
            BlockPos placePos
    ) {
        CompoundTag rawNbt = info.nbt;
        BlockPos tilePos = transformBlockPos(info.blockPos, settings, placePos);

        if (isNotInside(processBox, tilePos)){
            return;
        }

        Vec3 entityPos = transformVec(info.pos, settings, placePos);
        CompoundTag entityNbt = transformHangingNbt(rawNbt, tilePos, entityPos, settings);

        Direction facing = readFacing(entityNbt);
        BlockPos supportPos = tilePos.relative(facing.getOpposite());

        if (isNotInside(processBox, supportPos)){
            return;
        }

        if (!canPlaceHangingEntity(level, tilePos, facing)){
            return;
        }

        Optional<Entity> entity = EntityType.create(entityNbt, level.getLevel());
        if (entity.isEmpty()){
            return;
        }

        Entity created = entity.get();
        if (created instanceof HangingEntity hanging && !hanging.survives()){
            return;
        }

        level.addFreshEntity(created);
    }

    private static final MobSpawnType ZOMBIE_VILLAGER_SPAWN_TYPE = MobSpawnType.STRUCTURE;

    private static void spawnNaturalZombieVillager(
            WorldGenLevel level,
            BoundingBox processBox,
            StructureTemplate.StructureEntityInfo info,
            StructurePlaceSettings settings,
            BlockPos placePos
    ) {
        Vec3 entityPos = transformVec(info.pos, settings, placePos);
        BlockPos blockPos = BlockPos.containing(entityPos);

        if (isNotInside(processBox, blockPos)){
            return;
        }

        if (!canSpawnZombieVillagerAt(level, blockPos)){
            return;
        }

        ZombieVillager zombie = EntityType.ZOMBIE_VILLAGER.create(level.getLevel());
        if (zombie == null){
            return;
        }

        float yaw = readYaw(info.nbt) + yawOffset(settings.getRotation());
        yaw = Mth.wrapDegrees(yaw);

        zombie.moveTo(
                entityPos.x(),
                entityPos.y(),
                entityPos.z(),
                yaw,
                0.0F
        );
        zombie.setYHeadRot(yaw);
        zombie.setYBodyRot(yaw);

        DifficultyInstance difficulty = level.getCurrentDifficultyAt(blockPos);

        SpawnGroupData spawnData = zombie.finalizeSpawn(
                level,
                difficulty,
                ZOMBIE_VILLAGER_SPAWN_TYPE,
                null,
                null
        );

        // spawnData 当前不用，但保留局部变量方便调试，也避免你之后要扩展 group spawn 逻辑。
        zombie.setPersistenceRequired();

        level.addFreshEntity(zombie);
    }

    private static boolean canSpawnZombieVillagerAt(
            WorldGenLevel level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);

        return state.getCollisionShape(level, pos).isEmpty()
               && above.getCollisionShape(level, pos.above()).isEmpty()
               && below.isFaceSturdy(level, belowPos, Direction.UP);
    }

    private static CompoundTag transformHangingNbt(
            CompoundTag rawNbt,
            BlockPos tilePos,
            Vec3 entityPos,
            StructurePlaceSettings settings
    ) {
        CompoundTag nbt = rawNbt.copy();

        Direction rawFacing = readFacing(rawNbt);
        Direction fixedFacing = rotateFacing(rawFacing, settings.getRotation(), settings.getMirror());

        nbt.remove("UUID");
        nbt.put("Pos", newDoubleList(entityPos.x(), entityPos.y(), entityPos.z()));
        nbt.putInt("TileX", tilePos.getX());
        nbt.putInt("TileY", tilePos.getY());
        nbt.putInt("TileZ", tilePos.getZ());
        nbt.putByte("Facing", (byte) fixedFacing.get3DDataValue());

        rotateEntityYaw(nbt, settings.getRotation());

        return nbt;
    }

    private static Direction readFacing(CompoundTag nbt) {
        if (!nbt.contains("Facing", Tag.TAG_BYTE)){
            return Direction.NORTH;
        }

        return Direction.from3DDataValue(nbt.getByte("Facing"));
    }

    private static Direction rotateFacing(
            Direction facing,
            Rotation rotation,
            Mirror mirror
    ) {
        Direction mirrored = mirror.mirror(facing);
        return rotation.rotate(mirrored);
    }

    private static BlockPos transformBlockPos(
            BlockPos localPos,
            StructurePlaceSettings settings,
            BlockPos placePos
    ) {
        return StructureTemplate.calculateRelativePosition(settings, localPos).offset(placePos);
    }

    private static Vec3 transformVec(
            Vec3 localPos,
            StructurePlaceSettings settings,
            BlockPos placePos
    ) {
        Vec3 transformed = StructureTemplate.transformedVec3d(settings, localPos);

        return transformed.add(
                placePos.getX(),
                placePos.getY(),
                placePos.getZ()
        );
    }

    private static boolean canPlaceHangingEntity(
            WorldGenLevel level,
            BlockPos tilePos,
            Direction facing
    ) {
        BlockPos supportPos = tilePos.relative(facing.getOpposite());
        BlockState support = level.getBlockState(supportPos);

        return !support.isAir()
               && support.isFaceSturdy(level, supportPos, facing);
    }

    private static boolean isNotInside(BoundingBox box, BlockPos pos) {
        return pos.getX() < box.minX()
               || pos.getX() > box.maxX()
               || pos.getY() < box.minY()
               || pos.getY() > box.maxY()
               || pos.getZ() < box.minZ()
               || pos.getZ() > box.maxZ();
    }

    private static void rotateEntityYaw(CompoundTag nbt, Rotation rotation) {
        if (!nbt.contains("Rotation", Tag.TAG_LIST)){
            return;
        }

        ListTag rotationList = nbt.getList("Rotation", Tag.TAG_FLOAT);
        if (rotationList.size() < 2){
            return;
        }

        float yaw = rotationList.getFloat(0);
        float pitch = rotationList.getFloat(1);
        float fixedYaw = yaw + yawOffset(rotation);

        nbt.put("Rotation", newFloatList(Mth.wrapDegrees(fixedYaw), pitch));
    }

    private static float readYaw(CompoundTag nbt) {
        if (!nbt.contains("Rotation", Tag.TAG_LIST)){
            return 0.0F;
        }

        ListTag rotationList = nbt.getList("Rotation", Tag.TAG_FLOAT);
        if (rotationList.isEmpty()){
            return 0.0F;
        }

        return rotationList.getFloat(0);
    }

    private static float yawOffset(Rotation rotation) {
        return switch (rotation) {
            case NONE -> 0.0F;
            case CLOCKWISE_90 -> 90.0F;
            case CLOCKWISE_180 -> 180.0F;
            case COUNTERCLOCKWISE_90 -> -90.0F;
        };
    }

    private static ListTag newDoubleList(double... values) {
        ListTag list = new ListTag();

        for (double value : values) {
            list.add(DoubleTag.valueOf(value));
        }

        return list;
    }

    private static ListTag newFloatList(float... values) {
        ListTag list = new ListTag();

        for (float value : values) {
            list.add(FloatTag.valueOf(value));
        }

        return list;
    }
}