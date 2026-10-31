package org.brahypno.esotericismtinker.plugin.JEI;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.brahypno.esotericismtinker.smeltery.block.component.AshenButtonBlock;
import slimeknights.tconstruct.library.recipe.melting.IDisplayableMeltingRecipe;

/** Uses the shared melting layout with the recipe's original output amounts, without the v1 exchange. */
public class TransmuteV2Category extends TransmuteCategory {
    public static final RecipeType<IDisplayableMeltingRecipe> TYPE =
            RecipeType.create(EsotericismTinker.MODID, "transmute_v2", IDisplayableMeltingRecipe.class);

    public TransmuteV2Category(IGuiHelper helper) {
        super(helper, TYPE, Component.translatable("jei.esotericism_tinker.transmute_v2.title"), createModeIcon());
    }

    private static IDrawable createModeIcon() {
        var state = EsotericismTinkerSmeltery.ashenMeltSwitch.get().defaultBlockState()
                .setValue(AshenButtonBlock.Function_Set, 3);
        var stack = new ItemStack(EsotericismTinkerSmeltery.ashenMeltSwitch);
        return new IDrawable() {
            @Override
            public int getWidth() {
                return 16;
            }

            @Override
            public int getHeight() {
                return 16;
            }

            @Override
            public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
                var minecraft = Minecraft.getInstance();
                // Resolve the block state each draw so resource reloads update the fourth-mode model too.
                var model = minecraft.getBlockRenderer().getBlockModel(state);
                var pose = graphics.pose();
                pose.pushPose();
                pose.translate(xOffset + 8, yOffset + 8, 150);
                pose.scale(16, -16, 16);
                minecraft.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, pose, graphics.bufferSource(),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
                graphics.flush();
                pose.popPose();
            }
        };
    }
}
