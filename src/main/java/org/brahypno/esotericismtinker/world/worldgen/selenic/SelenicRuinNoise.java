package org.brahypno.esotericismtinker.world.worldgen.selenic;

import net.minecraft.core.BlockPos;

public final class SelenicRuinNoise {
    private SelenicRuinNoise() {}

    public static float value2D(BlockPos pos, int salt, double scale) {
        double x = pos.getX() / scale;
        double z = pos.getZ() / scale;

        int x0 = floor(x);
        int z0 = floor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        float tx = smooth((float) (x - x0));
        float tz = smooth((float) (z - z0));

        float a = hashUnit(x0, z0, salt);
        float b = hashUnit(x1, z0, salt);
        float c = hashUnit(x0, z1, salt);
        float d = hashUnit(x1, z1, salt);

        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    public static float influence(float base, float noise, float influence) {
        return clamp01(base + (noise - 0.5F) * influence);
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float lerp(float a, float b, float delta) {
        return a + (b - a) * delta;
    }

    private static float hashUnit(int x, int z, int salt) {
        long n = 341873128712L * x + 132897987541L * z + 42317861L * salt;
        n ^= n >>> 33;
        n *= 0xff51afd7ed558ccdL;
        n ^= n >>> 33;
        n *= 0xc4ceb9fe1a85ec53L;
        n ^= n >>> 33;

        return (n >>> 40) / (float) (1 << 24);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}