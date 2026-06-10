package org.brahypno.esotericismtinker.tools.data.sprite;

import org.brahypno.esotericismtinker.EsotericismTinker;
import slimeknights.tconstruct.library.client.data.material.AbstractPartSpriteProvider;

public class EsotericismPartSpriteProvider extends AbstractPartSpriteProvider {
    public EsotericismPartSpriteProvider() {
        super(EsotericismTinker.MODID);
    }

    @Override
    public String getName() {
        return "Esotericism Tinker Part Sprite Provider";
    }

    @Override
    protected void addAllSpites() {
        buildTool("ritual_blade").addBreakableHead("blade").addHandle("handle");
    }
}
