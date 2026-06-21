package org.brahypno.esotericismtinker.selenic.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.selenic.block.entity.TestimonyStandBlockEntity;

public class TestimonyStandBlockEntityRenderer implements BlockEntityRenderer<TestimonyStandBlockEntity> {
    private static final float ITEM_BASE_Y = 0.85F;
    private static final float ITEM_BOB = 0.055F;
    private static final float ITEM_SCALE = 0.80F;
    private static final float ITEM_ROTATION_SPEED = 1.25F;

    public TestimonyStandBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TestimonyStandBlockEntity stand, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (stand.getLevel() == null){
            return;
        }

        ItemStack stack = stand.getTestimony();
        if (stack.isEmpty()){
            return;
        }

        float time = stand.getLevel().getGameTime() + partialTick;
        float bob = (float) Math.sin(time * 0.08F) * ITEM_BOB;
        float rotation = (time * ITEM_ROTATION_SPEED) % 360.0F;

        pose.pushPose();
        pose.translate(0.5D, ITEM_BASE_Y + bob, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                packedOverlay,
                pose,
                buffer,
                stand.getLevel(),
                0
        );

        pose.popPose();
    }
}
