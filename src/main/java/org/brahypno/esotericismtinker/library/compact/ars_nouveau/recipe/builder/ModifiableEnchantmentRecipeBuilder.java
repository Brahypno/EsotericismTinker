package org.brahypno.esotericismtinker.library.compact.ars_nouveau.recipe.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import org.brahypno.esotericismtinker.library.compact.ars_nouveau.NovaRegistry;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModifiableEnchantmentRecipeBuilder implements RecipeBuilder {
    private Ingredient tools = Ingredient.EMPTY;
    private final List<Ingredient> pedestalItems = new ArrayList<>();

    private final ModifierId resultModifier;
    private final int minLevel;
    private final int maxLevel;
    private int source = 0;

    @Nullable
    private SlotType.SlotCount slots;

    private boolean allowCrystal = false;
    private boolean checkTraitLevel = false;

    private ModifiableEnchantmentRecipeBuilder(ModifierId resultModifier, int minLevel, int maxLevel) {
        this.resultModifier = resultModifier;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public static ModifiableEnchantmentRecipeBuilder modifier(ModifierId resultModifier) {
        return new ModifiableEnchantmentRecipeBuilder(resultModifier, 1, 1);
    }

    public static ModifiableEnchantmentRecipeBuilder modifier(ModifierId resultModifier, int level) {
        return new ModifiableEnchantmentRecipeBuilder(resultModifier, level, level);
    }

    public static ModifiableEnchantmentRecipeBuilder modifier(ModifierId resultModifier, int minLevel, int maxLevel) {
        return new ModifiableEnchantmentRecipeBuilder(resultModifier, minLevel, maxLevel);
    }

    public static ModifiableEnchantmentRecipeBuilder modifier(StaticModifier<?> result) {
        return modifier(result.getId());
    }

    public static ModifiableEnchantmentRecipeBuilder modifier(StaticModifier<?> result, int level) {
        return modifier(result.getId(), level);
    }

    public static ModifiableEnchantmentRecipeBuilder modifier(StaticModifier<?> result, int minLevel, int maxLevel) {
        return modifier(result.getId(), minLevel, maxLevel);
    }

    public ModifiableEnchantmentRecipeBuilder setTools(Ingredient tools) {
        this.tools = tools;
        return this;
    }

    public ModifiableEnchantmentRecipeBuilder setTools(TagKey<Item> tag) {
        return setTools(Ingredient.of(tag));
    }

    public ModifiableEnchantmentRecipeBuilder setTools(ItemLike item) {
        return setTools(Ingredient.of(item));
    }

    public ModifiableEnchantmentRecipeBuilder addPedestalItem(int count, Ingredient ingredient) {
        if (count < 1){
            throw new IllegalArgumentException("Pedestal item count must be at least 1");
        }

        for (int i = 0; i < count; i++) {
            addPedestalItem(ingredient);
        }

        return this;
    }

    public ModifiableEnchantmentRecipeBuilder addPedestalItem(int count, ItemLike item) {
        return addPedestalItem(count, Ingredient.of(item));
    }

    public ModifiableEnchantmentRecipeBuilder addPedestalItem(int count, TagKey<Item> tag) {
        return addPedestalItem(count, Ingredient.of(tag));
    }

    public ModifiableEnchantmentRecipeBuilder addPedestalItem(Ingredient ingredient) {
        this.pedestalItems.add(ingredient);
        return this;
    }

    public ModifiableEnchantmentRecipeBuilder addPedestalItem(ItemLike item) {
        return addPedestalItem(Ingredient.of(item));
    }

    public ModifiableEnchantmentRecipeBuilder addPedestalItem(TagKey<Item> tag) {
        return addPedestalItem(Ingredient.of(tag));
    }

    public ModifiableEnchantmentRecipeBuilder source(int source) {
        this.source = source;
        return this;
    }

    public ModifiableEnchantmentRecipeBuilder sourceCost(int source) {
        return source(source);
    }

    public ModifiableEnchantmentRecipeBuilder slot(SlotType type, int count) {
        this.slots = new SlotType.SlotCount(type, count);
        return this;
    }

    public ModifiableEnchantmentRecipeBuilder allowCrystal() {
        this.allowCrystal = true;
        return this;
    }

    public ModifiableEnchantmentRecipeBuilder checkTraitLevel() {
        this.checkTraitLevel = true;
        return this;
    }

    @Override
    public RecipeBuilder unlockedBy(String name, CriterionTriggerInstance criterion) {
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String group) {
        return this;
    }

    @Override
    public Item getResult() {
        return Items.AIR;
    }

    @Override
    public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
        if (tools == Ingredient.EMPTY){
            throw new IllegalStateException("Must set applicable tools");
        }

        if (pedestalItems.isEmpty() && !allowCrystal){
            throw new IllegalStateException("Must have at least 1 pedestal item");
        }

        if (minLevel < 1){
            throw new IllegalStateException("Modifier recipe min level must be at least 1: " + id);
        }

        if (maxLevel < minLevel){
            throw new IllegalStateException("Modifier recipe max level must be >= min level: " + id);
        }

        consumer.accept(new Result(
                id,
                tools,
                pedestalItems,
                resultModifier,
                minLevel,
                maxLevel,
                source,
                slots,
                allowCrystal,
                checkTraitLevel
        ));
    }

    private record Result(ResourceLocation id, Ingredient tools, List<Ingredient> pedestalItems, ModifierId resultModifier, int minLevel, int maxLevel,
                          int source,
                          @Nullable SlotType.SlotCount slots, boolean allowCrystal, boolean checkTraitLevel) implements FinishedRecipe {
        private Result(
                ResourceLocation id,
                Ingredient tools,
                List<Ingredient> pedestalItems,
                ModifierId resultModifier,
                int minLevel,
                int maxLevel,
                int source,
                @Nullable SlotType.SlotCount slots,
                boolean allowCrystal,
                boolean checkTraitLevel
        ) {
            this.id = id;
            this.tools = tools;
            this.pedestalItems = List.copyOf(pedestalItems);
            this.resultModifier = resultModifier;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.source = source;
            this.slots = slots;
            this.allowCrystal = allowCrystal;
            this.checkTraitLevel = checkTraitLevel;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            json.add("tools", tools.toJson());

            JsonArray pedestalArray = new JsonArray();
            for (Ingredient ingredient : pedestalItems) {
                pedestalArray.add(ingredient.toJson());
            }
            json.add("pedestalItems", pedestalArray);

            json.addProperty("result", resultModifier.toString());

            if (minLevel == maxLevel){
                json.addProperty("level", minLevel);
            }else {
                JsonObject levelObject = new JsonObject();
                levelObject.addProperty("min", minLevel);
                levelObject.addProperty("max", maxLevel);
                json.add("level", levelObject);
            }

            json.addProperty("source", source);

            if (slots != null){
                json.add("slots", SlotType.SlotCount.LOADABLE.serialize(slots));
            }

            if (allowCrystal){
                json.addProperty("allow_crystal", true);
            }

            if (checkTraitLevel){
                json.addProperty("check_trait_level", true);
            }
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return NovaRegistry.MODIFIABLE_ENCHANTMENT_SERIALIZER.get();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return null;
        }
    }
}