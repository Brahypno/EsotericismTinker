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
import org.brahypno.esotericismtinker.selenic.block.entity.LunarFontBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class LunarFontBlockEntityRenderer implements BlockEntityRenderer<LunarFontBlockEntity> {
    private static final ResourceLocation HALO_TEXTURE = new ResourceLocation("minecraft", "textures/entity/beacon_beam.png");

    private static final float ITEM_BASE_Y = 1.46F;
    private static final float ITEM_SCALE = 0.58F;

    private static final float IDLE_BOB_AMOUNT = 0.035F;
    private static final float ACTIVE_BOB_AMOUNT = 0.075F;

    private static final float IDLE_ITEM_ROTATION_SPEED = 1.35F;
    private static final float ACTIVE_ITEM_ROTATION_SPEED = 3.25F;

    private static final float IDLE_HALO_ROTATION_SPEED = -0.75F;
    private static final float ACTIVE_HALO_ROTATION_SPEED = -3.75F;

    private static final float HALO_BASE_Y = 1.50F;

    private static final float IDLE_HALO_BOB_AMOUNT = 0.010F;
    private static final float ACTIVE_HALO_BOB_AMOUNT = 0.025F;

    private static final float HALO_OUTER_RADIUS = 0.25F;
    private static final float HALO_INNER_RADIUS = 0.18F;
    private static final float HALO_THICKNESS = 0.017F;
    private static final int HALO_SEGMENTS = 48;

    public LunarFontBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            LunarFontBlockEntity font,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {

        boolean active = font.isRitualActive();
        long gameTime = font.getLevel() == null ? 0L : font.getLevel().getGameTime();
        float time = gameTime + partialTick;

        renderFloatingItem(font, time, active, pose, buffer, packedLight, packedOverlay);
        renderRotatingHalo(time, active, pose, buffer, packedLight, packedOverlay);
    }

    private void renderFloatingItem(
            LunarFontBlockEntity font,
            float time,
            boolean active,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {

        ItemStack stack = font.getOutputItemView();
        if (stack.isEmpty()){
            return;
        }

        float bobAmount = active ? ACTIVE_BOB_AMOUNT : IDLE_BOB_AMOUNT;
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
                font.getLevel(),
                0);

        pose.popPose();
    }

    private void renderRotatingHalo(float time, boolean active, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {

        float bobAmount = active ? ACTIVE_HALO_BOB_AMOUNT : IDLE_HALO_BOB_AMOUNT;
        float rotationSpeed = active ? ACTIVE_HALO_ROTATION_SPEED : IDLE_HALO_ROTATION_SPEED;
        float bob = (float) Math.sin(time * 0.09F + 1.4F) * bobAmount;
        float rotation = (time * rotationSpeed) % 360.0F;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(HALO_TEXTURE));

        pose.pushPose();
        pose.translate(0.5D, HALO_BASE_Y + bob, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));

        renderHaloRing(pose, consumer, HALO_OUTER_RADIUS, HALO_INNER_RADIUS, HALO_THICKNESS, HALO_SEGMENTS, active ? 210 : 150, LightTexture.FULL_BRIGHT,
                       packedOverlay);

        pose.popPose();
    }

    private void renderHaloRing(PoseStack pose, VertexConsumer consumer, float outerRadius, float innerRadius, float thickness, int segments, int alpha, int light, int overlay) {

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
            haloVertex(consumer, matrix, normal, innerX0, topY, innerZ0, u0, 0.15F, alpha, light, overlay, 0, 1, 0);
            haloVertex(consumer, matrix, normal, innerX1, topY, innerZ1, u1, 0.15F, alpha, light, overlay, 0, 1, 0);
            haloVertex(consumer, matrix, normal, outerX1, topY, outerZ1, u1, 0.85F, alpha, light, overlay, 0, 1, 0);
            haloVertex(consumer, matrix, normal, outerX0, topY, outerZ0, u0, 0.85F, alpha, light, overlay, 0, 1, 0);

            // bottom face
            haloVertex(consumer, matrix, normal, outerX0, bottomY, outerZ0, u0, 0.85F, alpha, light, overlay, 0, -1, 0);
            haloVertex(consumer, matrix, normal, outerX1, bottomY, outerZ1, u1, 0.85F, alpha, light, overlay, 0, -1, 0);
            haloVertex(consumer, matrix, normal, innerX1, bottomY, innerZ1, u1, 0.15F, alpha, light, overlay, 0, -1, 0);
            haloVertex(consumer, matrix, normal, innerX0, bottomY, innerZ0, u0, 0.15F, alpha, light, overlay, 0, -1, 0);

            // outer wall
            haloVertex(consumer, matrix, normal, outerX0, bottomY, outerZ0, u0, 0.0F, alpha, light, overlay, cos0, 0, sin0);
            haloVertex(consumer, matrix, normal, outerX1, bottomY, outerZ1, u1, 0.0F, alpha, light, overlay, cos1, 0, sin1);
            haloVertex(consumer, matrix, normal, outerX1, topY, outerZ1, u1, 1.0F, alpha, light, overlay, cos1, 0, sin1);
            haloVertex(consumer, matrix, normal, outerX0, topY, outerZ0, u0, 1.0F, alpha, light, overlay, cos0, 0, sin0);

            // inner wall
            haloVertex(consumer, matrix, normal, innerX1, bottomY, innerZ1, u1, 0.0F, alpha, light, overlay, -cos1, 0, -sin1);
            haloVertex(consumer, matrix, normal, innerX0, bottomY, innerZ0, u0, 0.0F, alpha, light, overlay, -cos0, 0, -sin0);
            haloVertex(consumer, matrix, normal, innerX0, topY, innerZ0, u0, 1.0F, alpha, light, overlay, -cos0, 0, -sin0);
            haloVertex(consumer, matrix, normal, innerX1, topY, innerZ1, u1, 1.0F, alpha, light, overlay, -cos1, 0, -sin1);
        }
    }

    private void haloVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float z, float u, float v, int alpha, int light, int overlay, float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, x, y, z)
                .color(170, 220, 255, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}