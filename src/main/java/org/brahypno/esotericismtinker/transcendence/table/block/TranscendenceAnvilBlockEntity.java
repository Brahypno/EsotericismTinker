package org.brahypno.esotericismtinker.transcendence.table.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.network.PacketDistributor;
import org.brahypno.esotericismtinker.library.recipe.EsotericismTinkerRecipeTypes;
import org.brahypno.esotericismtinker.network.EsotericismTinkerNetwork;
import org.brahypno.esotericismtinker.network.UpdateTranscendenceAnvilRecipePacket;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonAllocationLogic;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonData;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonInvestitureLogic;
import org.brahypno.esotericismtinker.transcendence.table.EsotericismTinkerTranscendenceTable;
import org.brahypno.esotericismtinker.transcendence.table.menu.TranscendenceAnvilMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;
import slimeknights.tconstruct.tables.block.entity.table.RetexturedTableBlockEntity;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static slimeknights.tconstruct.library.tools.part.IMaterialItem.MATERIAL_TAG;

public final class TranscendenceAnvilBlockEntity extends RetexturedTableBlockEntity
        implements LazyResultContainer.ILazyCrafter {
    public static final int INPUT_COUNT = 5;
    private static final Component NAME =
            Component.translatable("container.esotericism_tinker.transcendence_anvil");

    private final LazyResultContainer craftingResult = new LazyResultContainer(this);
    private final TranscendenceStationContainer wrapper = new TranscendenceStationContainer(this);
    private final Map<String, Integer> initialReception = new LinkedHashMap<>();
    private final Map<String, Integer> pendingReception = new LinkedHashMap<>();
    private ItemStack pendingInvestitureSource = ItemStack.EMPTY;
    private int pendingInvestitureIndex = -1;

    @Nullable
    private ITinkerStationRecipe lastRecipe;
    @Nullable
    private Component currentError;
    @Nullable
    private LazyToolStack result;
    private ItemStack synchronizedPreview = ItemStack.EMPTY;
    private ItemStack pendingBaseTool = ItemStack.EMPTY;
    private MaterialVariantId material = IMaterial.UNKNOWN_ID;

    public TranscendenceAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(
                EsotericismTinkerTranscendenceTable.transcendenceAnvilBE.get(),
                pos,
                state,
                NAME,
                INPUT_COUNT + 1
        );
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TranscendenceAnvilMenu(id, inventory, this);
    }

    public LazyResultContainer getCraftingResult() {
        return craftingResult;
    }

    @Nullable
    public Component getCurrentError() {
        return currentError;
    }

    @Nullable
    public LazyToolStack getTool() {
        return wrapper.getTool();
    }

    @Nullable
    public LazyToolStack getResult() {
        craftingResult.getResult();
        return result;
    }

    public int getInitialReception(String slotType) {
        return initialReception.getOrDefault(slotType, 0);
    }

    public boolean hasPendingGuiChanges() {
        return !pendingReception.equals(initialReception) || pendingInvestitureIndex >= 0;
    }

    private void clearPendingForNewTool(ItemStack current) {
        pendingBaseTool = current.copy();
        resetReceptionBaseline();
        pendingInvestitureSource = ItemStack.EMPTY;
        pendingInvestitureIndex = -1;
        lastRecipe = null;
        result = null;
        synchronizedPreview = ItemStack.EMPTY;
        currentError = null;
        craftingResult.clearContent();
    }

    public int getPendingReception(String slotType) {
        return pendingReception.getOrDefault(slotType, getInitialReception(slotType));
    }

    public int getPendingInvestitureIndex() {
        return pendingInvestitureIndex;
    }

    public MaterialVariantId getMaterial() {
        return material;
    }

    public boolean adjustReception(String slotType, int delta) {
        if (level == null || level.isClientSide || (delta != 1 && delta != -1)){
            return false;
        }

        int initial = getInitialReception(slotType);
        int target = getPendingReception(slotType) + delta;
        if (target < initial){
            return false;
        }

        Map<String, Integer> candidate = new LinkedHashMap<>(pendingReception);
        if (target <= 0){
            candidate.remove(slotType);
        }else {
            candidate.put(slotType, target);
        }

        ItemStack original = getItem(0);
        if (original.isEmpty()){
            return false;
        }

        ToolStack base = ToolStack.from(original.copy());
        Component error = validateReceptionCandidate(base, candidate);
        if (error != null){
            currentError = error;
            craftingResult.clearContent();
            return false;
        }

        pendingReception.clear();
        pendingReception.putAll(candidate);
        currentError = null;

        craftingResult.clearContent();
        craftingResult.getResult();

        setChanged();
        return true;
    }

    /**
     * Selects one tool-definition trait from the player's offhand tool.
     */
    public boolean selectInvestiture(ServerPlayer player, int traitIndex) {
        if (level == null || level.isClientSide || traitIndex < 0 || getItem(0).isEmpty()){
            return false;
        }

        ItemStack sourceStack = player.getOffhandItem();
        if (sourceStack.isEmpty()){
            return false;
        }

        try {
            ToolStack source = ToolStack.from(sourceStack.copy());
            NoumenonInvestitureLogic.listDefinitionTraits(source);
        }
        catch (RuntimeException exception) {
            currentError = Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.investiture.invalid"
            );
            return false;
        }

        ItemStack oldSource = pendingInvestitureSource;
        int oldIndex = pendingInvestitureIndex;
        boolean deselecting = pendingInvestitureIndex == traitIndex
                              && ItemStack.isSameItemSameTags(sourceStack, pendingInvestitureSource);
        if (deselecting){
            pendingInvestitureSource = ItemStack.EMPTY;
            pendingInvestitureIndex = -1;
        }else {
            pendingInvestitureSource = sourceStack.copyWithCount(1);
            pendingInvestitureIndex = traitIndex;
        }

        ToolStack candidate = ToolStack.from(getItem(0).copy());
        Component error = applyPendingGuiChanges(candidate);
        if (error != null){
            pendingInvestitureSource = oldSource;
            pendingInvestitureIndex = oldIndex;
            currentError = error;
            craftingResult.clearContent();
            return false;
        }

        currentError = null;
        craftingResult.clearContent();
        craftingResult.getResult();
        setChanged();
        return true;
    }

    @Nullable
    private ToolStack getRecipePreviewTool() {
        if (result != null && !result.getStack().isEmpty()){
            return result.getTool().copy();
        }
        LazyToolStack input = getTool();
        return input == null || input.getStack().isEmpty() ? null : input.getTool().copy();
    }

    @Nullable
    private Component validateReceptionCandidate(
            ToolStack recipePreview,
            Map<String, Integer> candidate
    ) {
        return NoumenonAllocationLogic.validateAndApply(recipePreview, data -> {
            data.receptionSlots.clear();
            data.receptionSlots.putAll(candidate);
        });
    }

    private void resetReceptionBaseline() {
        initialReception.clear();
        pendingReception.clear();

        LazyToolStack tool = getTool();
        if (tool == null || tool.getStack().isEmpty()){
            return;
        }

        NoumenonData data = NoumenonData.read(tool.getTool());
        initialReception.putAll(data.receptionSlots);
        pendingReception.putAll(data.receptionSlots);
    }

    @Nullable
    private Component applyPendingGuiChanges(ToolStack preview) {
        Component receptionError = NoumenonAllocationLogic.validateAndApply(preview, data -> {
            data.receptionSlots.clear();
            data.receptionSlots.putAll(pendingReception);
        });
        if (receptionError != null){
            return receptionError;
        }

        if (pendingInvestitureIndex >= 0){
            try {
                ToolStack source = ToolStack.from(pendingInvestitureSource.copy());
                return NoumenonInvestitureLogic.captureOneTraitFromSourceTool(
                        preview, source, false, pendingInvestitureIndex
                );
            }
            catch (RuntimeException exception) {
                return Component.translatable(
                        "gui.esotericism_tinker.transcendence_anvil.investiture.selection_invalid"
                );
            }
        }
        return null;
    }

    @Nullable
    private ITinkerStationRecipe findRecipe(RecipeManager manager) {
        ITinkerStationRecipe recipe =
                manager.getAllRecipesFor(EsotericismTinkerRecipeTypes.STIGMATA_TYPE.get())
                       .stream()
                       .filter(candidate -> candidate.matches(wrapper, level))
                       .findFirst()
                       .orElse(null);

        if (recipe != null){
            return recipe;
        }

        return manager.getAllRecipesFor(TinkerRecipeTypes.TINKER_STATION.get())
                      .stream()
                      .filter(candidate -> !(candidate instanceof ToolBuildingRecipe))
                      .filter(candidate -> candidate.matches(wrapper, level))
                      .findFirst()
                      .orElse(null);
    }

    @Override
    public ItemStack calcResult(@Nullable Player player) {
        if (level == null){
            lastRecipe = null;
            result = null;
            currentError = null;
            synchronizedPreview = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }

        /*
         * 客户端不自行重算。
         *
         * 普通配方、圣痕配方、容受和授勋的最终预览，
         * 都使用服务端完成验证后同步的完整 ItemStack。
         */
        if (level.isClientSide){
            return synchronizedPreview.copy();
        }

        if (getItem(0).isEmpty()){
            return commitCalculatedState(
                    null,
                    null,
                    null,
                    ItemStack.EMPTY
            );
        }

        /*
         * 左侧 GUI 存在实际修改时，只拒绝中间配方匹配。
         *
         * 中间槽位本身仍然可以正常操作；
         * 无论中间槽内容如何变化，都不会调用 findRecipe()。
         */
        if (hasPendingGuiChanges()){
            ToolStack candidate = ToolStack.from(getItem(0).copy());
            Component guiError = applyPendingGuiChanges(candidate);

            if (guiError != null){
                return commitCalculatedState(
                        null,
                        null,
                        guiError,
                        ItemStack.EMPTY
                );
            }

            ItemStack preview = candidate.createStack();

            return commitCalculatedState(
                    null,
                    LazyToolStack.from(preview.copy()),
                    null,
                    preview
            );
        }

        /*
         * 没有左侧 pending 修改时，恢复正常配方匹配。
         *
         * findRecipe() 保留当前规则：
         * 1. 圣痕配方优先；
         * 2. 普通匠魂工作站配方其次；
         * 3. 排除 ToolBuildingRecipe。
         */
        ITinkerStationRecipe recipe = findRecipe(level.getRecipeManager());

        if (recipe == null){
            return commitCalculatedState(
                    null,
                    null,
                    null,
                    ItemStack.EMPTY
            );
        }

        RecipeResult<LazyToolStack> validated =
                recipe.getValidatedResult(wrapper, level.registryAccess());

        if (!validated.isSuccess()){
            Component error = validated.hasError()
                              ? validated.getMessage()
                              : null;

            /*
             * recipe.matches() 已成功，但配方验证失败时仍保留 recipe，
             * 以便 GUI 能正确表示当前匹配配方及验证错误。
             */
            return commitCalculatedState(
                    recipe,
                    null,
                    error,
                    ItemStack.EMPTY
            );
        }

        LazyToolStack calculatedResult = validated.getResult();
        ItemStack preview = calculatedResult.getStack().copy();

        return commitCalculatedState(
                recipe,
                calculatedResult,
                null,
                preview
        );
    }

    private ItemStack commitCalculatedState(
            @Nullable ITinkerStationRecipe recipe,
            @Nullable LazyToolStack calculatedResult,
            @Nullable Component error,
            ItemStack preview
    ) {
        ITinkerStationRecipe previousRecipe = this.lastRecipe;
        ItemStack previousPreview = this.synchronizedPreview;
        Component previousError = this.currentError;

        this.lastRecipe = recipe;
        this.result = calculatedResult;
        this.currentError = error;
        this.synchronizedPreview = preview.copy();

        boolean recipeChanged = !sameRecipe(previousRecipe, recipe);
        boolean previewChanged =
                !ItemStack.matches(previousPreview, this.synchronizedPreview);
        boolean errorChanged =
                !Objects.equals(previousError, this.currentError);

        /*
         * recipe、preview 或验证错误任一发生变化，都必须同步。
         */
        if ((recipeChanged || previewChanged || errorChanged)
            && level != null
            && !level.isClientSide){
            syncRecipeToTracking();
        }

        return this.synchronizedPreview.copy();
    }

    public void updateClientResult(
            @Nullable ITinkerStationRecipe recipe,
            ItemStack preview,
            @Nullable Component error
    ) {
        if (level == null || !level.isClientSide){
            return;
        }

        this.lastRecipe = recipe;
        this.synchronizedPreview = preview.copy();
        this.result = preview.isEmpty()
                      ? null
                      : LazyToolStack.from(preview.copy());
        this.currentError = error;

        craftingResult.clearContent();
    }

    private static boolean sameRecipe(@Nullable ITinkerStationRecipe first, @Nullable ITinkerStationRecipe second) {
        if (first == second){
            return true;
        }
        if (first == null || second == null){
            return false;
        }
        return first.getId().equals(second.getId());
    }


    public void syncRecipe(ServerPlayer player) {
        if (level == null || level.isClientSide){
            return;
        }
        /* Ensure the current recipe has been calculated before the first menu sync. */
        craftingResult.getResult();
        EsotericismTinkerNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                createRecipePacket()
        );
    }

    private void syncRecipeToTracking() {
        if (!(level instanceof ServerLevel serverLevel)){
            return;
        }
        EsotericismTinkerNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> serverLevel.getChunkAt(worldPosition)),
                createRecipePacket()
        );
    }

    private UpdateTranscendenceAnvilRecipePacket createRecipePacket() {
        return new UpdateTranscendenceAnvilRecipePacket(
                worldPosition.asLong(),
                lastRecipe == null ? null : lastRecipe.getId(),
                synchronizedPreview,
                currentError
        );
    }

    @Override
    public void onCraft(Player player, ItemStack resultItem, int amount) {
        if (amount == 0 || level == null){
            return;
        }

        if (hasPendingGuiChanges()){
            ItemStack toolStack = getItem(0);
            if (!toolStack.isEmpty()){
                toolStack.shrink(1);
            }

            clearPendingForNewTool(getItem(0));
            setChanged();
            syncRecipeToTracking();
            return;
        }

        /*
         * 必须在修改任何输入槽之前保存这些对象。
         * updateInputs() 会触发容器槽位更新，并可能使字段 lastRecipe/result 失效。
         */
        ITinkerStationRecipe recipe = this.lastRecipe;
        LazyToolStack craftedResult = this.result;

        if (recipe == null || craftedResult == null){
            return;
        }

        ItemStack tinkerable = getItem(0);

        /*
         * 必须先计算中心工具消耗量。
         * 不能放在 updateInputs() 之后。
         */
        int shrinkToolBy = tinkerable.isEmpty()
                           ? 0
                           : recipe.shrinkToolSlotBy(craftedResult, wrapper);

        wrapper.setPlayer(player);
        try {
            recipe.updateInputs(
                    craftedResult,
                    wrapper,
                    !level.isClientSide
            );
        }
        finally {
            wrapper.setPlayer(null);
        }

        if (shrinkToolBy > 0 && !tinkerable.isEmpty()){
            if (tinkerable.getCount() <= shrinkToolBy){
                setItem(0, ItemStack.EMPTY);
            }else {
                ItemStack remaining = tinkerable.copy();
                remaining.shrink(shrinkToolBy);
                setItem(0, remaining);
            }
        }

        this.lastRecipe = null;
        this.result = null;
        this.synchronizedPreview = ItemStack.EMPTY;
        this.currentError = null;
        this.craftingResult.clearContent();

        setChanged();
        syncRecipeToTracking();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);

        /*
         * 匠魂工作站的 wrapper 需要在输入变化后刷新。
         * 当前 wrapper.refresh(slot) 会在中心工具槽变化时刷新工具缓存。
         */
        wrapper.refresh(slot);

        /*
         * 中心工具发生变化时，原工具对应的容受和授勋 pending 状态
         * 不能继承到新工具。
         */
        if (slot == 0
            && (!ItemStack.isSameItemSameTags(stack, pendingBaseTool)
                || stack.getCount() != pendingBaseTool.getCount())){
            clearPendingForNewTool(stack);
        }

        /*
         * 这里只使 LazyResultContainer 缓存失效。
         * 不在 setItem() 内主动计算结果。
         */
        craftingResult.clearContent();
        setChanged();
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        return RetexturedHelper.getModelDataBuilder(texture)
                               .with(ModelProperties.MATERIAL, material)
                               .build();
    }

    @Override
    public void updateTexture(String name) {
        if (!name.isEmpty()){
            material = IMaterial.UNKNOWN_ID;
        }
        super.updateTexture(name);
    }

    public void setMaterial(MaterialVariantId material) {
        MaterialVariantId oldMaterial = this.material;
        this.material = material;
        texture = Blocks.AIR;
        if (!oldMaterial.equals(material)){
            setChangedFast();
            RetexturedHelper.onTextureUpdated(this);
        }
    }

    @Override
    public void saveSynced(CompoundTag tags) {
        super.saveSynced(tags);
        if (material != IMaterial.UNKNOWN_ID){
            tags.putString(MATERIAL_TAG, material.toString());
        }
    }

    @Override
    public void load(CompoundTag tags) {
        super.load(tags);
        if (tags.contains(MATERIAL_TAG, Tag.TAG_STRING)){
            material = Objects.requireNonNullElse(
                    MaterialVariantId.tryParse(tags.getString(MATERIAL_TAG)),
                    IMaterial.UNKNOWN_ID
            );
            RetexturedHelper.onTextureUpdated(this);
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(1.0D);
    }
}
