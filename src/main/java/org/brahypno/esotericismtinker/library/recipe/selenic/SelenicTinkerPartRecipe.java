package org.brahypno.esotericismtinker.library.recipe.selenic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.library.recipe.RitualItemOutput;
import org.brahypno.esotericismtinker.utils.PartInfoLookup;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SelenicTinkerPartRecipe extends SelenicAstrolabeRecipe {
    private final MaterialVariantId materialId;
    private final List<MaterialStatsId> statIds;

    @Nullable
    private final ResourceLocation partItemId;

    private List<ItemStack> cachedDisplayOutputs;

    public SelenicTinkerPartRecipe(
            CoreData data,
            MaterialVariantId materialId,
            List<MaterialStatsId> statIds,
            @Nullable ResourceLocation partItemId) {
        super(
                data.id(),
                data.priority(),
                data.duration(),
                data.elevation(),
                data.lunarPhases(),
                data.input(),
                data.testimonies(),
                data.medium(),
                data.mediumOutput(),
                data.mediumOutputMode(),
                RitualItemOutput.EMPTY,
                data.consumeMedium());

        if (SelenicAstrolabeRecipe.isIngredientEmpty(data.input())){
            throw new IllegalArgumentException("Selenic tinker part recipe requires a crown input.");
        }

        if (statIds == null || statIds.isEmpty()){
            throw new IllegalArgumentException("Selenic tinker part recipe requires at least one stat.");
        }

        this.materialId = materialId;
        this.statIds = List.copyOf(statIds);
        this.partItemId = partItemId;
    }

    public MaterialVariantId getMaterialId() {
        return materialId;
    }

    public List<MaterialStatsId> getStatIds() {
        return statIds;
    }

    @Deprecated
    public MaterialStatsId getStatId() {
        return statIds.get(0);
    }

    @Nullable
    public ResourceLocation getPartItemId() {
        return partItemId;
    }

    @Override
    public PreparedSelenicRecipe prepareRecipe(SelenicRecipeAccess access, @Nullable RandomSource random) {
        FluidStack fluidOutput = prepareFluidOutput(access);

        if (!getMediumOutput().isEmpty() && fluidOutput.isEmpty()){
            return PreparedSelenicRecipe.fail(SelenicFailure.OUTPUT_FLUID_BLOCKED);
        }

        int maxCost = access.countCrownInput(getInput());

        PartInfoLookup.CostedPart anyPart = createRuntimePart(
                access.level(),
                maxCost,
                stack -> true,
                null);

        if (anyPart.isEmpty()){
            return PreparedSelenicRecipe.fail(SelenicFailure.MISSING_CROWN_INPUT);
        }

        PartInfoLookup.CostedPart fittingPart = createRuntimePart(
                access.level(),
                maxCost,
                stack -> access.freeItemSpace(stack) > 0,
                random);

        if (fittingPart.isEmpty()){
            return PreparedSelenicRecipe.fail(SelenicFailure.OUTPUT_ITEM_BLOCKED);
        }

        ItemStack stack = fittingPart.stack().copy();
        int amount = Math.min(stack.getCount(), access.freeItemSpace(stack));

        if (amount <= 0){
            return PreparedSelenicRecipe.fail(SelenicFailure.OUTPUT_ITEM_BLOCKED);
        }

        stack.setCount(amount);

        return PreparedSelenicRecipe.ok(stack, fluidOutput, fittingPart.cost());
    }

    /**
     * 运行时输出：maxCost 来自天球冠当前匹配 input 的物品数量。
     * accepts 用于让太阴泉槽根据当前输出内容过滤可输出物。
     */
    public PartInfoLookup.CostedPart createRuntimePart(
            Level level,
            int maxCost,
            Predicate<ItemStack> accepts,
            @Nullable RandomSource random) {
        if (partItemId != null){
            return createExactRuntimePart(level, maxCost, accepts);
        }

        for (int candidateCost = maxCost; candidateCost > 0; candidateCost--) {
            List<ToolPartItem> parts = PartInfoLookup.runtimeParts(level, materialId, statIds, candidateCost)
                                                     .stream()
                                                     .filter(part -> accepts.test(PartInfoLookup.withMaterial(part, materialId)))
                                                     .toList();

            if (parts.isEmpty()){
                continue;
            }

            ToolPartItem part = random == null
                                ? parts.get(0)
                                : parts.get(random.nextInt(parts.size()));

            return new PartInfoLookup.CostedPart(
                    PartInfoLookup.withMaterial(part, materialId),
                    candidateCost);
        }

        return PartInfoLookup.CostedPart.empty();
    }

    private PartInfoLookup.CostedPart createExactRuntimePart(
            Level level,
            int maxCost,
            Predicate<ItemStack> accepts) {
        ToolPartItem part = PartInfoLookup.exactPart(partItemId, materialId, statIds);

        if (part == null){
            return PartInfoLookup.CostedPart.empty();
        }

        PartInfoLookup.CostedPart result = PartInfoLookup.exactPartWithCost(level, materialId, part, maxCost);

        if (result.isEmpty() || !accepts.test(result.stack())){
            return PartInfoLookup.CostedPart.empty();
        }

        return result;
    }

    @Override
    public ItemStack getItemOutput() {
        List<ItemStack> outputs = getDisplayOutputs();
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).copy();
    }

    /**
     * 这个只给 vanilla fallback 用。
     * 真正执行时走 prepareRecipe(...) / createRuntimePart(...)。
     */
    @Override
    public ItemStack createItemOutput(RandomSource random) {
        return getItemOutput();
    }

    @Override
    public List<ItemStack> getDisplayOutputs() {
        if (cachedDisplayOutputs != null){
            return cachedDisplayOutputs;
        }

        List<ItemStack> outputs = new ArrayList<>();

        if (partItemId != null){
            ToolPartItem part = PartInfoLookup.exactPart(partItemId, materialId, statIds);

            if (part != null){
                outputs.add(PartInfoLookup.withMaterial(part, materialId));
            }

            cachedDisplayOutputs = List.copyOf(outputs);
            return cachedDisplayOutputs;
        }

        for (int cost = 1; cost <= 64; cost++) {
            for (ToolPartItem part : PartInfoLookup.runtimeParts(materialId, statIds, cost)) {
                outputs.add(PartInfoLookup.withMaterial(part, materialId));
            }
        }

        cachedDisplayOutputs = List.copyOf(outputs);
        return cachedDisplayOutputs;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return getItemOutput();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return getItemOutput();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EsotericismTinkerRecipeTypes.SELENIC_TINKER_PART_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return EsotericismTinkerRecipeTypes.SELENIC_ASTROLABE_TINKER_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<SelenicTinkerPartRecipe> {
        @Override
        public SelenicTinkerPartRecipe fromJson(ResourceLocation id, JsonObject json) {
            CoreData data = SelenicAstrolabeRecipe.readCore(id, json);

            if (SelenicAstrolabeRecipe.isIngredientEmpty(data.input())){
                throw new JsonSyntaxException("Selenic tinker part recipe requires non-empty 'input'.");
            }

            JsonObject part = GsonHelper.getAsJsonObject(json, "tconstruct_runtime_part");
            MaterialVariantId materialId = MaterialVariantId.parse(GsonHelper.getAsString(part, "material"));
            List<MaterialStatsId> statIds = readStats(part);
            ResourceLocation partItemId = part.has("part")
                                          ? new ResourceLocation(GsonHelper.getAsString(part, "part"))
                                          : null;

            return new SelenicTinkerPartRecipe(data, materialId, statIds, partItemId);
        }

        @Override
        public SelenicTinkerPartRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            CoreData data = SelenicAstrolabeRecipe.readCoreNetwork(id, buffer);
            MaterialVariantId materialId = MaterialVariantId.fromNetwork(buffer);

            int statCount = buffer.readVarInt();
            List<MaterialStatsId> statIds = new ArrayList<>();

            for (int i = 0; i < statCount; i++) {
                statIds.add(new MaterialStatsId(buffer.readResourceLocation()));
            }

            ResourceLocation partItemId = buffer.readBoolean()
                                          ? buffer.readResourceLocation()
                                          : null;

            return new SelenicTinkerPartRecipe(data, materialId, statIds, partItemId);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SelenicTinkerPartRecipe recipe) {
            SelenicAstrolabeRecipe.writeCoreNetwork(buffer, recipe);
            recipe.materialId.toNetwork(buffer);

            buffer.writeVarInt(recipe.statIds.size());

            for (MaterialStatsId statId : recipe.statIds) {
                buffer.writeResourceLocation(statId);
            }

            buffer.writeBoolean(recipe.partItemId != null);

            if (recipe.partItemId != null){
                buffer.writeResourceLocation(recipe.partItemId);
            }
        }

        private static List<MaterialStatsId> readStats(JsonObject json) {
            List<MaterialStatsId> stats = new ArrayList<>();

            if (json.has("stat")){
                stats.add(new MaterialStatsId(new ResourceLocation(GsonHelper.getAsString(json, "stat"))));
            }

            if (json.has("stats")){
                JsonArray array = GsonHelper.getAsJsonArray(json, "stats");

                for (JsonElement element : array) {
                    stats.add(new MaterialStatsId(new ResourceLocation(element.getAsString())));
                }
            }

            if (stats.isEmpty()){
                throw new JsonSyntaxException("Missing 'stat' or 'stats' in tconstruct_runtime_part.");
            }

            return stats.stream().distinct().toList();
        }
    }
}