package org.brahypno.esotericismtinker.common.data.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;

public class EsotericismTinkerItemModelProvider extends ItemModelProvider {
    public static final String parent_item = "item/generated";
    public static final String parent_fluid = "forge:item/bucket_drip";

    public EsotericismTinkerItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, EsotericismTinker.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        generateItemModel(EsotericismTinkerSmeltery.ashenBrick, "materials");
    }

    public void generateItemModel(RegistryObject<Item> object, String typePath) {
        withExistingParent(object.getId().getPath(), parent_item).texture("layer0", getItemLocation(object.getId().getPath(), typePath));
    }

    public void generateItemModel(ResourceLocation rs, String typePath) {
        withExistingParent(rs.getPath(), parent_item).texture("layer0", getItemLocation(rs.getPath(), typePath));
    }

    public ResourceLocation getItemLocation(String path, String typePath) {
        return new ResourceLocation(EsotericismTinker.MODID, "item/" + (typePath.isEmpty() ? typePath : typePath + "/") + path);
    }

    private ResourceLocation itemKey(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem());
    }
}
