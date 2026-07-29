package org.brahypno.esotericismtinker.transcendence.table.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.block.TinkersAnvilBlock;

import javax.annotation.Nullable;

public final class TranscendenceAnvilBlock extends TinkersAnvilBlock {
    public TranscendenceAnvilBlock(Properties properties) {super(properties, 6);}

    private static final VoxelShape Z_AXIS_SHAPE = Shapes.or(
            // Continuous transverse base
            Block.box(1.0D, 0.0D, 5.0D, 15.0D, 3.0D, 11.0D),

            // Continuous longitudinal base
            Block.box(5.0D, 0.0D, 1.0D, 11.0D, 3.0D, 15.0D),

            // Lower plinth
            Block.box(3.0D, 3.0D, 3.0D, 13.0D, 5.0D, 13.0D),

            // Narrow waist
            Block.box(5.0D, 5.0D, 5.0D, 11.0D, 9.0D, 11.0D),

            // Broad shoulder
            Block.box(3.0D, 9.0D, 3.0D, 13.0D, 11.0D, 13.0D),

            // Lower longitudinal anvil body
            Block.box(4.0D, 11.0D, 0.0D, 12.0D, 13.0D, 16.0D),

            // Lower transverse anvil body
            Block.box(1.0D, 11.0D, 3.0D, 15.0D, 13.0D, 13.0D),

            // Upper longitudinal working surface
            Block.box(3.0D, 13.0D, 0.0D, 13.0D, 16.0D, 16.0D),

            // Upper transverse working surface
            Block.box(0.0D, 13.0D, 4.0D, 16.0D, 16.0D, 12.0D)
    );

    private static final VoxelShape X_AXIS_SHAPE = Shapes.or(
            // Rotated base cross
            Block.box(5.0D, 0.0D, 1.0D, 11.0D, 3.0D, 15.0D),
            Block.box(1.0D, 0.0D, 5.0D, 15.0D, 3.0D, 11.0D),

            // Lower plinth
            Block.box(3.0D, 3.0D, 3.0D, 13.0D, 5.0D, 13.0D),

            // Narrow waist
            Block.box(5.0D, 5.0D, 5.0D, 11.0D, 9.0D, 11.0D),

            // Broad shoulder
            Block.box(3.0D, 9.0D, 3.0D, 13.0D, 11.0D, 13.0D),

            // Rotated lower longitudinal body
            Block.box(0.0D, 11.0D, 4.0D, 16.0D, 13.0D, 12.0D),

            // Rotated lower transverse body
            Block.box(3.0D, 11.0D, 1.0D, 13.0D, 13.0D, 15.0D),

            // Rotated upper longitudinal surface
            Block.box(0.0D, 13.0D, 3.0D, 16.0D, 16.0D, 13.0D),

            // Rotated upper transverse surface
            Block.box(4.0D, 13.0D, 0.0D, 12.0D, 16.0D, 16.0D)
    );

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        Direction direction = state.getValue(FACING);
        return direction.getAxis() == Direction.Axis.X
               ? X_AXIS_SHAPE
               : Z_AXIS_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {return new TranscendenceAnvilBlockEntity(pos, state);}

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!stack.hasTag() || !(level.getBlockEntity(pos) instanceof TranscendenceAnvilBlockEntity anvil))
            return;
        String texture = RetexturedHelper.getTextureName(stack);
        if (!texture.isEmpty())
            anvil.updateTexture(texture);
        else {
            MaterialVariantId material = IMaterialItem.getMaterialFromStack(stack);
            if (material != IMaterial.UNKNOWN_ID)
                anvil.setMaterial(material);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(state.getBlock());
        if (level.getBlockEntity(pos) instanceof TranscendenceAnvilBlockEntity anvil){
            Block texture = anvil.getTexture();
            if (texture != Blocks.AIR)
                RetexturedHelper.setTexture(stack, texture);
            else
                stack = IMaterialItem.withMaterial(stack, anvil.getMaterial());
        }
        return stack;
    }
}
