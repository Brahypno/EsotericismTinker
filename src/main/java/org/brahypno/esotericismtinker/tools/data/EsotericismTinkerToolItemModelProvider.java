package org.brahypno.esotericismtinker.tools.data;

import com.google.gson.JsonObject;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerTools;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.data.AbstractToolItemModelProvider;

import java.io.IOException;

/**
 * Provider for tool models, mostly used for duplicating displays
 */
public class EsotericismTinkerToolItemModelProvider extends AbstractToolItemModelProvider {
    public EsotericismTinkerToolItemModelProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, existingFileHelper, EsotericismTinker.MODID);
    }

    @Override
    protected void addModels() throws IOException {
        JsonObject toolBlocking = readJson(TConstruct.getResource("base/tool_blocking"));
        //JsonObject shieldBlocking = readJson(EsotericismTinker.getLocation("base/shield_blocking"));

        // blocking //
        // pickaxe
        tool(EsotericismTinkerTools.ritual_blade, toolBlocking, "blade");
    }

    @Override
    public @NotNull String getName() {
        return "EsotericismTinker Tool Item Model Provider";
    }
}
