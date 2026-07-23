package org.brahypno.esotericismtinker.smeltery.block.entity.module;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.brahypno.esotericismtinker.Config;
import org.brahypno.esotericismtinker.smeltery.recipe.entitymelting.ByproductEntityMeltingRecipe;
import org.brahypno.esotericismtinker.smeltery.recipe.entitymelting.ByproductEntityMeltingRecipeCache;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.common.TinkerTags.EntityTypes;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipeCache;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/** TConstruct entity melting module with a custom byproduct-recipe-first selection step. */
public class ByproductEntityMeltingModule {
  private final MantleBlockEntity parent;
  private final IFluidHandler tank;
  private final BooleanSupplier canMeltEntities;
  private final Function<ItemStack, ItemStack> insertFunction;
  private final Supplier<AABB> bounds;
  @Nullable private EntityMeltingRecipe lastRecipe;
  @Nullable private DamageSource smelteryMagic;
  @Nullable private DamageSource smelteryHeat;

  public ByproductEntityMeltingModule(MantleBlockEntity parent, IFluidHandler tank, BooleanSupplier canMeltEntities,
                                      Function<ItemStack, ItemStack> insertFunction, Supplier<AABB> bounds) {
    this.parent = parent;
    this.tank = tank;
    this.canMeltEntities = canMeltEntities;
    this.insertFunction = insertFunction;
    this.bounds = bounds;
  }
  private Level getLevel() { return Objects.requireNonNull(parent.getLevel(), "Parent tile entity has null world"); }
  private DamageSource smelteryMagic() {
    if (smelteryMagic == null) smelteryMagic = TinkerDamageTypes.source(getLevel().registryAccess(), TinkerDamageTypes.SMELTERY_MAGIC);
    return smelteryMagic;
  }
  private DamageSource smelteryHeat() {
    if (smelteryHeat == null) smelteryHeat = TinkerDamageTypes.source(getLevel().registryAccess(), TinkerDamageTypes.SMELTERY_HEAT);
    return smelteryHeat;
  }
  @Nullable private EntityMeltingRecipe findRecipe(EntityType<?> type) {
    if (lastRecipe != null && lastRecipe.matches(type)) return lastRecipe;
    EntityMeltingRecipe recipe = EntityMeltingRecipeCache.findRecipe(getLevel().getRecipeManager(), type);
    if (recipe != null) lastRecipe = recipe;
    return recipe;
  }
  public static FluidStack getDefaultFluid() { return new FluidStack(TinkerFluids.liquidSoul.get(), FluidValues.GLASS_PANE / 5); }
  private boolean canMeltEntity(LivingEntity entity) {
    return !entity.isInvulnerableTo(entity.fireImmune() ? smelteryMagic() : smelteryHeat())
        && !(entity instanceof Player player && player.getAbilities().invulnerable)
        && !entity.hasEffect(MobEffects.FIRE_RESISTANCE);
  }
  public boolean interactWithEntities() {
    AABB boundingBox = bounds.get();
    if (boundingBox == null) return false;
    Boolean canMelt = null;
    boolean melted = false;
    for (Entity entity : getLevel().getEntitiesOfClass(Entity.class, boundingBox)) {
      if (!entity.isAlive()) continue;
      EntityType<?> type = entity.getType();
      if (entity instanceof ItemEntity itemEntity) {
        ItemStack stack = insertFunction.apply(itemEntity.getItem());
        if (stack.isEmpty()) entity.discard(); else itemEntity.setItem(stack);
      } else if (canMelt != Boolean.FALSE && !type.is(EntityTypes.MELTING_HIDE)
          && entity instanceof LivingEntity living && canMeltEntity(living)) {
        if (canMelt == null) canMelt = canMeltEntities.getAsBoolean();
        if (canMelt) {
          DamageSource source = entity.fireImmune() ? smelteryMagic() : smelteryHeat();
          ByproductEntityMeltingRecipe custom = ByproductEntityMeltingRecipeCache.findRecipe(getLevel().getRecipeManager(), type);
          if (custom != null) {
            if (entity.hurt(source, custom.getDamage())) {
              tank.fill(custom.getOutput(living), IFluidHandler.FluidAction.EXECUTE);
              custom.getByproducts(living).forEach(stack -> tank.fill(stack, IFluidHandler.FluidAction.EXECUTE));
              melted = true;
            }
          } else {
            EntityMeltingRecipe recipe = findRecipe(type);
            FluidStack fluid = recipe != null ? recipe.getOutput(living) : getDefaultFluid();
            fluid = fluid.copy();
            long multipliedAmount = (long) fluid.getAmount() * Config.BYPRODUCT_ENTITY_MELTING_MULTIPLIER.get();
            fluid.setAmount((int) Math.min(Integer.MAX_VALUE, multipliedAmount));
            int damage = recipe != null ? recipe.getDamage() : 2;
            if (entity.hurt(source, damage)) {
              tank.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
              melted = true;
            }
          }
        }
      }
    }
    return melted;
  }
}
