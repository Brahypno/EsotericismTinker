package org.brahypno.esotericismtinker.library.modifiers.modules.armor;

import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule.SlotInCharge;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;


/**
 * Module that grants flight while an active armor slot has this modifier.
 */
public record FlightModule(ModifierCondition<IToolContext> condition)
        implements ModifierModule, InventoryTickModifierHook, EquipmentChangeModifierHook, ConditionalModule<IToolContext> {
    private static final TinkerDataKey<SlotInCharge> SLOT_KEY = TinkerDataCapability.TinkerDataKey.of(EsotericismTinker.getLocation("flight"));
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<FlightModule>defaultHooks(
            ModifierHooks.INVENTORY_TICK,
            ModifierHooks.EQUIPMENT_CHANGE
    );

    public static final RecordLoadable<FlightModule> LOADER = RecordLoadable.create(
            ModifierCondition.CONTEXT_FIELD,
            FlightModule::new
    );

    /**
     * @apiNote Internal constructor, use {@link #INSTANCE}
     */
    @Internal
    public FlightModule {}

    public FlightModule() {this(ModifierCondition.ANY_CONTEXT);}

    public static final FlightModule INSTANCE = new FlightModule(ModifierCondition.ANY_CONTEXT);

    @Override
    public RecordLoadable<FlightModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public void addModules(ModuleHookMap.Builder builder) {
        builder.addModule(new SlotInChargeModule(SLOT_KEY));
    }

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull Level world, @NotNull LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, @NotNull ItemStack stack) {
        if (!isCorrectSlot || world.isClientSide || !condition.matches(tool, modifier)){
            return;
        }
        enableFlight(holder);
    }

    @Override
    public void onEquip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, EquipmentChangeContext context) {
        if (condition.matches(tool, modifier)){
            enableFlight(context.getEntity());
        }
    }

    @Override
    public void onUnequip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, EquipmentChangeContext context) {
        LivingEntity entity = context.getEntity();
        if (!(entity instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()){
            return;
        }

        // If another active slot still has this modifier, keep flight enabled.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (SlotInChargeModule.getLevel(context.getTinkerData(), SLOT_KEY, slot) > 0){
                return;
            }
        }
        disableFlight(player);
    }

    private static void enableFlight(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()){
            return;
        }

        if (!player.getAbilities().mayfly){
            player.getAbilities().mayfly = true;
            syncAbilities(player);
        }
    }

    private static void disableFlight(ServerPlayer player) {
        if (player.getAbilities().mayfly){
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            syncAbilities(player);
        }
    }

    private static void syncAbilities(ServerPlayer player) {
        player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
    }
}