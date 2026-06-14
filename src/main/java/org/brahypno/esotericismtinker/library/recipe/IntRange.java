package org.brahypno.esotericismtinker.library.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;

public record IntRange(int min, int max) {
    public static final IntRange ANY = new IntRange(0, Integer.MAX_VALUE);

    public boolean contains(int value) {
        return value >= min && value <= max;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        if (min > 0){
            json.addProperty("min", min);
        }

        if (max < Integer.MAX_VALUE){
            json.addProperty("max", max);
        }

        return json;
    }

    public static IntRange fromJson(JsonObject json) {
        return new IntRange(
                GsonHelper.getAsInt(json, "min", 0),
                GsonHelper.getAsInt(json, "max", Integer.MAX_VALUE));
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarInt(min);
        buffer.writeVarInt(max);
    }

    public static IntRange fromNetwork(FriendlyByteBuf buffer) {
        return new IntRange(buffer.readVarInt(), buffer.readVarInt());
    }
}
