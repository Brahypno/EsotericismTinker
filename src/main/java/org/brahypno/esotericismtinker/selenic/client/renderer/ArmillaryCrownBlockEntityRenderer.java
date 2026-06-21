package org.brahypno.esotericismtinker.selenic.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.selenic.block.entity.ArmillaryCrownBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ArmillaryCrownBlockEntityRenderer implements BlockEntityRenderer<ArmillaryCrownBlockEntity> {
    private static final ResourceLocation RING_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/beacon_beam.png");


    private static final float IDLE_ITEM_BOB = 0.025F;
    private static final float ACTIVE_ITEM_BOB = 0.055F;

    private static final float IDLE_ITEM_ROTATION_SPEED = 1.25F;
    private static final float ACTIVE_ITEM_ROTATION_SPEED = 3.25F;

    private static final float HORIZONTAL_RING_Y = 0.58F;
    private static final float VERTICAL_RING_Y = 0.58F;

    private static final int RING_SEGMENTS = 64;

    private static final float IDLE_HORIZONTAL_RING_SPEED = 0.45F;
    private static final float ACTIVE_HORIZONTAL_RING_SPEED = 2.0F;

    private static final float IDLE_VERTICAL_RING_SPEED = -0.35F;
    private static final float ACTIVE_VERTICAL_RING_SPEED = -1.75F;
    private static final float RING_CENTER_Y = 1.18F;

    private static final float ITEM_BASE_Y = RING_CENTER_Y;
    private static final float ITEM_SCALE = 0.48F;

    private static final float RING_OUTER_RADIUS = 0.47F;
    private static final float RING_INNER_RADIUS = 0.415F;
    private static final float RING_THICKNESS = 0.035F;

    public ArmillaryCrownBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            ArmillaryCrownBlockEntity crown,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (crown.getLevel() == null){
            return;
        }

        float time = crown.getLevel().getGameTime() + partialTick;
        boolean active = crown.isDisplayActive();

        renderInputItem(crown, time, active, pose, buffer, packedLight, packedOverlay);
        renderHorizontalRing(time, active, pose, buffer, packedLight, packedOverlay);
        renderVerticalRingNS(time, active, pose, buffer, packedLight, packedOverlay);
        renderVerticalRingEW(time, active, pose, buffer, packedLight, packedOverlay);
    }

    private void renderInputItem(ArmillaryCrownBlockEntity crown, float time, boolean active, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack stack = crown.getDisplayedInputItem();
        if (stack.isEmpty()){
            return;
        }

        float bobAmount = active ? ACTIVE_ITEM_BOB : IDLE_ITEM_BOB;
        float rotationSpeed = active ? ACTIVE_ITEM_ROTATION_SPEED : IDLE_ITEM_ROTATION_SPEED;
        float bob = (float) Math.sin(time * 0.09F) * bobAmount;
        float rotation = (time * rotationSpeed) % 360.0F;
        int light = active ? LightTexture.FULL_BRIGHT : packedLight;

        pose.pushPose();
        pose.translate(0.5D, ITEM_BASE_Y + bob, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                light,
                packedOverlay,
                pose,
                buffer,
                crown.getLevel(),
                0
        );

        pose.popPose();
    }

    private void renderHorizontalRing(float time, boolean active, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float speed = active ? 2.0F : 0.45F;
        float rotation = (time * speed) % 360.0F;
        int alpha = active ? 210 : 135;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(RING_TEXTURE));

        pose.pushPose();
        pose.translate(0.5D, RING_CENTER_Y, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));

        renderRing(
                pose,
                consumer,
                RING_OUTER_RADIUS,
                RING_INNER_RADIUS,
                RING_THICKNESS,
                RING_SEGMENTS,
                alpha,
                LightTexture.FULL_BRIGHT,
                packedOverlay
        );

        pose.popPose();
    }

    private void renderVerticalRingNS(
            float time,
            boolean active,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        float speed = active ? -1.75F : -0.35F;
        float rotation = (time * speed) % 360.0F;
        int alpha = active ? 190 : 115;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(RING_TEXTURE));

        pose.pushPose();
        pose.translate(0.5D, RING_CENTER_Y, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));

        renderRing(
                pose,
                consumer,
                RING_OUTER_RADIUS,
                RING_INNER_RADIUS,
                RING_THICKNESS,
                RING_SEGMENTS,
                alpha,
                LightTexture.FULL_BRIGHT,
                packedOverlay
        );

        pose.popPose();
    }

    private void renderVerticalRingEW(
            float time,
            boolean active,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        float speed = active ? 1.45F : 0.28F;
        float rotation = (time * speed + 90.0F) % 360.0F;
        int alpha = active ? 185 : 105;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(RING_TEXTURE));

        pose.pushPose();
        pose.translate(0.5D, RING_CENTER_Y, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));

        renderRing(
                pose,
                consumer,
                RING_OUTER_RADIUS,
                RING_INNER_RADIUS,
                RING_THICKNESS,
                RING_SEGMENTS,
                alpha,
                LightTexture.FULL_BRIGHT,
                packedOverlay
        );

        pose.popPose();
    }

    private void renderRing(PoseStack pose, VertexConsumer consumer, float outerRadius, float innerRadius, float thickness, int segments, int alpha, int light, int overlay) {
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();

        float halfThickness = thickness * 0.5F;
        float topY = halfThickness;
        float bottomY = -halfThickness;

        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / segments);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / segments);

            float cos0 = (float) Math.cos(a0);
            float sin0 = (float) Math.sin(a0);
            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);

            float outerX0 = cos0 * outerRadius;
            float outerZ0 = sin0 * outerRadius;
            float outerX1 = cos1 * outerRadius;
            float outerZ1 = sin1 * outerRadius;

            float innerX0 = cos0 * innerRadius;
            float innerZ0 = sin0 * innerRadius;
            float innerX1 = cos1 * innerRadius;
            float innerZ1 = sin1 * innerRadius;

            float u0 = i / (float) segments;
            float u1 = (i + 1) / (float) segments;

            // top face
            ringVertex(consumer, matrix, normal, innerX0, topY, innerZ0, u0, 0.2F, alpha, light, overlay, 0, 1, 0);
            ringVertex(consumer, matrix, normal, innerX1, topY, innerZ1, u1, 0.2F, alpha, light, overlay, 0, 1, 0);
            ringVertex(consumer, matrix, normal, outerX1, topY, outerZ1, u1, 0.8F, alpha, light, overlay, 0, 1, 0);
            ringVertex(consumer, matrix, normal, outerX0, topY, outerZ0, u0, 0.8F, alpha, light, overlay, 0, 1, 0);

            // bottom face
            ringVertex(consumer, matrix, normal, outerX0, bottomY, outerZ0, u0, 0.8F, alpha, light, overlay, 0, -1, 0);
            ringVertex(consumer, matrix, normal, outerX1, bottomY, outerZ1, u1, 0.8F, alpha, light, overlay, 0, -1, 0);
            ringVertex(consumer, matrix, normal, innerX1, bottomY, innerZ1, u1, 0.2F, alpha, light, overlay, 0, -1, 0);
            ringVertex(consumer, matrix, normal, innerX0, bottomY, innerZ0, u0, 0.2F, alpha, light, overlay, 0, -1, 0);

            // outer wall
            ringVertex(consumer, matrix, normal, outerX0, bottomY, outerZ0, u0, 0.0F, alpha, light, overlay, cos0, 0, sin0);
            ringVertex(consumer, matrix, normal, outerX1, bottomY, outerZ1, u1, 0.0F, alpha, light, overlay, cos1, 0, sin1);
            ringVertex(consumer, matrix, normal, outerX1, topY, outerZ1, u1, 1.0F, alpha, light, overlay, cos1, 0, sin1);
            ringVertex(consumer, matrix, normal, outerX0, topY, outerZ0, u0, 1.0F, alpha, light, overlay, cos0, 0, sin0);

            // inner wall
            ringVertex(consumer, matrix, normal, innerX1, bottomY, innerZ1, u1, 0.0F, alpha, light, overlay, -cos1, 0, -sin1);
            ringVertex(consumer, matrix, normal, innerX0, bottomY, innerZ0, u0, 0.0F, alpha, light, overlay, -cos0, 0, -sin0);
            ringVertex(consumer, matrix, normal, innerX0, topY, innerZ0, u0, 1.0F, alpha, light, overlay, -cos0, 0, -sin0);
            ringVertex(consumer, matrix, normal, innerX1, topY, innerZ1, u1, 1.0F, alpha, light, overlay, -cos1, 0, -sin1);
        }
    }

    private void ringVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            float x,
            float y,
            float z,
            float u,
            float v,
            int alpha,
            int light,
            int overlay,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(170, 220, 255, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}