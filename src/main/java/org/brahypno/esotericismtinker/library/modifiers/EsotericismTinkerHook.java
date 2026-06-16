package org.brahypno.esotericismtinker.library.modifiers;

import net.minecraft.resources.ResourceLocation;
import org.brahypno.esotericismtinker.library.modifiers.hook.LeftClickHook;
import org.brahypno.esotericismtinker.library.modifiers.hook.LivingHealHealHook;
import org.brahypno.esotericismtinker.library.modifiers.hook.ProjectileHurtHook;
import org.brahypno.esotericismtinker.library.modifiers.hook.RightClickHook;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;

import static org.brahypno.esotericismtinker.EsotericismTinker.MODID;

public class EsotericismTinkerHook {
    public static final ModuleHook<LeftClickHook> LEFT_CLICK =
            ModifierHooks.register(new ResourceLocation(MODID, "left_click"), LeftClickHook.class, LeftClickHook.AllMerger::new, new LeftClickHook() {});
    public static final ModuleHook<RightClickHook> RIGHT_CLICK =
            ModifierHooks.register(new ResourceLocation(MODID, "right_click"), RightClickHook.class, RightClickHook.AllMerger::new, new RightClickHook() {});
    public static final ModuleHook<ProjectileHurtHook> PROJECTILE_HURT =
            ModifierHooks.register(new ResourceLocation(MODID, "projectile_hurt"), ProjectileHurtHook.class, ProjectileHurtHook.AllMerger::new,
                                   new ProjectileHurtHook() {});
    public static final ModuleHook<LivingHealHealHook> HEAL =
            ModifierHooks.register(new ResourceLocation(MODID, "heal"), LivingHealHealHook.class, LivingHealHealHook.AllMerger::new,
                                   new LivingHealHealHook() {});
}
