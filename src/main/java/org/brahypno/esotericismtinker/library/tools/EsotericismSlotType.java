package org.brahypno.esotericismtinker.library.tools;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.brahypno.esotericismtinker.EsotericismTinker;
import slimeknights.mantle.client.model.NBTKeyModel;
import slimeknights.tconstruct.library.tools.SlotType;

import static slimeknights.tconstruct.library.tools.SlotType.getOrCreate;

public class EsotericismSlotType {
    public static final SlotType DELUSION = getOrCreate("delusions");

    @OnlyIn(Dist.CLIENT)
    public static void init() {
        NBTKeyModel.registerExtraTexture(new ResourceLocation("tconstruct:creative_slot"), DELUSION.getName(),
                                         new ResourceLocation(EsotericismTinker.MODID, "item/slot/delusion"));
    }
}
