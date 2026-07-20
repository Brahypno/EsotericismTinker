package org.brahypno.esotericismtinker.transcendence.table.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.transcendence.table.EsotericismTinkerTranscendenceTable;
import org.brahypno.esotericismtinker.transcendence.table.block.TranscendenceAnvilBlockEntity;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.menu.TabbedContainerMenu;
import slimeknights.tconstruct.tables.menu.slot.ArmorSlot;
import slimeknights.tconstruct.tables.menu.slot.LazyResultSlot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TranscendenceAnvilMenu extends TabbedContainerMenu<TranscendenceAnvilBlockEntity> {
    public static final int TOOL_SLOT = 0;
    public static final int FIRST_MATERIAL_SLOT = 1;
    public static final int MATERIAL_SLOT_COUNT = 5;
    public static final int RESULT_SLOT = 6;
    public static final int OFFHAND_SLOT = 11;

    @Nullable
    private final TranscendenceAnvilBlockEntity station;
    @Nullable
    private final Slot resultSlot;
    private final StationSlotLayout stationLayout;

    private final Map<String, Integer> syncedInitialReception = new LinkedHashMap<>();
    private final Map<String, Integer> syncedPendingReception = new LinkedHashMap<>();
    private int syncedPendingInvestitureIndex = -1;

    public TranscendenceAnvilMenu(
            int id,
            Inventory inventory,
            @Nullable TranscendenceAnvilBlockEntity station
    ) {
        super(
                EsotericismTinkerTranscendenceTable.transcendenceAnvilMenu.get(),
                id,
                inventory,
                station
        );
        this.station = station;
        this.stationLayout =
                StationSlotLayoutLoader.getInstance().get(TinkerTables.tinkersAnvil.getId());

        if (station != null) {
            LayoutSlot toolLayout = stationLayout.getSlot(TOOL_SLOT);
            addSlot(new TranscendenceStationSlot(
                    station,
                    station.getCraftingResult(),
                    TOOL_SLOT,
                    toolLayout.getX(),
                    toolLayout.getY(),
                    stack -> stack.getItem() instanceof IModifiable
            ));

            for (int index = FIRST_MATERIAL_SLOT;
                 index < FIRST_MATERIAL_SLOT + MATERIAL_SLOT_COUNT;
                 index++) {
                LayoutSlot inputLayout = stationLayout.getSlot(index);
                addSlot(new TranscendenceStationSlot(
                        station,
                        station.getCraftingResult(),
                        index,
                        inputLayout.getX(),
                        inputLayout.getY(),
                        stack -> true
                ));
            }

            resultSlot = addSlot(new LazyResultSlot(station.getCraftingResult(), 114, 38));
        } else {
            resultSlot = null;
        }

        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            addSlot(new ArmorSlot(inventory, type.getSlot(), 152, 20 + type.ordinal() * 18));
        }
        addSlot(new Slot(inventory, 40, 132, 74)
                .setBackground(
                        InventoryMenu.BLOCK_ATLAS,
                        InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD
                ));
        addInventorySlots();

        List<String> slotTypes = SlotType.getAllSlotTypes()
                .stream()
                .map(SlotType::getName)
                .sorted()
                .toList();

        for (String slotType : slotTypes) {
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return station == null
                            ? syncedInitialReception.getOrDefault(slotType, 0)
                            : station.getInitialReception(slotType);
                }

                @Override
                public void set(int value) {
                    syncedInitialReception.put(slotType, value);
                }
            });
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return station == null
                            ? syncedPendingReception.getOrDefault(slotType, 0)
                            : station.getPendingReception(slotType);
                }

                @Override
                public void set(int value) {
                    syncedPendingReception.put(slotType, value);
                }
            });
        }

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return station == null
                        ? syncedPendingInvestitureIndex
                        : station.getPendingInvestitureIndex();
            }

            @Override
            public void set(int value) {
                syncedPendingInvestitureIndex = value;
            }
        });

        if (station != null && inventory.player instanceof ServerPlayer serverPlayer) {
            station.syncRecipe(serverPlayer);
        }
    }

    public TranscendenceAnvilMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, getTileEntityFromBuf(buffer, TranscendenceAnvilBlockEntity.class));
    }

    @Nullable
    public TranscendenceAnvilBlockEntity getStation() {
        return station;
    }

    public StationSlotLayout getStationLayout() {
        return stationLayout;
    }

    public int getInitialReception(String slotType) {
        return syncedInitialReception.getOrDefault(
                slotType,
                station == null ? 0 : station.getInitialReception(slotType)
        );
    }

    public boolean hasPendingGuiChanges() {
        for (SlotType type : SlotType.getAllSlotTypes()) {
            String name = type.getName();
            if (getPendingReception(name) != getInitialReception(name)) {
                return true;
            }
        }
        return syncedPendingInvestitureIndex >= 0;
    }

    public int getPendingReception(String slotType) {
        return syncedPendingReception.getOrDefault(
                slotType,
                station == null ? 0 : station.getPendingReception(slotType)
        );
    }

    @Nullable
    public LazyToolStack getResultTool() {
        return resultSlot == null || !resultSlot.hasItem()
                ? null
                : LazyToolStack.from(resultSlot.getItem());
    }

    @Nullable
    public LazyToolStack getInputTool() {
        return slots.isEmpty() || !slots.get(TOOL_SLOT).hasItem()
                ? null
                : LazyToolStack.from(slots.get(TOOL_SLOT).getItem());
    }

    @Nullable
    public LazyToolStack getOffhandTool() {
        return slots.size() <= OFFHAND_SLOT || !slots.get(OFFHAND_SLOT).hasItem()
                ? null
                : LazyToolStack.from(slots.get(OFFHAND_SLOT).getItem());
    }

    @Override
    protected int getInventoryYOffset() {
        return 102;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot != resultSlot && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);

        if (slot == resultSlot) {
            if (station != null && slot.hasItem()) {
                ItemStack original = slot.getItem().copy();
                ItemStack result = original.copy();

                station.onCraft(player, result, result.getCount());

                boolean nothingDone = true;
                if (!subContainers.isEmpty()) {
                    nothingDone = refillAnyContainer(result, subContainers);
                }
                nothingDone &= moveToPlayerInventory(result);
                if (!subContainers.isEmpty()) {
                    nothingDone &= moveToAnyContainer(result, subContainers);
                }

                if (!nothingDone) {
                    if (!result.isEmpty()) {
                        player.drop(result, false);
                    }
                    station.getCraftingResult().clearContent();
                    return original;
                }
            }
            return ItemStack.EMPTY;
        }

        if (index >= playerInventoryStart
                && slot.hasItem()
                && slot.getItem().getItem() instanceof IModifiable) {
            ItemStack original = slot.getItem().copy();
            ItemStack moving = slot.getItem().copy();
            if (moveItemStackTo(moving, TOOL_SLOT, TOOL_SLOT + 1, false)) {
                return notifySlotAfterTransfer(player, moving, original, slot);
            }
            return ItemStack.EMPTY;
        }

        return super.quickMoveStack(player, index);
    }
}
