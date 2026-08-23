package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

import javax.annotation.Nullable;
import java.util.*;

public final class NoumenonSublimationLogic {
    private NoumenonSublimationLogic() {}

    public static List<NoumenonSublimationGroup> resolveGroups(IToolContext context, NoumenonData data) {
        List<NoumenonSublimationGroup> matched = NoumenonDatabase.allSublimationGroups().stream()
                .filter(group -> group.matches(context))
                .toList();

        Set<ResourceLocation> suppressed = new HashSet<>();
        for (NoumenonSublimationGroup group : matched) {
            collectReplaced(group, suppressed, new HashSet<>());
        }

        return matched.stream()
                .filter(group -> !suppressed.contains(group.id()))
                .filter(group -> !NoumenonDatabase.visibleSublimationsInGroup(group.id(), context, data).isEmpty())
                .toList();
    }

    private static void collectReplaced(NoumenonSublimationGroup group,
                                        Set<ResourceLocation> output,
                                        Set<ResourceLocation> visiting) {
        if (!visiting.add(group.id())) return;
        for (ResourceLocation parent : group.replaces()) {
            if (output.add(parent)) {
                NoumenonDatabase.sublimationGroup(parent)
                        .ifPresent(next -> collectReplaced(next, output, visiting));
            }
        }
        visiting.remove(group.id());
    }

    @Nullable
    public static Component validateSelections(IToolContext context, NoumenonData data) {
        Map<ResourceLocation, ResourceLocation> selectedByGroup = new HashMap<>();
        Set<ResourceLocation> resolved = new HashSet<>();
        for (NoumenonSublimationGroup group : resolveGroups(context, data)) {
            resolved.add(group.id());
        }

        for (Map.Entry<ResourceLocation, Integer> chosen : data.sublimations.entrySet()) {
            if (chosen.getValue() <= 0) continue;
            NoumenonSublimationEntry entry = NoumenonDatabase.sublimation(chosen.getKey()).orElse(null);
            if (entry == null) {
                return Component.translatable(
                        "gui.esotericism_tinker.transcendence_anvil.sublimation.unknown",
                        chosen.getKey().toString());
            }
            if (!resolved.contains(entry.groupId()) || !entry.canShow(context, data)) {
                return Component.translatable(
                        "gui.esotericism_tinker.transcendence_anvil.sublimation.unavailable",
                        entry.display().name());
            }
            if (chosen.getValue() > entry.maxLevel()) {
                return Component.translatable(
                        "gui.esotericism_tinker.transcendence_anvil.sublimation.max_level",
                        entry.display().name(), entry.maxLevel());
            }
            ResourceLocation previous = selectedByGroup.putIfAbsent(entry.groupId(), entry.id());
            if (previous != null && !previous.equals(entry.id())) {
                return Component.translatable(
                        "gui.esotericism_tinker.transcendence_anvil.sublimation.group_conflict");
            }
        }
        return null;
    }

    @Nullable
    public static Component mutateSelection(IToolContext context,
                                            NoumenonData baseline,
                                            NoumenonData candidate,
                                            ResourceLocation pathId,
                                            int delta) {
        NoumenonSublimationEntry target = NoumenonDatabase.sublimation(pathId).orElse(null);
        if (target == null || !target.canShow(context, candidate)) {
            return Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.sublimation.unavailable",
                    pathId.toString());
        }

        boolean groupVisible = resolveGroups(context, candidate).stream()
                .anyMatch(group -> group.id().equals(target.groupId()));
        if (!groupVisible) {
            return Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.sublimation.unavailable",
                    target.display().name());
        }

        int initial = baseline.sublimations.getOrDefault(pathId, 0);
        int current = candidate.sublimations.getOrDefault(pathId, 0);
        int next = current + delta;
        if (next < initial || next < 0 || next > target.maxLevel()) {
            return Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.sublimation.invalid_level");
        }

        if (next > 0) {
            for (NoumenonSublimationEntry sibling :
                    NoumenonDatabase.sublimationsInGroup(target.groupId())) {
                if (sibling.id().equals(pathId)) continue;
                if (baseline.sublimations.getOrDefault(sibling.id(), 0) > 0) {
                    return Component.translatable(
                            "gui.esotericism_tinker.transcendence_anvil.sublimation.committed_group");
                }
                candidate.sublimations.remove(sibling.id());
            }
            candidate.sublimations.put(pathId, next);
        } else {
            candidate.sublimations.remove(pathId);
        }
        return validateSelections(context, candidate);
    }
}
