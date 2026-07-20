package org.brahypno.esotericismtinker.transcendence.table.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.brahypno.esotericismtinker.transcendence.table.block.TranscendenceAnvilBlockEntity;

public final class TranscendenceAnvilBlockEntityRenderer
        implements BlockEntityRenderer<TranscendenceAnvilBlockEntity> {
    private final ItemRenderer itemRenderer;

    public TranscendenceAnvilBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            TranscendenceAnvilBlockEntity anvil,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack tool = anvil.getItem(0);
        if (tool.isEmpty()) {
            return;
        }

        Direction facing = anvil.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        float rotation = switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };

        pose.pushPose();
        pose.translate(0.5D, 1.03D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.scale(0.55F, 0.55F, 0.55F);

        itemRenderer.renderStatic(
                tool,
                ItemDisplayContext.FIXED,
                getLight(anvil),
                packedOverlay,
                pose,
                buffers,
                anvil.getLevel(),
                (int)anvil.getBlockPos().asLong()
        );
        pose.popPose();
    }

    private static int getLight(TranscendenceAnvilBlockEntity anvil) {
        if (anvil.getLevel() == null) {
            return LightTexture.FULL_BRIGHT;
        }
        int block = anvil.getLevel().getBrightness(
                LightLayer.BLOCK,
                anvil.getBlockPos().above()
        );
        int sky = anvil.getLevel().getBrightness(
                LightLayer.SKY,
                anvil.getBlockPos().above()
        );
        return LightTexture.pack(block, sky);
    }
}
