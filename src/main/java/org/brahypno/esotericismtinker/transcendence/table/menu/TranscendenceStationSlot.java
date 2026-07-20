package org.brahypno.esotericismtinker.transcendence.table.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;

import java.util.function.Predicate;

final class TranscendenceStationSlot extends Slot {
    private final LazyResultContainer result;
    private final Predicate<ItemStack> filter;

    TranscendenceStationSlot(Container container, LazyResultContainer result, int index, int x, int y, Predicate<ItemStack> filter) {
        super(container, index, x, y);
        this.result = result;
        this.filter = filter;
    }

    @Override public boolean mayPlace(ItemStack stack) { return filter.test(stack); }
    @Override public void setChanged() { result.clearContent(); super.setChanged(); }
}
