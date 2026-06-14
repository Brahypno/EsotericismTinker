package org.brahypno.esotericismtinker.library.compact.ars_nouveau.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.SlotType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ModifiableEnchantmentRecipeSerializer implements RecipeSerializer<ModifiableEnchantmentRecipe> {
    @Override
    public @NotNull ModifiableEnchantmentRecipe fromJson(
            @NotNull ResourceLocation recipeId,
            @NotNull JsonObject json
    ) {
        Ingredient tools = Ingredient.fromJson(GsonHelper.getNonNull(json, "tools"));

        JsonArray pedestalJson = GsonHelper.getAsJsonArray(json, "pedestalItems");
        List<Ingredient> pedestalItems = new ArrayList<>();

        for (JsonElement element : pedestalJson) {
            pedestalItems.add(Ingredient.fromJson(element));
        }

        ModifierId result = new ModifierId(new ResourceLocation(GsonHelper.getAsString(json, "result")));

        ModifiableEnchantmentRecipe.LevelRange level = parseLevel(json.get("level"));

        int sourceCost = json.has("source")
                         ? GsonHelper.getAsInt(json, "source")
                         : GsonHelper.getAsInt(json, "sourceCost", 0);

        SlotType.SlotCount slots = null;
        if (json.has("slots")){
            slots = SlotType.SlotCount.LOADABLE.convert(json.get("slots"), "slots", TypedMap.EMPTY);
        }

        boolean allowCrystal = GsonHelper.getAsBoolean(json, "allow_crystal", false);
        boolean checkTraitLevel = GsonHelper.getAsBoolean(json, "check_trait_level", false);

        return new ModifiableEnchantmentRecipe(
                recipeId,
                tools,
                pedestalItems,
                result,
                level,
                sourceCost,
                slots,
                allowCrystal,
                checkTraitLevel
        );
    }

    @Override
    public @Nullable ModifiableEnchantmentRecipe fromNetwork(
            @NotNull ResourceLocation recipeId,
            @NotNull FriendlyByteBuf buffer
    ) {
        Ingredient tools = Ingredient.fromNetwork(buffer);

        int pedestalSize = buffer.readVarInt();
        List<Ingredient> pedestalItems = new ArrayList<>();
        for (int i = 0; i < pedestalSize; i++) {
            pedestalItems.add(Ingredient.fromNetwork(buffer));
        }

        ModifierId result = new ModifierId(buffer.readResourceLocation());
        ModifiableEnchantmentRecipe.LevelRange level =
                new ModifiableEnchantmentRecipe.LevelRange(buffer.readVarInt(), buffer.readVarInt());

        int sourceCost = buffer.readVarInt();

        SlotType.SlotCount slots = null;
        if (buffer.readBoolean()){
            SlotType type = SlotType.read(buffer);
            int count = buffer.readVarInt();
            slots = new SlotType.SlotCount(type, count);
        }

        boolean allowCrystal = buffer.readBoolean();
        boolean checkTraitLevel = buffer.readBoolean();

        return new ModifiableEnchantmentRecipe(
                recipeId,
                tools,
                pedestalItems,
                result,
                level,
                sourceCost,
                slots,
                allowCrystal,
                checkTraitLevel
        );
    }

    @Override
    public void toNetwork(
            @NotNull FriendlyByteBuf buffer,
            @NotNull ModifiableEnchantmentRecipe recipe
    ) {
        recipe.getTools().toNetwork(buffer);

        List<Ingredient> pedestalItems = recipe.getPedestalIngredients();
        buffer.writeVarInt(pedestalItems.size());
        for (Ingredient ingredient : pedestalItems) {
            ingredient.toNetwork(buffer);
        }

        buffer.writeResourceLocation(recipe.getResultModifier());
        buffer.writeVarInt(recipe.getLevel().min());
        buffer.writeVarInt(recipe.getLevel().max());

        buffer.writeVarInt(recipe.getSourceCost());

        SlotType.SlotCount slots = recipe.getSlots();
        buffer.writeBoolean(slots != null);
        if (slots != null){
            slots.type().write(buffer);
            buffer.writeVarInt(slots.count());
        }

        buffer.writeBoolean(recipe.allowCrystal());
        buffer.writeBoolean(recipe.checkTraitLevel());
    }

    protected static ModifiableEnchantmentRecipe.LevelRange parseLevel(JsonElement element) {
        if (element == null || element.isJsonNull()){
            return ModifiableEnchantmentRecipe.LevelRange.exact(1);
        }

        if (element.isJsonPrimitive()){
            return ModifiableEnchantmentRecipe.LevelRange.exact(element.getAsInt());
        }

        JsonObject object = element.getAsJsonObject();
        int min = GsonHelper.getAsInt(object, "min", 1);
        int max = GsonHelper.getAsInt(object, "max", min);

        return new ModifiableEnchantmentRecipe.LevelRange(min, max);
    }
}