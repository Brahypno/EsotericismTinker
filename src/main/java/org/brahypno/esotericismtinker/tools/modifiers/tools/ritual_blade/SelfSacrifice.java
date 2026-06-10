package org.brahypno.esotericismtinker.tools.modifiers.tools.ritual_blade;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import org.brahypno.esotericismtinker.fluids.EsotericismTinkerFluids;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.modifiers.ability.interaction.BlockingModifier;

import javax.annotation.Nullable;

import static slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper.TANK_HELPER;
import static slimeknights.tconstruct.library.tools.helper.ToolAttackUtil.isAttackable;

public class SelfSacrifice extends NoLevelsModifier implements GeneralInteractionModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder builder) {
        builder.addHook(this, ModifierHooks.GENERAL_INTERACT);
        super.registerHooks(builder);
        builder.addModule(ToolTankHelper.TANK_HANDLER);
        builder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(FluidType.BUCKET_VOLUME));
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return BlockingModifier.blockWhileCharging(tool, UseAnim.BOW);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (isAttackable(player, player)){
            float damageDealt = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
            meltTarget(tool, modifier, player, damageDealt);
            if (!player.level().isClientSide){
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int) (20 * damageDealt * 4 * modifier.getLevel()), modifier.getLevel() + 1));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, (int) (20 * damageDealt * 4 * modifier.getLevel()), modifier.getLevel() + 1));
                player.addEffect(
                        new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) (20 * damageDealt * 2 * modifier.getLevel()), modifier.getLevel()));
                player.hurt(player.level().damageSources().playerAttack(player), damageDealt);
            }
        }
        return InteractionResult.PASS;
    }

    private void meltTarget(IToolStackView tool, ModifierEntry modifier, @Nullable LivingEntity target, float damageDealt) {
        // must have done damage, and must be fully charged
        if (damageDealt > 0){
            if (target != null){
                FluidStack output = new FluidStack(EsotericismTinkerFluids.blood_soul.get(), FluidValues.GLASS_PANE / 5);
                int damagePerOutput;
                damagePerOutput = 2;
                FluidStack fluid = TANK_HELPER.getFluid(tool);
                if (fluid.isEmpty() || fluid.isFluidEqual(output)){
                    // recipe amount determines how much we get per hit, up to twice the recipe damage
                    int fluidAmount;
                    if (damageDealt < damagePerOutput * 2){
                        fluidAmount = (int) (output.getAmount() * damageDealt / damagePerOutput);
                    }else {
                        fluidAmount = output.getAmount() * 2;
                    }

                    // fluid must match that which is stored in the tank
                    if (fluid.isEmpty()){
                        output.setAmount(fluidAmount);
                        fluid = output;
                    }else {
                        fluid.grow(fluidAmount);
                    }
                    TANK_HELPER.setFluid(tool, fluid);
                }
            }
        }
    }
}
