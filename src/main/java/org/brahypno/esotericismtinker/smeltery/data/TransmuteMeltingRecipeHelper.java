package org.brahypno.esotericismtinker.smeltery.data;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.recipe.condition.TagFilledCondition;
import slimeknights.mantle.recipe.data.ConsumerWrapperBuilder;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Data generation helper that writes the standard "material form" recipes (ingot, nugget, block, gear, plate, ore, ...)
 * for {@code esotericism_tinker:transmute_v2}, the transmute furnace's transcendence mode.
 *
 * <p>It is the v2 counterpart of Tinkers' {@code SmelteryRecipeBuilder}: the same fluid based, tag driven helpers
 * ({@link #metal()}, {@link #ore()}, {@link #gear()}, ...), except that only melting recipes are generated and every
 * output amount is scaled by a configurable {@link #ratio(int, int)}. The ratio is what makes a form melt into less
 * than its usual amount, for example {@code ratio(1, 9)} makes a metal block melt into a single ingot worth of fluid.
 *
 * <p>Usage is the same three steps as Tinkers' builder:
 * <ol>
 *   <li>Create the builder with {@link #fluid(Consumer, String, FluidObject)} or one of its overloads.</li>
 *   <li>Optionally configure it: {@link #ratio(int, int)}, {@link #optional()}, {@link #temperature(int)},
 *       {@link #time(int)}, {@link #baseUnit(int)}, {@link #byproduct(FluidOutput)}, {@link #meltingFolder(String)}.</li>
 *   <li>Call the form helpers, each of which saves its recipes right away and returns {@code this} for chaining.</li>
 * </ol>
 *
 * <pre>{@code
 * // 1 block -> 1 ingot worth, 1 ingot -> 1/9 ingot worth, ...
 * TransmuteMeltingRecipeHelper.fluid(consumer, "iron", EsotericismTinkerFluids.some_fluid)
 *     .ratio(1, 9)
 *     .metal()   // block, ingot, nugget
 *     .gear();   // gears/iron
 * }</pre>
 *
 * <p>Recipes are saved under {@code <namespace>:smeltery/melting/transmute/<name>/<variant>} by default.
 * Amounts that scale down to zero are skipped instead of producing an empty recipe.
 */
public final class TransmuteMeltingRecipeHelper {
    /** Default folder for generated recipes, matching the transmute v2 recipes written by addons. */
    public static final String DEFAULT_FOLDER = "smeltery/melting/transmute/";

    /** Consumer receiving the finished recipes. */
    private final Consumer<FinishedRecipe> consumer;
    /** Recipe domain plus material name; the path is used for tags and output locations. */
    private final ResourceLocation name;
    /** Creates the fluid result for the given amount in mB. */
    private final IntFunction<FluidOutput> result;

    /** Required temperature for generated recipes. */
    private int temperature;
    /** Amount in mB for one "unit" of the material, by default a single ingot. */
    private int baseUnit = FluidValues.INGOT;
    /** Numerator of the amount ratio applied to every output. */
    private int amountNumerator = 1;
    /** Denominator of the amount ratio applied to every output. */
    private int amountDenominator = 1;
    /** Explicit melting time in Tinkers' steps, or a value below one to derive it from the output amount. */
    private int timeOverride = -1;
    /** If true, every tag based recipe receives a "tag filled" condition. */
    private boolean optional = false;
    /** Folder generated recipes are saved into; always ends in a slash. */
    private String meltingFolder = DEFAULT_FOLDER;
    /** Byproducts added to every generated recipe. */
    private final List<FluidOutput> byproducts = new ArrayList<>();

    private TransmuteMeltingRecipeHelper(Consumer<FinishedRecipe> consumer, ResourceLocation name, IntFunction<FluidOutput> result, int temperature) {
        this.consumer = consumer;
        this.name = name;
        this.result = result;
        this.temperature = temperature;
    }

    /* Constructors */

    /**
     * Creates a builder for the given fluid. Recipes are saved in the given location's namespace, its path is the
     * material name used for the common item tags ({@code forge:ingots/<name>}, {@code forge:gears/<name>}, ...).
     */
    public static TransmuteMeltingRecipeHelper fluid(Consumer<FinishedRecipe> consumer, ResourceLocation name, FluidObject<?> fluid) {
        return new TransmuteMeltingRecipeHelper(consumer, name, fluid::result, IMeltingRecipe.getTemperature(fluid));
    }

    /** Creates a builder for the given fluid in this mod's namespace. */
    public static TransmuteMeltingRecipeHelper fluid(Consumer<FinishedRecipe> consumer, String name, FluidObject<?> fluid) {
        return fluid(consumer, EsotericismTinker.getLocation(name), fluid);
    }

    /** Creates a builder for the given fluid. Recipes are saved in the given location's namespace. */
    public static TransmuteMeltingRecipeHelper fluid(Consumer<FinishedRecipe> consumer, ResourceLocation name, Fluid fluid) {
        return new TransmuteMeltingRecipeHelper(consumer, name, amount -> FluidOutput.fromFluid(fluid, amount), IMeltingRecipe.getTemperature(fluid));
    }

    /** Creates a builder for the given fluid in this mod's namespace. */
    public static TransmuteMeltingRecipeHelper fluid(Consumer<FinishedRecipe> consumer, String name, Fluid fluid) {
        return fluid(consumer, EsotericismTinker.getLocation(name), fluid);
    }

    /* Configuration */

    /** Marks all following tag based recipes as optional, using a "tag filled" condition like Tinkers does. */
    public TransmuteMeltingRecipeHelper optional() {
        this.optional = true;
        return this;
    }

    /** Overrides the temperature of following recipes, which defaults to the fluid's own temperature. */
    public TransmuteMeltingRecipeHelper temperature(int temperature) {
        if (temperature < 0) throw new IllegalArgumentException("Temperature must be non-negative");
        this.temperature = temperature;
        return this;
    }

    /** Sets a fixed melting time in Tinkers' steps for following recipes, instead of deriving it from the amount. */
    public TransmuteMeltingRecipeHelper time(int time) {
        if (time <= 0) throw new IllegalArgumentException("Melting time must be positive");
        this.timeOverride = time;
        return this;
    }

    /** Sets how much fluid one unit of the material produces, by default a single ingot. */
    public TransmuteMeltingRecipeHelper baseUnit(int baseUnit) {
        if (baseUnit <= 0) throw new IllegalArgumentException("Base unit must be positive");
        this.baseUnit = baseUnit;
        return this;
    }

    /**
     * Scales every following output amount by the given ratio, rounding down.
     * Use {@link #divideBy(int)} for the common "divided by nine" case.
     *
     * @param numerator   ratio numerator, at least one
     * @param denominator ratio denominator, at least one
     */
    public TransmuteMeltingRecipeHelper ratio(int numerator, int denominator) {
        if (numerator <= 0) throw new IllegalArgumentException("Ratio numerator must be positive");
        if (denominator <= 0) throw new IllegalArgumentException("Ratio denominator must be positive");
        this.amountNumerator = numerator;
        this.amountDenominator = denominator;
        return this;
    }

    /** Divides every following output amount by the given value, for example nine to turn a block into one ingot. */
    public TransmuteMeltingRecipeHelper divideBy(int divisor) {
        return ratio(1, divisor);
    }

    /** Multiplies every following output amount by the given value. */
    public TransmuteMeltingRecipeHelper multiplyBy(int factor) {
        return ratio(factor, 1);
    }

    /** Sets the folder following recipes are saved into, relative to the recipe domain. */
    public TransmuteMeltingRecipeHelper meltingFolder(String folder) {
        this.meltingFolder = folder.isEmpty() || folder.endsWith("/") ? folder : folder + '/';
        return this;
    }

    /** Adds a byproduct to every following recipe. */
    public TransmuteMeltingRecipeHelper byproduct(FluidOutput byproduct) {
        this.byproducts.add(byproduct);
        return this;
    }

    /** Adds a byproduct stack to every following recipe. */
    public TransmuteMeltingRecipeHelper byproduct(FluidStack byproduct) {
        return byproduct(FluidOutput.fromStack(byproduct));
    }

    /* Common forms */

    /** Melts the metal block tag into nine units. */
    public TransmuteMeltingRecipeHelper block() {
        return commonMelting("block", "storage_blocks", 9, false);
    }

    /** Melts the ingot tag into one unit. */
    public TransmuteMeltingRecipeHelper ingot() {
        return commonMelting("ingot", "ingots", 1, false);
    }

    /** Melts the nugget tag into one ninth of a unit. */
    public TransmuteMeltingRecipeHelper nugget() {
        return commonMelting("nugget", "nuggets", 1 / 9f, false);
    }

    /** Melts blocks, ingots, and nuggets, the same shapes Tinkers' {@code metal()} helper covers. */
    public TransmuteMeltingRecipeHelper metal() {
        this.baseUnit = FluidValues.INGOT;
        block();
        ingot();
        nugget();
        return this;
    }

    /** Melts gem blocks of the given storage size and the gem itself; {@link #smallGem()} and {@link #largeGem()} cover the usual sizes. */
    public TransmuteMeltingRecipeHelper gem(int storageSize) {
        this.baseUnit = FluidValues.GEM;
        commonMelting("block", "storage_blocks", storageSize, false);
        commonMelting("gem", "gems", 1, false);
        return this;
    }

    /** Melts a four block gem, such as amethyst or quartz. */
    public TransmuteMeltingRecipeHelper smallGem() {
        return gem(4);
    }

    /** Melts a nine block gem, such as diamond or emerald. */
    public TransmuteMeltingRecipeHelper largeGem() {
        return gem(9);
    }

    /** Melts the dust tag into one unit. */
    public TransmuteMeltingRecipeHelper dust() {
        return commonMelting("dust", "dusts", 1, true);
    }

    /** Melts the plate tag into one unit. */
    public TransmuteMeltingRecipeHelper plate() {
        return commonMelting("plate", "plates", 1, true);
    }

    /** Melts the gear tag into four units. */
    public TransmuteMeltingRecipeHelper gear() {
        return commonMelting("gear", "gears", 4, true);
    }

    /** Melts the rod tag into half a unit. */
    public TransmuteMeltingRecipeHelper rod() {
        return commonMelting("rod", "rods", 0.5f, true);
    }

    /** Melts the coin tag into a third of a unit. */
    public TransmuteMeltingRecipeHelper coin() {
        return commonMelting("coin", "coins", 1 / 3f, true);
    }

    /** Melts the sheetmetal tag into one unit. */
    public TransmuteMeltingRecipeHelper sheetmetal() {
        return commonMelting("sheetmetal", "sheetmetals", 1, true);
    }

    /** Melts the wire tag into half a unit. */
    public TransmuteMeltingRecipeHelper wire() {
        return commonMelting("wire", "wires", 0.5f, true);
    }

    /** Melts the ore tag into two units, the usual yield of Tinkers' singular ores. */
    public TransmuteMeltingRecipeHelper ore() {
        return ore(2);
    }

    /** Melts the ore tag into the given number of units. */
    public TransmuteMeltingRecipeHelper ore(float units) {
        return commonMelting("ore", "ores", units, false);
    }

    /** Melts raw materials into one unit and raw storage blocks into nine units. */
    public TransmuteMeltingRecipeHelper rawOre() {
        tagMelting("raw", tagPath("raw_materials"), 1, false);
        tagMelting("raw_block", "storage_blocks/raw_" + this.name.getPath(), 9, false);
        return this;
    }

    /* Custom recipes */

    /** Melts everything in the given common item tag; the tag must not be empty for the recipe to load when made optional. */
    public TransmuteMeltingRecipeHelper melting(String variant, float units, TagKey<Item> tag, boolean forceOptional) {
        return addRecipe(Ingredient.of(tag), variant, units, forceOptional, tag);
    }

    /** Melts everything in the given ingredient; {@link #optional()} cannot condition it, so it is always generated. */
    public TransmuteMeltingRecipeHelper melting(String variant, float units, Ingredient ingredient, boolean forceOptional) {
        return addRecipe(ingredient, variant, units, forceOptional, null);
    }

    /** Melts a single item, saving under the given variant name. */
    public TransmuteMeltingRecipeHelper melting(String variant, float units, ItemLike item, boolean forceOptional) {
        return melting(variant, units, Ingredient.of(item), forceOptional);
    }

    /* Internals */

    /** Builds the common tag path of the material, for example {@code ingots/iron}. */
    private String tagPath(String tagPrefix) {
        return tagPrefix + '/' + this.name.getPath();
    }

    /** Melts a tag under the common namespace via the given tag prefix directory. */
    private TransmuteMeltingRecipeHelper commonMelting(String variant, String tagPrefix, float units, boolean forceOptional) {
        return tagMelting(variant, tagPath(tagPrefix), units, forceOptional);
    }

    /** Melts a common item tag, saving under the given variant name. */
    private TransmuteMeltingRecipeHelper tagMelting(String variant, String tagPath, float units, boolean forceOptional) {
        TagKey<Item> tag = EsotericismTinker.forgeItemTag(tagPath);
        return addRecipe(Ingredient.of(tag), variant, units, forceOptional, tag);
    }

    /** Converts a unit multiplier into a nominal amount in mB, before the ratio is applied. */
    private int unitAmount(float units) {
        return (int) Math.round(this.baseUnit * (double) units);
    }

    /** Applies the configured ratio to a nominal amount, rounding down. */
    private int scaledAmount(int nominalAmount) {
        return (int) ((long) nominalAmount * this.amountNumerator / this.amountDenominator);
    }

    /** Derives the melting time in Tinkers' steps, unless a fixed time was configured. */
    private int timeFor(int amount) {
        if (this.timeOverride > 0) return this.timeOverride;
        return Math.max(1, IMeltingRecipe.calcTimeForAmount(this.temperature, amount));
    }

    /** Creates and saves a single transmute v2 melting recipe. */
    private TransmuteMeltingRecipeHelper addRecipe(Ingredient input, String variant, float units, boolean forceOptional, @Nullable TagKey<Item> conditionTag) {
        int amount = scaledAmount(unitAmount(units));
        // amounts below one millibucket cannot be expressed, so skip the form instead of emitting an empty recipe
        if (amount <= 0) return this;

        Consumer<FinishedRecipe> target = this.consumer;
        if ((this.optional || forceOptional) && conditionTag != null) {
            target = ConsumerWrapperBuilder.wrap().addCondition(new TagFilledCondition<>(conditionTag)).build(this.consumer);
        }

        TransmuteMeltingRecipeBuilder builder = TransmuteMeltingRecipeBuilder.melting(input, this.result.apply(amount), this.temperature, timeFor(amount));
        for (FluidOutput byproduct : this.byproducts) {
            builder.addByproduct(byproduct);
        }
        builder.save(target, this.name.withPath(this.meltingFolder + this.name.getPath() + '/' + variant));
        return this;
    }
}
