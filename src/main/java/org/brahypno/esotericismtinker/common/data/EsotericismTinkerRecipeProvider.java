package org.brahypno.esotericismtinker.common.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.common.crafting.DifferenceIngredient;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.common.EsotericismTinkerCommon;
import org.brahypno.esotericismtinker.common.EsotericismTinkerTagKeys;
import org.brahypno.esotericismtinker.fluids.EsotericismTinkerFluids;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.brahypno.esotericismtinker.tools.data.ToolsRecipesProvider;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipeBuilder;
import slimeknights.mantle.recipe.data.ICommonRecipeHelper;
import slimeknights.mantle.recipe.data.IRecipeHelper;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.data.recipe.ISmelteryRecipeHelper;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.casting.ItemCastingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.casting.container.ContainerFillingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.ingredient.NoContainerIngredient;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeBuilder;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.Objects;
import java.util.function.Consumer;

public class EsotericismTinkerRecipeProvider extends RecipeProvider implements IConditionBuilder, ISmelteryRecipeHelper, IRecipeHelper, ICommonRecipeHelper {
    private static final String CASTING_FOLDER = "smeltery/casting/";
    private static final String MELTING_FOLDER = "smeltery/melting/";

    public EsotericismTinkerRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> consumer) {
        addTransmuteRecipes(consumer);
        new ToolsRecipesProvider().buildRecipes(consumer);
        addCastingRecipes(consumer);
    }

    private void addCastingRecipes(Consumer<FinishedRecipe> consumer) {
        ItemCastingRecipeBuilder.tableRecipe(EsotericismTinkerCommon.hypnagogic_transmute.get())
                                .setFluidAndTime(EsotericismTinkerFluids.blood_soul, FluidValues.GEM)
                                .setCast(Items.BOOK, true)
                                .save(consumer, prefix(EsotericismTinkerCommon.hypnagogic_transmute, "common/"));
    }


    private void addTransmuteRecipes(Consumer<FinishedRecipe> consumer) {
        String folder = "smeltery/ashen/";
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.enderMortar, 2)
                              .requires(TinkerCommons.slimeball.get(SlimeType.ENDER))
                              .requires(Items.POPPED_CHORUS_FRUIT)
                              .requires(Tags.Items.GRAVEL)
                              .unlockedBy("has_item", has(TinkerCommons.slimeball.get(SlimeType.ENDER)))
                              .save(consumer, prefix(EsotericismTinkerSmeltery.enderMortar, folder));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.enderMortar, 8)
                              .requires(TinkerWorld.congealedSlime.get(SlimeType.ENDER))
                              .requires(Items.POPPED_CHORUS_FRUIT).requires(Items.POPPED_CHORUS_FRUIT).requires(Items.POPPED_CHORUS_FRUIT)
                              .requires(Items.POPPED_CHORUS_FRUIT)
                              .requires(Tags.Items.GRAVEL).requires(Tags.Items.GRAVEL).requires(Tags.Items.GRAVEL).requires(Tags.Items.GRAVEL)
                              .unlockedBy("has_item", has(TinkerCommons.slimeball.get(SlimeType.ENDER)))
                              .save(consumer, wrap(EsotericismTinkerSmeltery.enderMortar, folder, "_multiple"));

        // scorched bricks from grout
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(EsotericismTinkerSmeltery.enderMortar), RecipeCategory.BUILDING_BLOCKS,
                                            EsotericismTinkerSmeltery.ashenBrick.get(), 0.3f,
                                            200)
                                  .unlockedBy("has_item", has(EsotericismTinkerSmeltery.enderMortar))
                                  .save(consumer, prefix(EsotericismTinkerSmeltery.ashenBrick, folder));
        Consumer<Consumer<FinishedRecipe>> fastMortar = c ->
                SimpleCookingRecipeBuilder.blasting(Ingredient.of(EsotericismTinkerSmeltery.enderMortar), RecipeCategory.BUILDING_BLOCKS,
                                                    EsotericismTinkerSmeltery.ashenBrick.get(),
                                                    0.3f, 100)
                                          .unlockedBy("has_item", has(EsotericismTinkerSmeltery.enderMortar)).save(c);
        // block from bricks
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.ashenBricks)
                           .define('b', EsotericismTinkerSmeltery.ashenBrick.get())
                           .pattern("bb")
                           .pattern("bb")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, wrap(EsotericismTinkerSmeltery.ashenBricks, folder, "_from_brick"));

        // ladder from bricks
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.ashenLadder, 4)
                           .define('b', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('B', EsotericismTinkerTagKeys.Items.ASHEN_BLOCKS)
                           .pattern("b b")
                           .pattern("b b")
                           .pattern("BBB")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, prefix(EsotericismTinkerSmeltery.ashenLadder, folder));

        // stone -> polished
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.polishedAshenStone, 4)
                           .define('b', EsotericismTinkerSmeltery.ashenStone)
                           .pattern("bb")
                           .pattern("bb")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenStone))
                           .save(consumer, wrap(EsotericismTinkerSmeltery.polishedAshenStone, folder, "_crafting"));
        // polished -> bricks
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.ashenBricks, 4)
                           .define('b', EsotericismTinkerSmeltery.polishedAshenStone)
                           .pattern("bb")
                           .pattern("bb")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.polishedAshenStone))
                           .save(consumer, wrap(EsotericismTinkerSmeltery.ashenBricks, folder, "_crafting"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenDrain)
                           .define('#', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('C', TinkerMaterials.knightmetal.getIngotTag())
                           .pattern("# #")
                           .pattern("C C")
                           .pattern("# #")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, location(folder + "drain"));
        ShapedRetexturedRecipeBuilder.fromShaped(
                                             ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenDrain)
                                                                .define('#', EsotericismTinkerTagKeys.Items.TRANSMUTE)
                                                                .define('C', TinkerMaterials.knightmetal.getIngotTag())
                                                                .pattern("C#C")
                                                                .unlockedBy("has_item", has(EsotericismTinkerTagKeys.Items.TRANSMUTE_BLOCKS)))
                                     .setSource(EsotericismTinkerTagKeys.Items.TRANSMUTE_BLOCKS)
                                     .build(consumer, location(folder + "drain_retextured"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenChute)
                           .define('#', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('C', TinkerMaterials.knightmetal.getIngotTag())
                           .pattern("#C#")
                           .pattern("   ")
                           .pattern("#C#")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, location(folder + "chute"));
        ShapedRetexturedRecipeBuilder.fromShaped(
                                             ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenChute)
                                                                .define('#', EsotericismTinkerTagKeys.Items.TRANSMUTE_BLOCKS)
                                                                .define('C', TinkerMaterials.knightmetal.getIngotTag())
                                                                .pattern("C")
                                                                .pattern("#")
                                                                .pattern("C")
                                                                .unlockedBy("has_item", has(EsotericismTinkerTagKeys.Items.TRANSMUTE_BLOCKS)))
                                     .setSource(EsotericismTinkerTagKeys.Items.TRANSMUTE_BLOCKS)
                                     .build(consumer, location(folder + "chute_retextured"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenDuct)
                           .define('#', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('C', Tags.Items.INGOTS_GOLD)
                           .pattern("# #")
                           .pattern("C C")
                           .pattern("# #")
                           .unlockedBy("has_item", has(Tags.Items.INGOTS_GOLD))
                           .save(consumer, location(folder + "duct"));
        ShapedRetexturedRecipeBuilder.fromShaped(
                                             ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenDuct)
                                                                .define('#', EsotericismTinkerTagKeys.Items.TRANSMUTE_BLOCKS)
                                                                .define('C', Tags.Items.INGOTS_GOLD)
                                                                .pattern("C#C")
                                                                .unlockedBy("has_item", has(EsotericismTinkerTagKeys.Items.TRANSMUTE_BLOCKS)))
                                     .setSource(EsotericismTinkerTagKeys.Items.TRANSMUTE_BLOCKS)
                                     .build(consumer, location(folder + "duct_retextured"));
        // stone -> road
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(EsotericismTinkerSmeltery.ashenStone), RecipeCategory.BUILDING_BLOCKS,
                                            EsotericismTinkerSmeltery.ashenRoad, 0.1f,
                                            200)
                                  .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenStone))
                                  .save(consumer, wrap(EsotericismTinkerSmeltery.ashenRoad, folder, "_smelting"));
        // brick slabs -> chiseled
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.chiseledAshenBricks)
                           .define('s', EsotericismTinkerSmeltery.ashenBricks.getSlab())
                           .pattern("s")
                           .pattern("s")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBricks.getSlab()))
                           .save(consumer, wrap(EsotericismTinkerSmeltery.chiseledAshenBricks, folder, "_crafting"));
        // stonecutting
        this.ashenStonecutter(consumer, EsotericismTinkerSmeltery.polishedAshenStone, folder);
        this.ashenStonecutter(consumer, EsotericismTinkerSmeltery.ashenBricks, folder);
        this.ashenStonecutter(consumer, EsotericismTinkerSmeltery.chiseledAshenBricks, folder);
        // tanks
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.FUEL_TANK))
                           .define('#', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('B', Tags.Items.GEMS_QUARTZ)
                           .pattern("###")
                           .pattern("#B#")
                           .pattern("###")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, location(folder + "fuel_tank"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.FUEL_GAUGE))
                           .define('#', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('B', Tags.Items.GEMS_QUARTZ)
                           .pattern("#B#")
                           .pattern("BBB")
                           .pattern("#B#")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, location(folder + "fuel_gauge"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.INGOT_TANK))
                           .define('#', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('B', Tags.Items.GEMS_QUARTZ)
                           .pattern("#B#")
                           .pattern("#B#")
                           .pattern("#B#")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, location(folder + "ingot_tank"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.INGOT_GAUGE))
                           .define('#', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('B', Tags.Items.GEMS_QUARTZ)
                           .pattern("B#B")
                           .pattern("#B#")
                           .pattern("B#B")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, location(folder + "ingot_gauge"));

        // stone
        ashenCasting(consumer, EsotericismTinkerSmeltery.ashenStone, Ingredient.of(Blocks.END_STONE), CASTING_FOLDER + "stone_from_ender");
        ashenCasting(consumer, EsotericismTinkerSmeltery.polishedAshenStone, Ingredient.of(Blocks.END_STONE_BRICKS), CASTING_FOLDER + "polished_from_ender");
        // glass
        MeltingRecipeBuilder.melting(Ingredient.of(EsotericismTinkerSmeltery.ashenGlass), EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK * 4, 2f)
                            .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM))
                            .save(consumer, location(MELTING_FOLDER + "glass"));
        MeltingRecipeBuilder.melting(Ingredient.of(EsotericismTinkerSmeltery.ashenSoulGlass), EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK * 4,
                                     2f)
                            .addByproduct(TinkerFluids.liquidSoul.result(FluidValues.GLASS_BLOCK))
                            .save(consumer, location(MELTING_FOLDER + "glass_soul"));
        MeltingRecipeBuilder.melting(Ingredient.of(EsotericismTinkerSmeltery.ashenTintedGlass), EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK * 4,
                                     2f)
                            .addByproduct(TinkerFluids.moltenGlass.result(FluidValues.GLASS_BLOCK))
                            .addByproduct(TinkerFluids.moltenAmethyst.result(FluidValues.GEM * 2))
                            .save(consumer, location(MELTING_FOLDER + "glass_tinted"));
        // panes
        MeltingRecipeBuilder.melting(Ingredient.of(EsotericismTinkerSmeltery.ashenGlassPane), EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK, 1.0f)
                            .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM_SHARD))
                            .save(consumer, location(MELTING_FOLDER + "pane"));
        MeltingRecipeBuilder.melting(Ingredient.of(EsotericismTinkerSmeltery.ashenSoulGlassPane), EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK,
                                     1.0f)
                            .addByproduct(TinkerFluids.liquidSoul.result(FluidValues.GLASS_PANE))
                            .save(consumer, location(MELTING_FOLDER + "pane_soul"));
        // controller
        ItemCastingRecipeBuilder.retexturedBasinRecipe(ItemOutput.fromItem(EsotericismTinkerSmeltery.transmuteController))
                                .setCast(EsotericismTinkerTagKeys.Items.TRANSMUTE_BLOCKS, true)
                                .setFluidAndTime(TinkerFluids.moltenKnightmetal, FluidValues.INGOT * 4)
                                .save(consumer, prefix(EsotericismTinkerSmeltery.transmuteController, CASTING_FOLDER));
        // stairs, slabs, and fences
        this.slabStairsCrafting(consumer, EsotericismTinkerSmeltery.ashenBricks, folder, true);
        this.slabStairsCrafting(consumer, EsotericismTinkerSmeltery.ashenRoad, folder, true);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.ashenBricks.getFence(), 6)
                           .define('B', EsotericismTinkerSmeltery.ashenBricks)
                           .define('b', EsotericismTinkerSmeltery.ashenBrick.get())
                           .pattern("BbB")
                           .pattern("BbB")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBricks))
                           .save(consumer, prefix(id(EsotericismTinkerSmeltery.ashenBricks.getFence()), folder));

        // casting
        String castingFolder = "smeltery/casting/ashen/";
        ItemCastingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenStone)
                                .setFluidAndTime(EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK_BLOCK)
                                .save(consumer, location(castingFolder + "stone_from_ashen"));
        cast(EsotericismTinkerFluids.molten_ender_ash.get(), EsotericismTinkerSmeltery.ashenBrick.get(), FluidValues.BRICK, consumer);


        MeltingRecipeBuilder.melting(Ingredient.of(EsotericismTinkerSmeltery.enderMortar), EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK * 2,
                                     1.5f)
                            .save(consumer, location(MELTING_FOLDER + "transmute/martar"));

        MeltingRecipeBuilder.melting(CompoundIngredient.of(Ingredient.of(EsotericismTinkerTagKeys.Items.ASHEN_BLOCKS),
                                                           Ingredient.of(EsotericismTinkerSmeltery.ashenLadder,
                                                                         EsotericismTinkerSmeltery.ashenBricks.getStairs(),
                                                                         EsotericismTinkerSmeltery.ashenRoad.getStairs())),
                                     EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK_BLOCK, 2.0f)
                            .save(consumer, location(MELTING_FOLDER + "block"));
        MeltingRecipeBuilder.melting(
                                    Ingredient.of(EsotericismTinkerSmeltery.ashenBricks.getSlab(), EsotericismTinkerSmeltery.ashenBricks.getSlab(),
                                                  EsotericismTinkerSmeltery.ashenRoad.getSlab()),
                                    EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK_BLOCK / 2, 1.5f)
                            .save(consumer, location(MELTING_FOLDER + "slab"));
        MeltingRecipeBuilder.melting(Ingredient.of(EsotericismTinkerSmeltery.ashenBrick.get()), EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK,
                                     1.0f)
                            .save(consumer, location(MELTING_FOLDER + "brick"));
        MeltingRecipeBuilder.melting(Ingredient.of(EsotericismTinkerSmeltery.ashenBricks.getFence()), EsotericismTinkerFluids.molten_ender_ash,
                                     FluidValues.BRICK * 3, 1.0f)
                            .save(consumer, location(MELTING_FOLDER + "fence"));

        MeltingRecipeBuilder.melting(Ingredient.of(EsotericismTinkerSmeltery.transmuteController), TinkerFluids.moltenKnightmetal, FluidValues.INGOT * 4, 3.5f)
                            .addByproduct(EsotericismTinkerFluids.molten_ender_ash.result(FluidValues.BRICK * 4))
                            .save(consumer, location(MELTING_FOLDER + "knightmetal/transmute_controller"));

        // scorched glass
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.ashenGlass)
                           .define('b', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('G', Tags.Items.GEMS_QUARTZ)
                           .pattern(" b ")
                           .pattern("bGb")
                           .pattern(" b ")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, prefix(EsotericismTinkerSmeltery.ashenGlass, folder));
        ItemCastingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenGlass)
                                .setFluidAndTime(TinkerFluids.moltenQuartz, FluidValues.GEM)
                                .setCast(EsotericismTinkerSmeltery.ashenBricks, true)
                                .save(consumer, location(castingFolder + "glass"));
        ItemCastingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenLamp)
                                .setFluidAndTime(EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK_BLOCK)
                                .setCast(Blocks.GLOWSTONE, true)
                                .save(consumer, location(castingFolder + "lamp"));
        ItemCastingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenSoulGlass)
                                .setFluidAndTime(EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK_BLOCK)
                                .setCast(TinkerCommons.soulGlass, true)
                                .save(consumer, location(castingFolder + "glass_soul"));
        ItemCastingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenTintedGlass)
                                .setFluidAndTime(EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK_BLOCK)
                                .setCast(Tags.Items.GLASS_TINTED, true)
                                .save(consumer, location(castingFolder + "glass_tinted"));
        // discount for casting panes
        ItemCastingRecipeBuilder.tableRecipe(EsotericismTinkerSmeltery.ashenGlassPane)
                                .setFluidAndTime(TinkerFluids.moltenQuartz, FluidValues.GEM_SHARD)
                                .setCast(EsotericismTinkerSmeltery.ashenBrick.get(), true)
                                .save(consumer, location(castingFolder + "glass_pane"));
        ItemCastingRecipeBuilder.tableRecipe(EsotericismTinkerSmeltery.ashenSoulGlassPane)
                                .setFluidAndTime(EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK)
                                .setCast(TinkerCommons.soulGlassPane, true)
                                .save(consumer, location(castingFolder + "glass_pane_soul"));


        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.ashenLamp)
                           .define('b', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('G', Blocks.GLOWSTONE)
                           .pattern(" b ")
                           .pattern("bGb")
                           .pattern(" b ")
                           .unlockedBy("has_item", has(Blocks.GLOWSTONE))
                           .save(consumer, prefix(EsotericismTinkerSmeltery.ashenLamp, folder));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.ashenSoulGlass)
                           .define('b', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('G', TinkerCommons.soulGlass)
                           .pattern(" b ")
                           .pattern("bGb")
                           .pattern(" b ")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, prefix(EsotericismTinkerSmeltery.ashenSoulGlass, folder));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EsotericismTinkerSmeltery.ashenTintedGlass)
                           .define('b', EsotericismTinkerSmeltery.ashenBrick.get())
                           .define('G', Tags.Items.GLASS_TINTED)
                           .pattern(" b ")
                           .pattern("bGb")
                           .pattern(" b ")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenBrick.get()))
                           .save(consumer, prefix(EsotericismTinkerSmeltery.ashenTintedGlass, folder));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, EsotericismTinkerSmeltery.ashenGlassPane, 16)
                           .define('#', EsotericismTinkerSmeltery.ashenGlass)
                           .pattern("###")
                           .pattern("###")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenGlass))
                           .save(consumer, prefix(EsotericismTinkerSmeltery.ashenGlassPane, folder));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, EsotericismTinkerSmeltery.ashenSoulGlassPane, 16)
                           .define('#', EsotericismTinkerSmeltery.ashenSoulGlass)
                           .pattern("###")
                           .pattern("###")
                           .unlockedBy("has_item", has(EsotericismTinkerSmeltery.ashenSoulGlass))
                           .save(consumer, prefix(EsotericismTinkerSmeltery.ashenSoulGlassPane, folder));
        // tanks
        MeltingRecipeBuilder.melting(NoContainerIngredient.of(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.FUEL_TANK)),
                                     EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK * 8, 3f)
                            .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM))
                            .save(consumer, location(MELTING_FOLDER + "fuel_tank"));
        MeltingRecipeBuilder.melting(NoContainerIngredient.of(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.INGOT_TANK)),
                                     EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK * 6, 2.5f)
                            .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM * 3))
                            .save(consumer, location(MELTING_FOLDER + "ingot_tank"));
        MeltingRecipeBuilder.melting(
                                    NoContainerIngredient.of(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.FUEL_GAUGE), EsotericismTinkerSmeltery.ashenTank.get(
                                            SearedTankBlock.TankType.INGOT_GAUGE)), EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK * 4, 2f)
                            .addByproduct(TinkerFluids.moltenQuartz.result(FluidValues.GEM * 5))
                            .save(consumer, location(MELTING_FOLDER + "gauge"));
        // tank filling - scorched
        ContainerFillingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.INGOT_TANK), FluidValues.INGOT)
                                     .save(consumer, location(folder + "filling/ashen_ingot_tank"));
        ContainerFillingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.INGOT_GAUGE), FluidValues.INGOT)
                                     .save(consumer, location(folder + "filling/ashen_ingot_gauge"));
        ContainerFillingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.FUEL_TANK), FluidType.BUCKET_VOLUME / 4)
                                     .save(consumer, location(folder + "filling/ashen_fuel_tank"));
        ContainerFillingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.FUEL_GAUGE), FluidType.BUCKET_VOLUME / 4)
                                     .save(consumer, location(folder + "filling/ashen_fuel_gauge"));

        ItemCastingRecipeBuilder.tableRecipe(EsotericismTinkerSmeltery.ashenAlloySwitch)
                                .setCast(TinkerSmeltery.smelteryController, true)
                                .setFluidAndTime(EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK_BLOCK * 9)
                                .save(consumer, location(CASTING_FOLDER + "ashen_alloy_switch"));

        ItemCastingRecipeBuilder.basinRecipe(EsotericismTinkerSmeltery.ashenMeltSwitch)
                                .setCast(TinkerSmeltery.foundryController, true)
                                .setFluidAndTime(EsotericismTinkerFluids.molten_ender_ash, FluidValues.BRICK_BLOCK * 9)
                                .save(consumer, location(CASTING_FOLDER + "ashen_melt_switch"));

    }

    private void ashenStonecutter(Consumer<FinishedRecipe> consumer, ItemLike output, String folder) {
        SingleItemRecipeBuilder.stonecutting(
                                       CompoundIngredient.of(
                                               Ingredient.of(EsotericismTinkerSmeltery.ashenStone),
                                               DifferenceIngredient.of(Ingredient.of(EsotericismTinkerTagKeys.Items.ASHEN_BLOCKS), Ingredient.of(output))),
                                       RecipeCategory.BUILDING_BLOCKS,
                                       output,
                                       1)
                               .unlockedBy("has_stone", has(EsotericismTinkerSmeltery.ashenStone))
                               .unlockedBy("has_bricks", has(EsotericismTinkerTagKeys.Items.ASHEN_BLOCKS))
                               .save(consumer, wrap(id(output), folder, "_stonecutting"));
    }

    /**
     * Adds a recipe to create the given seared block using molten clay on stone
     *
     * @param consumer Recipe consumer
     * @param block    Output block
     * @param cast     Cast item
     * @param location Recipe location
     */
    private void ashenCasting(Consumer<FinishedRecipe> consumer, ItemLike block, Ingredient cast, String location) {
        ashenCasting(consumer, block, cast, FluidValues.SLIMEBALL * 2, location);
    }

    /**
     * Adds a recipe to create the given seared slab block using molten clay on stone
     *
     * @param consumer Recipe consumer
     * @param block    Output block
     * @param cast     Cast item
     * @param location Recipe location
     */
    private void ashenSlabCasting(Consumer<FinishedRecipe> consumer, ItemLike block, Ingredient cast, String location) {
        ashenCasting(consumer, block, cast, FluidValues.SLIMEBALL, location);
    }

    /**
     * Adds a recipe to create the given seared block using molten clay on stone
     *
     * @param consumer Recipe consumer
     * @param block    Output block
     * @param cast     Cast item
     * @param amount   Amount of fluid needed
     * @param location Recipe location
     */
    private void ashenCasting(Consumer<FinishedRecipe> consumer, ItemLike block, Ingredient cast, int amount, String location) {
        ItemCastingRecipeBuilder.basinRecipe(block)
                                .setFluidAndTime(TinkerFluids.enderSlime, amount)
                                .setCast(cast, true)
                                .save(consumer, location(location));
    }

    private void cast(Fluid fluid, ItemLike ingredient, int amount, Consumer<FinishedRecipe> consumer) {
        CastItemObject cast =
                FluidValues.GEM == amount ? TinkerSmeltery.gemCast :
                FluidValues.INGOT == amount || FluidValues.BRICK == amount ? TinkerSmeltery.ingotCast : TinkerSmeltery.nuggetCast;
        ItemCastingRecipeBuilder.tableRecipe(ingredient).setCoolingTime(IMeltingRecipe.getTemperature(fluid), amount)
                                .setFluid(FluidIngredient.of(new FluidStack(fluid, amount)))
                                .setCast(cast.getSingleUseTag(), true)
                                .save(consumer, location(
                                        CASTING_FOLDER + Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(ingredient.asItem())).getPath() + "/single"));
        ItemCastingRecipeBuilder.tableRecipe(ingredient).setCoolingTime(IMeltingRecipe.getTemperature(fluid), amount)
                                .setFluid(FluidIngredient.of(new FluidStack(fluid, amount)))
                                .setCast(cast.getMultiUseTag(), false).save(consumer, location(
                                        CASTING_FOLDER + Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(ingredient.asItem())).getPath() + "/multi"));
    }


    @Override
    public @NotNull String getModId() {
        return EsotericismTinker.MODID;
    }
}
