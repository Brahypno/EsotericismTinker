package org.brahypno.esotericismtinker.utils.damage.scan;

import java.lang.reflect.Field;

public record FieldCandidate(Field field, String name, Object value, int score, DataMode mode) {}
