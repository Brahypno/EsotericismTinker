package org.brahypno.esotericismtinker.utils.CompactUtils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.brahypno.esotericismtinker.EsotericismTinker.configCompactDisabled;

public class CuriosCompact {
    private static Predicate<ItemStack> preferredModifiable = stack -> stack.is(TinkerTags.Items.MODIFIABLE) && stack.is(EsotericismTinkerTagKeys.Items.CURIOS);

    private CuriosCompact() {}

    public static void registerPreferredModifiable(Predicate<ItemStack> predicate) {
        Predicate<ItemStack> previous = preferredModifiable;
        preferredModifiable = stack -> predicate.test(stack) || previous.test(stack);
    }

    public static ItemStack findFirstItemWithModifier(Player player, ModifierId id) {
        if (!ModList.get().isLoaded("curios"))
            return ItemStack.EMPTY;
        return doFindModifierItem(player, id).orElse(ItemStack.EMPTY);
    }

    public static int getCurioModifierNumber(Player player, ModifierId id) {
        if (!ModList.get().isLoaded("curios"))
            return 0;
        return doFindModifierNum(player, id);
    }

    public static List<ItemStack> getCurioStacks(Player player) {
        if (!ModList.get().isLoaded("curios"))
            return List.of();
        return doFindListItemStack(player);
    }

    public static void damageAllCurios(LivingEntity target, int amount, Predicate<ItemStack> filter) {
        if (!ModList.get().isLoaded("curios") || configCompactDisabled("curios") || target.level().isClientSide || amount <= 0)
            return;
        doDamageAllCurios(target, amount, filter);
    }

    public static ItemStack findPreferredModifiable(Player player) {
        if (!ModList.get().isLoaded("curios"))
            return ItemStack.EMPTY;
        return doFindPreferredModifiable(player).orElse(ItemStack.EMPTY);
    }

    public static ItemStack findPreferredGlove(Player player) {
        return findPreferredModifiable(player);
    }

    private static int doFindModifierNum(Player player, ModifierId id) {
        return CuriosApi.getCuriosInventory(player).map(h -> {
            int result = 0;
            for (ICurioStacksHandler handler : h.getCurios().values()) {
                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.is(TinkerTags.Items.MODIFIABLE))
                        result += ModifierUtil.getModifierLevel(stack, id);
                }
            }
            return result;
        }).orElse(0);
    }

    private static List<ItemStack> doFindListItemStack(Player player) {
        return CuriosApi.getCuriosInventory(player).map(h -> {
            List<ItemStack> result = new ArrayList<>();
            for (ICurioStacksHandler handler : h.getCurios().values()) {
                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.is(TinkerTags.Items.MODIFIABLE))
                        result.add(stack);
                }
            }
            return result;
        }).orElse(List.of());
    }

    private static void doDamageAllCurios(LivingEntity target, int amount, Predicate<ItemStack> filter) {
        CuriosApi.getCuriosInventory(target).ifPresent(inv -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : inv.getCurios().entrySet()) {
                IDynamicStackHandler stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.isDamageableItem() && (filter == null || filter.test(stack))){
                        stack.hurtAndBreak(amount, target, entity -> {});
                    }
                }
            }
        });
    }

    private static Optional<ItemStack> doFindModifierItem(Player player, ModifierId id) {
        LazyOptional<ICuriosItemHandler> opt = CuriosApi.getCuriosInventory(player);
        return Optional.of(opt.map(h -> {
            for (ICurioStacksHandler handler : h.getCurios().values()) {
                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.is(TinkerTags.Items.MODIFIABLE) && 0 < ModifierUtil.getModifierLevel(stack, id))
                        return stack;
                }
            }
            return ItemStack.EMPTY;
        }).orElse(ItemStack.EMPTY));
    }

    private static Optional<ItemStack> doFindPreferredModifiable(Player player) {
        LazyOptional<ICuriosItemHandler> opt = CuriosApi.getCuriosInventory(player);
        return Optional.of(opt.map(h -> {
            ItemStack fromHands = getFirstFromSlot(h, "hands");
            if (!fromHands.isEmpty())
                return fromHands;
            ItemStack fromHand = getFirstFromSlot(h, "hand");
            if (!fromHand.isEmpty())
                return fromHand;
            for (String id : h.getCurios().keySet()) {
                ItemStack stack = getFirstFromSlot(h, id);
                if (!stack.isEmpty())
                    return stack;
            }
            return ItemStack.EMPTY;
        }).orElse(ItemStack.EMPTY));
    }

    private static ItemStack getFirstFromSlot(ICuriosItemHandler handler, String slotId) {
        Optional<ICurioStacksHandler> stacksHandler = handler.getStacksHandler(slotId);
        if (stacksHandler.isEmpty())
            return ItemStack.EMPTY;
        ICurioStacksHandler curioStacksHandler = stacksHandler.get();
        IDynamicStackHandler stacks = curioStacksHandler.getStacks();
        for (int i = 0; i < curioStacksHandler.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (preferredModifiable.test(stack))
                return stack;
        }
        return ItemStack.EMPTY;
    }
}
