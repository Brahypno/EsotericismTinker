package org.brahypno.esotericismtinker.transcendence.appearance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unified state transition for the selectable target-stage item.
 * <p>
 * current = StigmataData.stage(), never modifier level.
 * <p>
 * target == current + 1 : upgrade and write the new target stage
 * target == current     : rewrite current stage
 * target < current      : truncate to target; input part is ritual input only
 * target > current + 1  : invalid jump
 */
public final class StigmataLogic {
    private StigmataLogic() {}

    public static StigmataMutationResult applyTarget(
            ToolStack targetTool, ItemStack partStack, StigmataStage targetStage,
            RandomSource random, boolean assignConsequenceSeed) {
        StigmataData original = StigmataData.read(targetTool);
        StigmataData changed = original.copy();

        int current = original.stage();
        int target = targetStage.index();

        if (target > current + 1){
            return StigmataMutationResult.failure(
                    "message.esotericism_tinker.stigmata.invalid_jump",
                    current,
                    target
            );
        }

        if (target < current){
            changed.truncateTo(targetStage);
            changed.write(targetTool);
            ensureModifier(targetTool, targetStage);
            targetTool.rebuildStats();
            return StigmataMutationResult.succeeded();
        }

        StigmataEntry incoming = StigmataPartResolver.resolve(partStack);
        if (null == incoming){
            return StigmataMutationResult.failure(
                    "message.esotericism_tinker.stigmata.not_tool_part"
            );
        }

        ToolDefinition definition = targetTool.getDefinition();
        Set<ResourceLocation> nativeParts = collectNativePartIds(definition);

        boolean isNativePart = nativeParts.isEmpty()
                               ? ToolMaterialHook.stats(definition).contains(incoming.statType())
                               : nativeParts.contains(incoming.partId());

        StigmataMutationResult validation =
                validatePart(
                        targetStage,
                        isNativePart,
                        original,
                        incoming.partId(),
                        partStack.getHoverName()
                );
        if (!validation.success()){
            return validation;
        }

        changed.set(targetStage, incoming);

        if (assignConsequenceSeed && !changed.hasConsequenceSeed()){
            changed.assignConsequenceSeed(random);
        }

        changed.write(targetTool);
        ensureModifier(targetTool, targetStage);
        targetTool.rebuildStats();
        return StigmataMutationResult.succeeded();
    }

    private static StigmataMutationResult validatePart(StigmataStage stage, boolean isNativePart, StigmataData data, ResourceLocation inputPartId, Component inputPartName) {
        return switch (stage) {
            case MANIFESTATION -> isNativePart
                                  ? StigmataMutationResult.succeeded()
                                  : StigmataMutationResult.failure(
                    "message.esotericism_tinker.stigmata.manifestation_requires_native",
                    inputPartName
            );

            case ALIENATION -> !isNativePart
                               ? StigmataMutationResult.succeeded()
                               : StigmataMutationResult.failure(
                    "message.esotericism_tinker.stigmata.alienation_requires_foreign",
                    inputPartName
            );

            case SEALING -> {
                if (!isNativePart){
                    yield StigmataMutationResult.failure("message.esotericism_tinker.stigmata.sealing_requires_native", inputPartName);
                }

                StigmataEntry manifestation = data.get(StigmataStage.MANIFESTATION);
                if (null == manifestation){
                    yield StigmataMutationResult.failure("message.esotericism_tinker.stigmata.missing_manifestation");
                }

                if (manifestation.partId().equals(inputPartId)){
                    yield StigmataMutationResult.failure("message.esotericism_tinker.stigmata.sealing_same_as_manifestation", inputPartName);
                }

                yield StigmataMutationResult.succeeded();
            }
        };
    }

    /**
     * Resolves the exact part identities declared by the tool definition's TOOL_PARTS hook.
     */
    private static Set<ResourceLocation> collectNativePartIds(ToolDefinition definition) {
        return ToolPartsHook.parts(definition).stream()
                            .map(IToolPart::asItem)
                            .map(BuiltInRegistries.ITEM::getKey)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toUnmodifiableSet());
    }

    private static void ensureModifier(ToolStack tool, StigmataStage stage) {
        int level = tool.getModifierLevel(EsotericismTinkerModifiers.STIGMATA);
        if (level < stage.index()){
            tool.addModifier(EsotericismTinkerModifiers.STIGMATA, stage.index() - level);
        }else if (level > stage.index()){
            tool.removeModifier(EsotericismTinkerModifiers.STIGMATA, level - stage.index());
        }
    }
}
