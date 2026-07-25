package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum StigmataOverloadDegree {
    SLIGHT,
    MODERATE,
    SEVERE,
    CRITICAL;

    public static StigmataOverloadDegree fromValues(int overload, int burden) {
        if (0 >= overload || 0 >= burden){
            throw new IllegalArgumentException("Tool is not overloaded");
        }

        long scaled = (long) overload * 4L;

        if (scaled <= burden)
            return SLIGHT;
        if (scaled <= (long) burden * 2L)
            return MODERATE;
        if (scaled <= (long) burden * 3L)
            return SEVERE;
        return CRITICAL;
    }

    public Component displayName() {
        return Component.translatable(
                "modifier.esotericism_tinker.stigmata.overload."
                + name().toLowerCase(Locale.ROOT)
        );
    }
}
