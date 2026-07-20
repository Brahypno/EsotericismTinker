package org.brahypno.esotericismtinker.transcendence.table.block;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.tinkerstation.IMutableTinkerStationContainer;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

final class TranscendenceStationContainer implements IMutableTinkerStationContainer {
    private final TranscendenceAnvilBlockEntity station;
    private LazyToolStack tool;
    private Player player;

    TranscendenceStationContainer(TranscendenceAnvilBlockEntity station) { this.station = station; }
    void setPlayer(@Nullable Player player) { this.player = player; }
    void refresh(int slot) { if (slot == 0) tool = null; }
    LazyToolStack getTool() { if (tool == null) tool = LazyToolStack.from(getTinkerableStack()); return tool; }
    @Override public ItemStack getTinkerableStack() { return station.getItem(0); }
    @Override public ToolStack getTinkerable() { return getTool().getTool(); }
    @Override public ItemStack getInput(int index) { return index >= 0 && index < getInputCount() ? station.getItem(index + 1) : ItemStack.EMPTY; }
    @Override public int getInputCount() { return TranscendenceAnvilBlockEntity.INPUT_COUNT; }
    @Nullable @Override public MaterialRecipe getInputMaterial(int index) { MaterialRecipe recipe = MaterialRecipeCache.findRecipe(getInput(index)); return recipe == MaterialRecipe.EMPTY ? null : recipe; }
    @Override public void setInput(int index, ItemStack stack) { if (index >= 0 && index < getInputCount()) station.setItem(index + 1, stack); }
    @Override public void giveItem(ItemStack stack) { if (player != null) player.getInventory().placeItemBackInInventory(stack); }
}
