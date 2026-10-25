package org.brahypno.esotericismtinker.network.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.brahypno.esotericismtinker.transcendence.table.block.TranscendenceAnvilBlockEntity;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;

import javax.annotation.Nullable;
import java.util.Optional;

/** Client-side handling, kept separate from packets registered on both physical sides. */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handleUpdateTranscendenceAnvilRecipe(
            long blockPos,
            @Nullable ResourceLocation recipeId,
            ItemStack preview,
            @Nullable Component error
    ) {
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

        ITinkerStationRecipe recipe = resolveRecipe(level, recipeId);

        /*
         * Recipe、preview 和 error 必须作为同一次服务端计算的结果
         * 一起提交，不能再通过多个方法分别更新。
         */
        anvil.updateClientResult(
                recipe,
                preview,
                error
        );
    }

    @Nullable
    private static ITinkerStationRecipe resolveRecipe(Level level, @Nullable ResourceLocation recipeId) {
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
