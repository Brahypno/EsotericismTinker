package org.brahypno.esotericismtinker.tools.data;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerTools;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.data.tinkering.AbstractStationSlotLayoutProvider;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.tools.TinkerToolParts;

import java.util.Objects;

public class EsotericismTinkerStationLayout extends AbstractStationSlotLayoutProvider {
    public EsotericismTinkerStationLayout(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void addLayouts() {
        defineModifiable(EsotericismTinkerTools.ritual_blade)
                .sortIndex(SORT_WEAPON)
                .addInputItem(TinkerToolParts.smallBlade, 39, 35)
                .addInputItem(TinkerToolParts.toolHandle, 21, 53)
                .addInputItem(new Pattern("tconstruct:pattern"), Blocks.GLASS, 53, 21)
                .build();

    }

    protected StationSlotLayout.Builder defineModifiable(IModifiableDisplay item, ICondition... conditions) {
        return this.define(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item.asItem())), conditions).translationKey(item.asItem().getDescriptionId())
                   .icon(item.getRenderTool());
    }

    @Override
    public @NotNull String getName() {
        return "EsotericismTinker Station Slot Layouts";
    }

}
