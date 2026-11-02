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
        withDisplay("tool/ritual_blade/blocking", EsotericismTinkerTools.ritual_blade.getId(), toolBlocking);
        // The broken blade has its own shorter geometry; changing only the texture leaves hollow cubes.
        models.put("tool/ritual_blade/broken", readJson(EsotericismTinker.getLocation("tool/ritual_blade/broken_3d")));
    }

    @Override
    public @NotNull String getName() {
        return "EsotericismTinker Tool Item Model Provider";
    }
}
