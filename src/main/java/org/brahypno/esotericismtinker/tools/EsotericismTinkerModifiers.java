package org.brahypno.esotericismtinker.tools;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.EsotericismTinkerModule;
import org.brahypno.esotericismtinker.tools.modifiers.tools.ritual_blade.SelfSacrifice;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public final class EsotericismTinkerModifiers extends EsotericismTinkerModule {
    public static ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(EsotericismTinker.MODID);
    public static final StaticModifier<SelfSacrifice> self_sacrifice = MODIFIERS.register("self_sacrifice", SelfSacrifice::new);

    @SuppressWarnings({"removal"})
    public EsotericismTinkerModifiers() {
        MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
