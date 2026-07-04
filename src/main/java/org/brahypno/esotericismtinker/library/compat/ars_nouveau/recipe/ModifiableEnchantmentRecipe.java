package org.brahypno.esotericismtinker.library.compat.ars_nouveau.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hollingsworth.arsnouveau.api.enchanting_apparatus.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.block.tile.EnchantingApparatusTile;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.library.compat.ars_nouveau.NovaRegistry;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModifiableEnchantmentRecipe extends EnchantingApparatusRecipe {
    protected static final String KEY_NOT_ENOUGH_SLOTS =
            TConstruct.makeTranslationKey("recipe", "modifier.not_enough_slots");
    protected static final String KEY_NOT_ENOUGH_SLOT =
            TConstruct.makeTranslationKey("recipe", "modifier.not_enough_slot");

    private static final Component NOT_VALID_TOOL =
            Component.translatable("recipe." + EsotericismTinker.MODID + ".modifiable_enchantment.not_valid_tool");
    private static final Component NOT_MODIFIABLE =
            Component.translatable("recipe." + EsotericismTinker.MODID + ".modifiable_enchantment.not_modifiable");

    private final Ingredient tools;
    private final ModifierId resultModifier;
    private final LevelRange level;
    @Nullable
    private final SlotType.SlotCount slots;
    private final boolean allowCrystal;
    private final boolean checkTraitLevel;

    public ModifiableEnchantmentRecipe(
            ResourceLocation id,
            Ingredient tools,
            List<Ingredient> pedestalItems,
            ModifierId result,
            LevelRange level,
            int sourceCost,
            @Nullable SlotType.SlotCount slots,
            boolean allowCrystal,
            boolean checkTraitLevel
    ) {
        super();

        this.id = id;
        this.tools = tools;
        this.pedestalItems = pedestalItems;
        this.resultModifier = result;
        this.level = level;
        this.sourceCost = sourceCost;
        this.slots = slots;
        this.allowCrystal = allowCrystal;
        this.checkTraitLevel = checkTraitLevel;
        //this.keepNbtOfReagent = true;
        //this.result = buildDisplayResult(tools, resultModifier, level.min());
    }

    public Ingredient getTools() {
        return tools;
    }

    public List<Ingredient> getPedestalIngredients() {
        return this.pedestalItems;
    }

    public ModifierId getResultModifier() {
        return resultModifier;
    }

    public ModifierId getModifierId() {
        return resultModifier;
    }

    public LevelRange getLevel() {
        return level;
    }

    public int getDisplayResultLevel() {
        return level.min();
    }

    @Nullable
    public SlotType.SlotCount getSlots() {
        return slots;
    }

    public boolean allowCrystal() {
        return allowCrystal;
    }

    public boolean checkTraitLevel() {
        return checkTraitLevel;
    }

    @Override
    public RecipeType<?> getType() {
        return NovaRegistry.MODIFIABLE_ENCHANTMENT_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return NovaRegistry.MODIFIABLE_ENCHANTMENT_SERIALIZER.get();
    }

    @Override
    public boolean isMatch(
            List<ItemStack> pedestalItems,
            ItemStack reagent,
            EnchantingApparatusTile enchantingApparatusTile,
            @Nullable Player player
    ) {
        List<ItemStack> nonEmptyPedestals = pedestalItems.stream()
                                                         .filter(stack -> !stack.isEmpty())
                                                         .collect(Collectors.toList());

        if (!checkPedestalItems(nonEmptyPedestals)){
            return false;
        }

        Component error = validateReagent(reagent);
        if (error != null){
            if (player != null){
                PortUtil.sendMessage(player, error);
            }
            return false;
        }

        return true;
    }

    public boolean doesReagentMatch(ItemStack stack, @Nullable Player player) {
        Component error = validateReagent(stack);
        if (error != null){
            if (player != null){
                PortUtil.sendMessage(player, error);
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean doesReagentMatch(ItemStack stack) {
        return validateReagent(stack) == null;
    }

    @Override
    public ItemStack getResult(
            List<ItemStack> pedestalItems,
            ItemStack reagent,
            EnchantingApparatusTile enchantingApparatusTile
    ) {
        return applyModifierToTool(reagent);
    }

    @Override
    public @NotNull ItemStack assemble(EnchantingApparatusTile inv, RegistryAccess access) {
        return applyModifierToTool(inv.getStack());
    }

    public ItemStack applyModifierToTool(ItemStack reagent) {
        ItemStack stack = reagent.copy();

        if (!(stack.getItem() instanceof IModifiable)){
            return stack;
        }

        ToolStack tool = ToolStack.from(stack);

        consumeSlots(tool);
        tool.addModifier(resultModifier, 1);
        tool.updateStack(stack);

        return stack;
    }

    private boolean checkPedestalItems(List<ItemStack> nonEmptyPedestals) {
        if (allowCrystal && matchesModifierCrystal(nonEmptyPedestals)){
            return true;
        }

        if (this.pedestalItems.size() != nonEmptyPedestals.size()){
            return false;
        }

        return doItemsMatch(nonEmptyPedestals, this.pedestalItems);
    }

    private boolean matchesModifierCrystal(List<ItemStack> nonEmptyPedestals) {
        /*
         * 这里先保留 allow_crystal 字段语义，但不默认启用具体 crystal 替代逻辑。
         * 如果你之后确定要完整接入 TConstruct modifier crystal，
         * 再在这里判断 crystal item 和 crystal 内部 modifier id。
         */
        return false;
    }

    @Nullable
    protected Component validateReagent(ItemStack stack) {
        if (stack.isEmpty()){
            return NOT_VALID_TOOL;
        }

        if (!tools.test(stack)){
            return NOT_VALID_TOOL;
        }

        if (!(stack.getItem() instanceof IModifiable)){
            return NOT_MODIFIABLE;
        }

        ToolStack tool = ToolStack.from(stack);

        Component prerequisiteError = validatePrerequisites(tool);
        if (prerequisiteError != null){
            return prerequisiteError;
        }

        ToolStack simulated = tool.copy();
        consumeSlots(simulated);
        simulated.addModifier(resultModifier, 1);

        return simulated.tryValidate();
    }

    @Nullable
    protected Component validatePrerequisites(IToolStackView tool) {
        return validatePrerequisites(tool, getNewLevel(tool));
    }

    @Nullable
    protected Component validatePrerequisites(IToolStackView tool, int newLevel) {
        Component levelError = validateLevel(newLevel);
        if (levelError != null){
            return levelError;
        }

        return checkSlots(tool, slots);
    }

    private int getNewLevel(IToolStackView tool) {
        return (checkTraitLevel ? tool.getModifiers() : tool.getUpgrades()).getLevel(resultModifier) + 1;
    }

    @Nullable
    protected Component validateLevel(int newLevel) {
        if (newLevel < level.min()){
            return Component.translatable(
                    "recipe." + EsotericismTinker.MODID + ".modifiable_enchantment.min_level",
                    level.min()
            );
        }

        if (newLevel > level.max()){
            return Component.translatable(
                    "recipe." + EsotericismTinker.MODID + ".modifiable_enchantment.max_level",
                    level.max()
            );
        }

        return null;
    }

    protected void consumeSlots(ToolStack tool) {
        if (slots != null){
            ToolDataNBT persistentData = tool.getPersistentData();
            persistentData.addSlots(slots.type(), -slots.count());
        }
    }

    @Nullable
    protected static Component checkSlots(IToolStackView tool, @Nullable SlotType.SlotCount slots) {
        if (slots != null){
            int count = slots.count();
            if (tool.getFreeSlots(slots.type()) < count){
                if (count == 1){
                    return Component.translatable(KEY_NOT_ENOUGH_SLOT, slots.type().getDisplayName());
                }
                return Component.translatable(KEY_NOT_ENOUGH_SLOTS, count, slots.type().getDisplayName());
            }
        }

        return null;
    }

    public record LevelRange(int min, int max) {
        public LevelRange {
            if (min < 1){
                throw new IllegalArgumentException("Modifier recipe level min must be at least 1");
            }
            if (max < min){
                throw new IllegalArgumentException("Modifier recipe level max must be >= min");
            }
        }

        public static LevelRange exact(int level) {
            return new LevelRange(level, level);
        }
    }

    public static class ModifiableEnchantmentRecipeSerializer implements RecipeSerializer<ModifiableEnchantmentRecipe> {
        @Override
        public @NotNull ModifiableEnchantmentRecipe fromJson(
                @NotNull ResourceLocation recipeId,
                @NotNull JsonObject json
        ) {
            Ingredient tools = Ingredient.fromJson(GsonHelper.getNonNull(json, "tools"));

            JsonArray pedestalJson = GsonHelper.getAsJsonArray(json, "pedestalItems");
            List<Ingredient> pedestalItems = new ArrayList<>();

            for (JsonElement element : pedestalJson) {
                pedestalItems.add(Ingredient.fromJson(element));
            }

            ModifierId result = new ModifierId(new ResourceLocation(GsonHelper.getAsString(json, "result")));

            LevelRange level = parseLevel(json.get("level"));

            int sourceCost = json.has("source")
                             ? GsonHelper.getAsInt(json, "source")
                             : GsonHelper.getAsInt(json, "sourceCost", 0);

            SlotType.SlotCount slots = null;
            if (json.has("slots")){
                slots = SlotType.SlotCount.LOADABLE.convert(json.get("slots"), "slots", TypedMap.EMPTY);
            }

            boolean allowCrystal = GsonHelper.getAsBoolean(json, "allow_crystal", false);
            boolean checkTraitLevel = GsonHelper.getAsBoolean(json, "check_trait_level", false);

            return new ModifiableEnchantmentRecipe(
                    recipeId,
                    tools,
                    pedestalItems,
                    result,
                    level,
                    sourceCost,
                    slots,
                    allowCrystal,
                    checkTraitLevel
            );
        }

        @Override
        public @Nullable ModifiableEnchantmentRecipe fromNetwork(
                @NotNull ResourceLocation recipeId,
                @NotNull FriendlyByteBuf buffer
        ) {
            Ingredient tools = Ingredient.fromNetwork(buffer);

            int pedestalSize = buffer.readVarInt();
            List<Ingredient> pedestalItems = new ArrayList<>();
            for (int i = 0; i < pedestalSize; i++) {
                pedestalItems.add(Ingredient.fromNetwork(buffer));
            }

            ModifierId result = new ModifierId(buffer.readResourceLocation());
            LevelRange level =
                    new LevelRange(buffer.readVarInt(), buffer.readVarInt());

            int sourceCost = buffer.readVarInt();

            SlotType.SlotCount slots = null;
            if (buffer.readBoolean()){
                SlotType type = SlotType.read(buffer);
                int count = buffer.readVarInt();
                slots = new SlotType.SlotCount(type, count);
            }

            boolean allowCrystal = buffer.readBoolean();
            boolean checkTraitLevel = buffer.readBoolean();

            return new ModifiableEnchantmentRecipe(
                    recipeId,
                    tools,
                    pedestalItems,
                    result,
                    level,
                    sourceCost,
                    slots,
                    allowCrystal,
                    checkTraitLevel
            );
        }

        @Override
        public void toNetwork(
                @NotNull FriendlyByteBuf buffer,
                @NotNull ModifiableEnchantmentRecipe recipe
        ) {
            recipe.getTools().toNetwork(buffer);

            List<Ingredient> pedestalItems = recipe.getPedestalIngredients();
            buffer.writeVarInt(pedestalItems.size());
            for (Ingredient ingredient : pedestalItems) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeResourceLocation(recipe.getResultModifier());
            buffer.writeVarInt(recipe.getLevel().min());
            buffer.writeVarInt(recipe.getLevel().max());

            buffer.writeVarInt(recipe.getSourceCost());

            SlotType.SlotCount slots = recipe.getSlots();
            buffer.writeBoolean(slots != null);
            if (slots != null){
                slots.type().write(buffer);
                buffer.writeVarInt(slots.count());
            }

            buffer.writeBoolean(recipe.allowCrystal());
            buffer.writeBoolean(recipe.checkTraitLevel());
        }

        protected static LevelRange parseLevel(JsonElement element) {
            if (element == null || element.isJsonNull()){
                return LevelRange.exact(1);
            }

            if (element.isJsonPrimitive()){
                return LevelRange.exact(element.getAsInt());
            }

            JsonObject object = element.getAsJsonObject();
            int min = GsonHelper.getAsInt(object, "min", 1);
            int max = GsonHelper.getAsInt(object, "max", min);

            return new LevelRange(min, max);
        }
    }
}