package org.brahypno.esotericismtinker.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.esotericismtinker.network.client.ClientPacketHandler;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Server -> client synchronization for the transcendence anvil.
 *
 * <p>The server remains authoritative for:</p>
 *
 * <ul>
 *     <li>custom recipe priority;</li>
 *     <li>recipe validation;</li>
 *     <li>reception/investiture previews;</li>
 *     <li>the concrete validation error shown by the GUI.</li>
 * </ul>
 */
public final class UpdateTranscendenceAnvilRecipePacket {
    private final long blockPos;

    @Nullable
    private final ResourceLocation recipeId;

    private final ItemStack preview;

    @Nullable
    private final Component error;

    public UpdateTranscendenceAnvilRecipePacket(
            long blockPos,
            @Nullable ResourceLocation recipeId,
            ItemStack preview,
            @Nullable Component error
    ) {
        this.blockPos = blockPos;
        this.recipeId = recipeId;
        this.preview = preview.copy();
        this.error = error;
    }

    public UpdateTranscendenceAnvilRecipePacket(FriendlyByteBuf buffer) {
        this.blockPos = buffer.readLong();

        this.recipeId = buffer.readBoolean()
                        ? buffer.readResourceLocation()
                        : null;

        this.preview = buffer.readItem();

        this.error = buffer.readBoolean()
                     ? buffer.readComponent()
                     : null;
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeLong(blockPos);

        buffer.writeBoolean(recipeId != null);
        if (recipeId != null){
            buffer.writeResourceLocation(recipeId);
        }

        buffer.writeItem(preview);

        buffer.writeBoolean(error != null);
        if (error != null){
            buffer.writeComponent(error);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleUpdateTranscendenceAnvilRecipe(blockPos, recipeId, preview, error)));

        context.setPacketHandled(true);
    }

}
