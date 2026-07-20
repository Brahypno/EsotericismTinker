package org.brahypno.esotericismtinker.transcendence.table.block;

import net.minecraft.core.BlockPos;
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
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.block.TinkersAnvilBlock;

import javax.annotation.Nullable;

public final class TranscendenceAnvilBlock extends TinkersAnvilBlock {
    public TranscendenceAnvilBlock(Properties properties) { super(properties, 6); }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TranscendenceAnvilBlockEntity(pos, state); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!stack.hasTag() || !(level.getBlockEntity(pos) instanceof TranscendenceAnvilBlockEntity anvil)) return;
        String texture = RetexturedHelper.getTextureName(stack);
        if (!texture.isEmpty()) anvil.updateTexture(texture);
        else {
            MaterialVariantId material = IMaterialItem.getMaterialFromStack(stack);
            if (material != IMaterial.UNKNOWN_ID) anvil.setMaterial(material);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(state.getBlock());
        if (level.getBlockEntity(pos) instanceof TranscendenceAnvilBlockEntity anvil) {
            Block texture = anvil.getTexture();
            if (texture != Blocks.AIR) RetexturedHelper.setTexture(stack, texture);
            else stack = IMaterialItem.withMaterial(stack, anvil.getMaterial());
        }
        return stack;
    }
}
