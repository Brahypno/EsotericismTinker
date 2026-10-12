package org.brahypno.esotericismtinker.library.modifiers.modules.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.math.ModifierFormula;
import slimeknights.tconstruct.library.json.variable.VariableFormula;
import slimeknights.tconstruct.library.json.variable.power.PowerFormula;
import slimeknights.tconstruct.library.json.variable.power.PowerVariable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.List;

/** Adjusts initial projectile speed without changing its direction, including secondary shots. */
public record ProjectileVelocityModule(PowerFormula formula, ModifierCondition<IToolStackView> condition)
        implements ModifierModule, ProjectileLaunchModifierHook, ModifierCondition.ConditionalModule<IToolStackView> {
    public static final RecordLoadable<ProjectileVelocityModule> LOADER = RecordLoadable.create(
            PowerFormula.LOADER.directField(ProjectileVelocityModule::formula),
            ModifierCondition.TOOL_FIELD,
            ProjectileVelocityModule::new);
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.defaultHooks(ModifierHooks.PROJECTILE_LAUNCH);

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, Projectile projectile,
                                   @Nullable AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
        if (!condition.matches(tool, modifier)){
            return;
        }
        Vec3 motion = projectile.getDeltaMovement();
        double speed = motion.length();
        if (speed > 0){
            float result = formula.apply(tool.getModifiers(), persistentData, modifier, projectile, null, shooter, null, 1, (float) speed);
            double scale = formula.percent() ? 1 + result : result / speed;
            projectile.setDeltaMovement(motion.scale(scale));
        }
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<ProjectileVelocityModule> getLoader() {
        return LOADER;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The formula value is the current speed; percent formulas return a proportional adjustment. */
    public static class Builder extends VariableFormula.Builder<Builder, ProjectileVelocityModule, PowerVariable> {
        private Builder() {
            super(PowerFormula.VARIABLES);
        }

        @Override
        protected ProjectileVelocityModule build(ModifierFormula formula) {
            return new ProjectileVelocityModule(new PowerFormula(formula, variables, percent), condition);
        }
    }
}
