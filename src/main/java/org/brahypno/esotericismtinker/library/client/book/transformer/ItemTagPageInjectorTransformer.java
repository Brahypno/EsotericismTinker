package org.brahypno.esotericismtinker.library.client.book.transformer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.mantle.client.book.data.content.PageContent;
import slimeknights.mantle.client.book.transformer.BookTransformer;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.RegistryHelper;

import java.util.*;
import java.util.function.Function;

/**
 * 从 item tag 自动向 Mantle book section/page 中注入页面。
 * <p>
 * 用法：
 * section/page 的 extraData 中写：
 * <p>
 * "yourmod:load_ores": {
 * "path": "pages/ores",
 * "tag": "yourmod:book/ores",
 * "prefix": "ore",
 * "sort": true
 * }
 * <p>
 * 或数组：
 * <p>
 * "yourmod:load_ores": [
 * {
 * "path": "pages/ores",
 * "tag": "yourmod:book/ores"
 * },
 * {
 * "path": "pages/gems",
 * "tag": "yourmod:book/gems"
 * }
 * ]
 * <p>
 * 对每个 tag item 生成：
 * page.name = prefix + "." + item_namespace + "." + item_path
 * page json = path + "/" + item_namespace + "_" + item_path + ".json"
 * <p>
 * 例如：
 * item = dreamtinker:moonlight_ice_ore
 * path = pages/ores
 * prefix = ore
 * <p>
 * 会尝试加载：
 * pages/ores/dreamtinker_moonlight_ice_ore.json
 * <p>
 * 若文件不存在，则使用 fallbackFactory 创建 fallback 内容。
 */
public class ItemTagPageInjectorTransformer extends BookTransformer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Comparator<PageData> PAGE_TITLE_COMPARATOR =
            Comparator.comparing(PageData::getTitle, String.CASE_INSENSITIVE_ORDER);

    /**
     * extraData 里的键，例如 esotericismtinker:load_ores
     */
    private final ResourceLocation key;

    /**
     * 默认页面类型；如果页面 JSON 内部声明了 type，会被 Mantle PageData.load() 覆盖
     */
    private final ResourceLocation pageType;

    /**
     * 页面 JSON 不存在时使用的 fallback 内容
     */
    private final Function<Item, PageContent> fallbackFactory;

    public ItemTagPageInjectorTransformer(
            ResourceLocation key,
            ResourceLocation pageType,
            Function<Item, PageContent> fallbackFactory) {
        this.key = key;
        this.pageType = pageType;
        this.fallbackFactory = fallbackFactory;
    }

    /**
     * 如果以后你想把 item tag 的读取方式换掉，可以覆写这个方法。
     */
    protected Iterator<Item> getTagEntries(TagKey<Item> tag) {
        return RegistryHelper.getTagValueStream(tag).iterator();
    }

    /**
     * 获取 item 的 registry id。
     */
    protected ResourceLocation getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    /**
     * 处理单个配置对象。
     */
    private int addPages(SectionData section, JsonElement element, String keyName, int index) {
        try {
            JsonObject load = GsonHelper.convertToJsonObject(element, keyName);

            String path = GsonHelper.getAsString(load, "path");
            boolean sort = GsonHelper.getAsBoolean(load, "sort", true);

            String prefix = "";
            if (load.has("prefix")){
                prefix = GsonHelper.getAsString(load, "prefix") + ".";
            }

            ResourceLocation tagId = JsonHelper.getResourceLocation(load, "tag");
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);

            List<PageData> newPages = new ArrayList<>();
            Iterator<Item> iterator = getTagEntries(tag);

            while (iterator.hasNext()) {
                Item item = iterator.next();
                ResourceLocation itemId = getId(item);

                PageData newPage = new PageData(true);
                newPage.parent = section;
                newPage.source = section.source;
                newPage.type = pageType;

                // 例如 ore.dreamtinker.moonlight_ice_ore
                newPage.name = prefix + itemId.getNamespace() + "." + itemId.getPath();

                // 例如 pages/ores/dreamtinker_moonlight_ice_ore.json
                String data = path + "/" + itemId.getNamespace() + "_" + itemId.getPath() + ".json";

                if (section.source.resourceExists(section.source.getResourceLocation(data))){
                    newPage.data = data;
                }else {
                    newPage.content = fallbackFactory.apply(item);
                }

                newPage.load();
                newPages.add(newPage);
            }

            if (sort){
                newPages.sort(PAGE_TITLE_COMPARATOR);
            }

            section.pages.addAll(index, newPages);
            return newPages.size();
        }
        catch (JsonParseException | IllegalStateException e) {
            LOGGER.error("Failed to parse book page injector config '{}'", keyName, e);
            return 0;
        }
    }

    /**
     * 处理 section/page 的 extraData。
     * 支持 object，也支持 array。
     */
    protected int addPages(SectionData section, Map<ResourceLocation, JsonElement> extraData, int index) {
        JsonElement element = extraData.get(key);

        if (element == null){
            return 0;
        }

        String keyName = key.toString();

        if (element.isJsonArray()){
            JsonArray array = element.getAsJsonArray();
            int added = 0;

            for (int i = 0; i < array.size(); i++) {
                added += addPages(section, array.get(i), keyName, index + added);
            }

            return added;
        }

        return addPages(section, element, keyName, index);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void transform(BookData book) {
        for (SectionData section : book.sections) {
            // 先处理 section 自己的 extraData：插到 section 最前面
            int i = addPages(section, section.extraData, 0);

            // 再处理 page 自己的 extraData：插到该 page 后面
            for (; i < section.pages.size(); i++) {
                PageData page = section.pages.get(i);
                i += addPages(section, page.extraData, i + 1);
            }
        }
    }
}
