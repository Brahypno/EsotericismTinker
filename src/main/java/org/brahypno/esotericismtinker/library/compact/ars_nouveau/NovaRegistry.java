package org.brahypno.esotericismtinker.library.compact.ars_nouveau;

import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.compact.ars_nouveau.recipe.ModifiableEnchantmentRecipe;
import org.brahypno.esotericismtinker.library.compact.ars_nouveau.recipe.ModifiableEnchantmentRecipeSerializer;

public class NovaRegistry {
    public static void init(IEventBus modBus) {
        NovaRegistry.RECIPE_TYPES.register(modBus);
        NovaRegistry.RECIPE_SERIALIZERS.register(modBus);

        modBus.addListener(NovaRegistry::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ArsNouveauAPI.getInstance()
                                             .getEnchantingRecipeTypes()
                                             .add(MODIFIABLE_ENCHANTMENT_TYPE.get()));
    }

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.RECIPE_SERIALIZERS, EsotericismTinker.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.Keys.RECIPE_TYPES, EsotericismTinker.MODID);
    public static final RegistryObject<RecipeType<ModifiableEnchantmentRecipe>> MODIFIABLE_ENCHANTMENT_TYPE =
            RECIPE_TYPES.register("modifiable_enchantment", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return EsotericismTinker.MODID + ":modifiable_enchantment";
                }
            });

    public static final RegistryObject<RecipeSerializer<ModifiableEnchantmentRecipe>> MODIFIABLE_ENCHANTMENT_SERIALIZER =
            RECIPE_SERIALIZERS.register("modifiable_enchantment", ModifiableEnchantmentRecipeSerializer::new);

}
