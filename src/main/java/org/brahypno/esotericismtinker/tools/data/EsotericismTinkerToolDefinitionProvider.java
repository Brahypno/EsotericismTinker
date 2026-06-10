package org.brahypno.esotericismtinker.tools.data;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ToolActions;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerToolDefinitions;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.data.tinkering.AbstractToolDefinitionDataProvider;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.tools.definition.module.build.MultiplyStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.SetStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolActionsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitsModule;
import slimeknights.tconstruct.library.tools.definition.module.display.FixedMaterialToolName;
import slimeknights.tconstruct.library.tools.definition.module.material.DefaultMaterialsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.PartStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningSpeedModifierModule;
import slimeknights.tconstruct.library.tools.nbt.MultiplierNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.data.ModifierIds;

import static slimeknights.tconstruct.tools.TinkerToolParts.smallBlade;
import static slimeknights.tconstruct.tools.TinkerToolParts.toolHandle;

public class EsotericismTinkerToolDefinitionProvider extends AbstractToolDefinitionDataProvider {
    public EsotericismTinkerToolDefinitionProvider(PackOutput packOutput) {
        super(packOutput, EsotericismTinker.MODID);
    }

    RandomMaterial tier1Material = RandomMaterial.random().tier(1).build();
    DefaultMaterialsModule defaultTwoParts = DefaultMaterialsModule.builder().material(tier1Material, tier1Material).build();

    @Override
    protected void addToolDefinitions() {
        define(EsotericismTinkerToolDefinitions.RitualBlade)
                // parts
                .module(PartStatsModule.parts()
                                       .part(smallBlade)
                                       .part(toolHandle).build())
                .module(defaultTwoParts)
                // stats
                .module(new SetStatsModule(StatsNBT.builder()
                                                   .set(ToolStats.ATTACK_DAMAGE, 4f)
                                                   .set(ToolStats.ATTACK_SPEED, 1.0f)
                                                   .set(ToolStats.BLOCK_AMOUNT, 10)
                                                   .set(ToolStats.USE_ITEM_SPEED, 1.0f).build()))
                .module(new MultiplyStatsModule(MultiplierNBT.builder()
                                                             .set(ToolStats.ATTACK_DAMAGE, 0.75f)
                                                             .set(ToolStats.MINING_SPEED, 0.75f)
                                                             .set(ToolStats.DURABILITY, 0.75f).build()))
                .smallToolStartingSlots()
                // traits
                .module(ToolTraitsModule.builder()
                                        .trait(TinkerModifiers.silky, 1)
                                        .trait(ModifierIds.spilling)
                                        .trait(EsotericismTinkerModifiers.self_sacrifice)
                                        .trait(TinkerModifiers.melting)
                                        .trait(TinkerModifiers.silkyShears).build())
                // behavior
                .module(ToolActionsModule.of(ToolActions.SWORD_DIG, ToolActions.HOE_DIG))
                .module(IsEffectiveModule.tag(TinkerTags.Blocks.MINABLE_WITH_DAGGER))
                .module(MiningSpeedModifierModule.blocks(7.5f, Blocks.COBWEB))
                // faster tool name logic
                .module(FixedMaterialToolName.FIRST);
    }

    @Override
    public @NotNull String getName() {
        return "EsotericismTinker Tool Definition Data Generator";
    }
}
