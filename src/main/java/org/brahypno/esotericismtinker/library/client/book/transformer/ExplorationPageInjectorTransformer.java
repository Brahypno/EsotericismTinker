package org.brahypno.esotericismtinker.library.client.book.transformer;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.brahypno.esotericismtinker.EsotericismTinker;
import slimeknights.mantle.client.book.data.content.ContentShowcase;
import slimeknights.mantle.client.book.data.content.ContentText;
import slimeknights.mantle.client.book.data.element.TextData;

/**
 * 自动向独立书的 exploration 章节注入页面。
 * <p>
 * 入口 tag:
 * esotericismtinker:book/exploration
 * <p>
 * 页面路径:
 * pages/exploration/<namespace>_<path>.json
 * <p>
 * 例如:
 * dreamtinker:moonlight_ice
 * <p>
 * 会尝试加载:
 * assets/esotericismtinker/book/esoteric_rituals/exploration/${tagname}/dreamtinker_moonlight_ice.json
 */
public class ExplorationPageInjectorTransformer extends ItemTagPageInjectorTransformer {
    public static final ResourceLocation LOAD_EXPLORATION =
            new ResourceLocation(EsotericismTinker.MODID, "load_exploration");

    public static final ExplorationPageInjectorTransformer INSTANCE =
            new ExplorationPageInjectorTransformer();

    private ExplorationPageInjectorTransformer() {
        super(
                LOAD_EXPLORATION,
                ContentShowcase.ID,
                ExplorationPageInjectorTransformer::fallback
        );
    }

    /**
     * 没有专门页面 JSON 时的 fallback。
     * 正式版你可以换成自己的 PageContent，例如 ContentExplorationItem。
     */
    private static ContentText fallback(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);

        ContentText content = new ContentText();
        content.title = item.getDescription().getString();
        content.text = new TextData[]{
                new TextData("Missing exploration book page JSON for " + id + "."),
                new TextData("Expected path: exploration/${tagname}/" + id.getNamespace() + "_" + id.getPath() + ".json")
        };

        return content;
    }
}