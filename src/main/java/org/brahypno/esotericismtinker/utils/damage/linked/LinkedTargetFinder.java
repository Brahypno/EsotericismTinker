package org.brahypno.esotericismtinker.utils.damage.linked;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class LinkedTargetFinder {
    private static final int MAX_DEPTH = 2;
    private static final int MAX_TARGETS = 8;
    private static final int MAX_OBJECTS = 48;

    private LinkedTargetFinder() {}

    public static List<LivingEntity> find(Entity root, LivingEntity victim, DamageSource source, Consumer<String> log) {
        if (root == null || victim == null) return Collections.emptyList();

        List<LivingEntity> targets = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<Node> queue = new ArrayDeque<>();

        queue.add(new Node(victim, 0, "victim"));
        if (root != victim) queue.add(new Node(root, 0, "root"));

        int objects = 0;
        while (!queue.isEmpty() && targets.size() < MAX_TARGETS && objects++ < MAX_OBJECTS) {
            Node node = queue.removeFirst();
            if (node.value() == null || !visited.add(node.value())) continue;
            scanObject(root, victim, source, node, targets, queue, visited, log);
        }

        log.accept("linked_target_probe: foundValid=" + targets.size());
        return targets;
    }

    private static void scanObject(Entity root, LivingEntity victim, DamageSource source, Node node, List<LivingEntity> targets, ArrayDeque<Node> queue, Set<Object> visited, Consumer<String> log) {
        Class<?> cls = node.value().getClass();
        while (LinkedTargetPolicy.canScanDeclaringClass(cls, victim.getClass()) || LinkedTargetPolicy.canScanContainerClass(cls)) {
            for (Field field : cls.getDeclaredFields()) scanField(root, victim, source, node, field, targets, queue, visited, log);
            cls = cls.getSuperclass();
        }
    }

    private static void scanField(Entity root, LivingEntity victim, DamageSource source, Node node, Field field, List<LivingEntity> targets, ArrayDeque<Node> queue, Set<Object> visited, Consumer<String> log) {
        try {
            if (!LinkedTargetPolicy.canReadField(field)) return;
            if (!LinkedTargetPolicy.isLinkedTargetField(field) && !LinkedTargetPolicy.isContainerField(field)) return;

            field.setAccessible(true);
            Object value = field.get(node.value());
            if (value == null) return;

            if (value instanceof LivingEntity candidate) {
                if (LinkedTargetPolicy.isForbiddenCandidate(root, victim, candidate, source, log)) return;
                if (!targets.contains(candidate)) {
                    targets.add(candidate);
                    log.accept("linked_target_valid field " + field.getDeclaringClass().getSimpleName() + "#" + field.getName() + " -> " + LinkedTargetPolicy.describe(candidate));
                }
                return;
            }

            if (node.depth() >= MAX_DEPTH || visited.contains(value)) return;
            if (!LinkedTargetPolicy.isContainerField(field) && !LinkedTargetPolicy.canScanContainerClass(value.getClass())) return;

            queue.addLast(new Node(value, node.depth() + 1, node.path() + "." + field.getName()));
            log.accept("linked_target_container field " + field.getDeclaringClass().getSimpleName() + "#" + field.getName() + " -> " + value.getClass().getName());
        } catch (Throwable e) {
            log.accept("linked_target_field error " + field.getDeclaringClass().getSimpleName() + "#" + field.getName() + ": " + e.getClass().getSimpleName());
        }
    }

    private record Node(Object value, int depth, String path) {}
}
