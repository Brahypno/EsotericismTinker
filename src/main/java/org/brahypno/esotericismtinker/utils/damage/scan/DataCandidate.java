package org.brahypno.esotericismtinker.utils.damage.scan;

import net.minecraft.network.syncher.EntityDataAccessor;

public record DataCandidate(String name, EntityDataAccessor<?> accessor, Object value, int score, DataMode mode) {}
