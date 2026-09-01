package org.brahypno.esotericismtinker.selenic.client.renderer;

/** Fixed unit-circle samples shared across frames; animation stays in the pose transforms. */
final class RingAngles {
    private final float[] cos;
    private final float[] sin;

    RingAngles(int segments) {
        cos = new float[segments + 1];
        sin = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 2.0D * i / segments);
            cos[i] = (float) Math.cos(angle);
            sin[i] = (float) Math.sin(angle);
        }
    }

    int segments() {
        return cos.length - 1;
    }

    float cos(int index) {
        return cos[index];
    }

    float sin(int index) {
        return sin[index];
    }
}
