package org.brahypno.esotericismtinker.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.EsotericismTinker;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;


public class ETHelper {
    public static final double MIN_PROJECTILE_SPEED_SQR = 1.0E-6D;
    public static final double PROJECTILE_SPAWN_EXTRA_DISTANCE = 0.45D;

    public static void placeProjectileOutsideShooter(Projectile projectile, LivingEntity shooter, Vec3 direction) {
        double distance = shooter.getBbWidth() * 0.5D
                          + projectile.getBbWidth() * 0.5D
                          + PROJECTILE_SPAWN_EXTRA_DISTANCE;

        Vec3 eye = shooter.getEyePosition();

        for (int i = 0; i < 8; i++) {
            Vec3 pos = eye.add(direction.scale(distance));
            projectile.setPos(pos.x, pos.y - projectile.getBbHeight() * 0.5D, pos.z);

            if (!projectile.getBoundingBox().intersects(shooter.getBoundingBox()))
                return;

            distance += 0.25D;
        }
    }

    @Nullable
    public static LivingEntity getLivingTarget(@Nullable Entity target) {
        if (target instanceof LivingEntity living){
            return living;
        }
        if (target instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity living){
            return living;
        }
        return null;
    }

    private static boolean isStalledProjectile(Projectile projectile) {
        return projectile.isAlive()
               && projectile.getDeltaMovement().lengthSqr() <= MIN_PROJECTILE_SPEED_SQR;
    }

    public static void debugEffects(List<MobEffect> effects) {
        for (MobEffect effect : effects) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
            String key = effect.getDescriptionId();
            EsotericismTinker.LOGGER.debug("Random effect -> {} ({})", id, key);
        }
    }


    public static boolean containsTranslationKey(Component root, String targetKey) {
        if (root == null || targetKey == null){
            return false;
        }

        if (hasTranslationKey(root, targetKey)){
            return true;
        }

        ComponentContents contents = root.getContents();
        if (contents instanceof TranslatableContents translatable){
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component argComponent && containsTranslationKey(argComponent, targetKey)){
                    return true;
                }
            }
        }

        for (Component sibling : root.getSiblings()) {
            if (containsTranslationKey(sibling, targetKey)){
                return true;
            }
        }

        return false;
    }

    private static boolean hasTranslationKey(Component component, String targetKey) {
        return component.getContents() instanceof TranslatableContents translatable
               && targetKey.equals(translatable.getKey());
    }


    public static float getPositiveAttributeBonus(LivingEntity entity, Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null){
            return 0.0f;
        }

        double base = instance.getBaseValue();

        double addition = 0.0D;
        double multiplyBase = 0.0D;
        double multiplyTotalFactor = 1.0D;

        for (AttributeModifier modifier : instance.getModifiers()) {
            double amount = modifier.getAmount();

            // 只统计正向 modifier，忽略 debuff / 负数 modifier
            if (amount <= 0.0D){
                continue;
            }

            switch (modifier.getOperation()) {
                case ADDITION -> {
                    addition += amount;
                }
                case MULTIPLY_BASE -> {
                    multiplyBase += base * amount;
                }
                case MULTIPLY_TOTAL -> {
                    multiplyTotalFactor *= 1.0D + amount;
                }
            }
        }

        double valueBeforeTotalMultiplier = base + addition + multiplyBase;
        double positiveOnlyValue = valueBeforeTotalMultiplier * multiplyTotalFactor;

        return (float) Math.max(0.0D, positiveOnlyValue);
    }

    public static double getMultipartVolume(Entity target) {
        Entity root = target instanceof PartEntity<?> part ? part.getParent() : target;

        PartEntity<?>[] parts = root.getParts();
        if (parts == null){
            return getBoxVolume(root.getBoundingBox());
        }

        double volume = 0.0D;

        for (PartEntity<?> part : parts) {
            if (part != null){
                volume += getBoxVolume(part.getBoundingBox());
            }
        }

        return volume;
    }

    public static double getBoxVolume(AABB box) {
        return box.getXsize() * box.getYsize() * box.getZsize();
    }

    public static int autoEndColor(int argb, float valueMul, float saturationMul, float alphaMul) {
        int a = (argb >>> 24) & 255;
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;

        float[] hsv = rgbToHsv(r / 255f, g / 255f, b / 255f);

        hsv[1] = clamp01(hsv[1] * saturationMul);
        hsv[2] = clamp01(hsv[2] * valueMul);

        int rgb = hsvToRgb(hsv[0], hsv[1], hsv[2]);
        int na = clamp255(Math.round(a * alphaMul));

        return (na << 24) | rgb;
    }

    private static float[] rgbToHsv(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0f;

        if (d > 1.0E-6f){
            if (max == r)
                h = ((g - b) / d) % 6f;
            else if (max == g)
                h = (b - r) / d + 2f;
            else
                h = (r - g) / d + 4f;
            h /= 6f;
            if (h < 0f)
                h += 1f;
        }

        float s = max == 0f ? 0f : d / max;
        return new float[]{h, s, max};
    }

    private static int hsvToRgb(float h, float s, float v) {
        float i = (float) Math.floor(h * 6f);
        float f = h * 6f - i;
        float p = v * (1f - s);
        float q = v * (1f - f * s);
        float t = v * (1f - (1f - f) * s);

        float r, g, b;
        switch (((int) i) % 6) {
            case 0 -> {
                r = v;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = v;
                b = p;
            }
            case 2 -> {
                r = p;
                g = v;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = v;
            }
            case 4 -> {
                r = t;
                g = p;
                b = v;
            }
            default -> {
                r = v;
                g = p;
                b = q;
            }
        }

        return (clamp255(Math.round(r * 255f)) << 16) |
               (clamp255(Math.round(g * 255f)) << 8) |
               clamp255(Math.round(b * 255f));
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static int materialToRender(int rgb, MaterialVariant variant) {
        Optional<MaterialRenderInfo> infoOptional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(variant.getVariant());
        if (infoOptional.isPresent()){
            MaterialRenderInfo info = infoOptional.get();
            rgb = info.vertexColor();
        }
        return rgb;
    }
}
