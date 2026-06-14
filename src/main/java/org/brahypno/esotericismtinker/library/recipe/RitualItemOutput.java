package org.brahypno.esotericismtinker.library.recipe;


import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.CraftingHelper;

public record RitualItemOutput(ItemStack stack) {
    public static final RitualItemOutput EMPTY = new RitualItemOutput(ItemStack.EMPTY);

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public ItemStack createStack() {
        return stack.copy();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        json.addProperty("item", net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getKey(stack.getItem())
                .toString());

        if (stack.getCount() != 1){
            json.addProperty("count", stack.getCount());
        }

        if (stack.hasTag()){
            json.addProperty("nbt", stack.getTag().toString());
        }

        return json;
    }

    public static RitualItemOutput fromJson(JsonObject json) {
        return new RitualItemOutput(CraftingHelper.getItemStack(json, true));
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeItem(stack);
    }

    public static RitualItemOutput fromNetwork(FriendlyByteBuf buffer) {
        return new RitualItemOutput(buffer.readItem());
    }
}
