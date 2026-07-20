package org.brahypno.esotericismtinker.transcendence.intrinsic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionData;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;

/**
 * Handles the Noumenon Crown investiture snapshot.
 */
public final class NoumenonInvestitureLogic {
    private NoumenonInvestitureLogic() {}

    public static boolean canChangeInvestiture(NoumenonData data) {
        return data.investedDefinition == null || !data.investitureLocked;
    }

    public static void clearInvestiture(NoumenonData data) {
        data.clearInvestiture();
    }

    public static List<ModifierEntry> listDefinitionTraits(ToolStack sourceTool) {
        validateSourceTool(sourceTool);
        ModifierNBT traits = collectDefinitionTraits(sourceTool);
        if (traits.isEmpty()){
            throw new IllegalArgumentException("Source tool definition has no definition traits to borrow");
        }
        return traits.getModifiers();
    }

    public static Component formatDefinitionTraits(ToolStack sourceTool) {
        List<ModifierEntry> traits = listDefinitionTraits(sourceTool);
        MutableComponent builder = Component.empty();
        for (int i = 0; i < traits.size(); i++) {
            ModifierEntry entry = traits.get(i);
            if (i > 0)
                builder.append(Component.literal(", "));
            builder.append(Component.literal(i + "="));
            builder.append(entry.getDisplayName());
            builder.append(Component.literal("@" + entry.getLevel()));
        }
        return builder;
    }

    public static Component captureOneTraitFromSourceTool(ToolStack targetTool, ToolStack sourceTool, boolean force, int index) {
        NoumenonData current = NoumenonData.read(targetTool);
        if (!force && !canChangeInvestiture(current))
            throw new IllegalStateException("Noumenon investiture is locked");

        validateSourceTool(sourceTool);
        List<ModifierEntry> traits = listDefinitionTraits(sourceTool);
        if (index < 0 || index >= traits.size()){
            throw new IndexOutOfBoundsException(
                    "Investiture trait index " + index + " is out of range 0.." + (traits.size() - 1)
                    + "; use investiture_list_offhand to list available traits");
        }

        ToolDefinition definition = sourceTool.getDefinition();
        ResourceLocation definitionId = definition.getId();
        ModifierEntry selected = traits.get(index);

        return NoumenonAllocationLogic.validateAndApply(targetTool, target -> {
            if (force)
                target.clearInvestiture();
            target.investedDefinition = definitionId;
            target.investitureLocked = true;
            target.investitureRejection = NoumenonDatabase.getInvestitureRejection(definitionId);
            target.investedTraits.clear();
            target.investedTraits.put(selected.getId(), selected.getLevel());
        });
    }

    /**
     * Test-only helper: captures all traits without applying the final GUI selection limit.
     */
    public static Component captureAllTraitsFromSourceTool(ToolStack targetTool, ToolStack sourceTool, boolean force) {
        NoumenonData current = NoumenonData.read(targetTool);
        if (!force && !canChangeInvestiture(current))
            throw new IllegalStateException("Noumenon investiture is locked");

        validateSourceTool(sourceTool);
        List<ModifierEntry> traits = listDefinitionTraits(sourceTool);
        ToolDefinition definition = sourceTool.getDefinition();
        ResourceLocation definitionId = definition.getId();

        return NoumenonAllocationLogic.validateAndApply(targetTool, target -> {
            if (force)
                target.clearInvestiture();
            target.investedDefinition = definitionId;
            target.investitureLocked = true;
            target.investitureRejection = NoumenonDatabase.getInvestitureRejection(definitionId);
            target.investedTraits.clear();
            for (ModifierEntry entry : traits)
                target.investedTraits.put(entry.getId(), entry.getLevel());
        });
    }

    private static void validateSourceTool(ToolStack sourceTool) {
        ToolDefinition definition = sourceTool.getDefinition();
        if (definition == ToolDefinition.EMPTY || !definition.isDataLoaded()){
            throw new IllegalArgumentException("Source tool has no loaded tool definition data");
        }
        ToolDefinitionData data = sourceTool.getDefinitionData();
        if (data == ToolDefinitionData.EMPTY){
            throw new IllegalArgumentException("Source tool definition data is empty");
        }
    }

    private static ModifierNBT collectDefinitionTraits(ToolStack sourceTool) {
        ModifierNBT.Builder builder = ModifierNBT.builder();
        sourceTool.getDefinitionData()
                  .getHook(ToolHooks.TOOL_TRAITS)
                  .addTraits(sourceTool.getDefinition(), MaterialNBT.EMPTY, builder);
        return builder.build();
    }
}
