package org.brahypno.esotericismtinker.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.esotericismtinker.transcendence.table.block.TranscendenceAnvilBlockEntity;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;

import javax.annotation.Nullable;
import java.util.Optional;
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

        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            Level level = minecraft.level;

            if (level == null){
                return;
            }

            BlockPos pos = BlockPos.of(blockPos);
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (!(blockEntity instanceof TranscendenceAnvilBlockEntity anvil)){
                return;
            }

            ITinkerStationRecipe recipe = resolveRecipe(level);

            /*
             * Recipe、preview 和 error 必须作为同一次服务端计算的结果
             * 一起提交，不能再通过多个方法分别更新。
             */
            anvil.updateClientResult(
                    recipe,
                    preview,
                    error
            );
        });

        context.setPacketHandled(true);
    }

    @Nullable
    private ITinkerStationRecipe resolveRecipe(Level level) {
        if (recipeId == null){
            return null;
        }

        Optional<? extends Recipe<?>> found =
                level.getRecipeManager().byKey(recipeId);

        if (found.isPresent()
            && found.get() instanceof ITinkerStationRecipe stationRecipe){
            return stationRecipe;
        }

        return null;
    }
}