package org.brahypno.esotericismtinker.tools;

import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.EsotericismTinkerModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.armor.FlightModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.armor.RepriseProtectionModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.armor.ResonanceArmorModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.build.AllSlotModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.combat.*;
import org.brahypno.esotericismtinker.library.modifiers.modules.harvest.BlockLootMultiplierModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.harvest.EntityLootMultiplierModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.weapon.SelfDestroyModule;
import org.brahypno.esotericismtinker.library.modifiers.modules.weapon.SwappableCircleWeaponAttack;
import org.brahypno.esotericismtinker.tools.modifiers.tools.ritual_blade.SelfSacrifice;
import org.brahypno.esotericismtinker.tools.traits.combat.ForceHurt;
import org.brahypno.esotericismtinker.tools.traits.combat.ForceRemove;
import org.brahypno.esotericismtinker.tools.traits.harvest.ForceDrop;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public final class EsotericismTinkerModifiers extends EsotericismTinkerModule {
    public static ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(EsotericismTinker.MODID);
    public static final StaticModifier<SelfSacrifice> self_sacrifice = MODIFIERS.register("self_sacrifice", SelfSacrifice::new);
    public static final StaticModifier<ForceHurt> force_hurt = MODIFIERS.register("force_hurt", ForceHurt::new);
    public static final StaticModifier<ForceDrop> force_drop = MODIFIERS.register("force_drop", ForceDrop::new);
    public static final StaticModifier<ForceRemove> force_remove = MODIFIERS.register("force_remove", ForceRemove::new);

    @SuppressWarnings({"removal"})
    public EsotericismTinkerModifiers() {
        MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    @SubscribeEvent
    void registerSerializers(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER){
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("swappable_circle_weapon_attack"), SwappableCircleWeaponAttack.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("effects_remover"), MobEffectsRemoverModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("self_mob_effect"), SelfMobEffectModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("block_loot_multiplier"), BlockLootMultiplierModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("entity_loot_multiplier"), EntityLootMultiplierModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("armor_resonance_module"), ResonanceArmorModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("reprise_protection_module"), RepriseProtectionModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("self_destory_module"), SelfDestroyModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("absorption_gain_module"), AbsorptionGainModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("explosion_like_projectile_damage_module"),
                                           ExplosionLikeProjectileDamageModule.LOADER);

            ModifierModule.LOADER.register(EsotericismTinker.getLocation("projectile_cloude_on_hit_module"), ProjectileCloudOnHitModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("projectile_spawn_module"), ProjectileSpawnModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("all_slot_module"), AllSlotModule.LOADER);
            ModifierModule.LOADER.register(EsotericismTinker.getLocation("my_creative_flight_module"), FlightModule.LOADER);

        }
    }
}
