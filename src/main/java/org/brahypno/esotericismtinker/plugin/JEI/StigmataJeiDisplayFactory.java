package org.brahypno.esotericismtinker.plugin.JEI;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.plugin.JEI.StigmataJeiRecipe.StigmataToolDisplay;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import org.brahypno.esotericismtinker.transcendence.appearance.*;
import org.brahypno.esotericismtinker.transcendence.appearance.config.StigmataConfig;
import org.brahypno.esotericismtinker.transcendence.appearance.recipe.StigmataRecipeAdapter;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import java.util.*;

/**
 * Builds one display entry per tool, each carrying the options that are valid for that tool.
 */public final class StigmataJeiDisplayFactory {
    /** How many parts, and how many material units, are offered per tool. */
    private static final int MAX_PARTS_PER_TOOL = 4;
    private static final int MAX_MATERIALS_PER_TOOL = 12;
    /** How many seeds are tried before giving up on a tool. */
    private static final int MAX_ATTEMPTS_PER_TOOL = 8;

    private StigmataJeiDisplayFactory() {}

    public static List<StigmataJeiRecipe> createAll(Level level, Collection<StigmataRecipeAdapter> recipes) {
        CandidatePool pool = CandidatePool.build();
        List<StigmataJeiRecipe> displays = new ArrayList<>();
        List<StigmataRecipeAdapter> orderedRecipes = recipes.stream()
                                                            .sorted(Comparator.comparingInt(recipe -> recipe.data().targetStage().index()))
                                                            .toList();
        for (StigmataRecipeAdapter recipe : orderedRecipes) {
            StigmataJeiRecipe display = create(recipe, pool);
            if (null != display){
                displays.add(display);
            }
        }
        return List.copyOf(displays);
    }

    private static StigmataJeiRecipe create(StigmataRecipeAdapter recipe, CandidatePool pool) {
        List<ItemStack> selectors = Arrays.stream(recipe.data().selector().getItems())
                                         .map(stack -> {
                                             ItemStack copy = stack.copy();
                                             copy.setCount(1);
                                             return copy;
                                         })
                                         .toList();
        if (selectors.isEmpty() || pool.tools.isEmpty()){
            return null;
        }

        List<StigmataToolDisplay> tools = new ArrayList<>();
        for (int toolIndex = 0; toolIndex < pool.tools.size(); toolIndex++) {
            StigmataToolDisplay display = createToolDisplay(recipe.data().targetStage(), pool, toolIndex);
            if (null != display){
                tools.add(display);
            }
        }

        if (tools.isEmpty()){
            return null;
        }
        return new StigmataJeiRecipe(recipe, List.copyOf(tools), selectors,
                                     new ModifierEntry(EsotericismTinkerModifiers.STIGMATA, recipe.data().targetStage().index()));
    }

    /**
     * Collects the options that are valid for one tool at the given stage.
     * <p>
     * The input tool uses the same prior stage picks for every option, so each tool has one well
     * defined input while its parts, materials, and outputs stay exchangeable.
     */
    private static StigmataToolDisplay createToolDisplay(StigmataStage target, CandidatePool pool, int toolIndex) {
        Item item = pool.tools.get(toolIndex);
        ToolTemplate template = pool.template(item);
        if (null == template){
            return null;
        }

        Set<ResourceLocation> nativeParts = template.nativeParts();
        if (2 > nativeParts.size()){
            return null;
        }

        ToolStack base = ToolStack.from(template.renderStack().copy());
        base.ensureHasData();

        // prior stages stay stable for a given tool, matching the rules the anvil enforces
        int stableSeed = toolIndex * 31;
        ToolStack before = base.copy();
        ResourceLocation excluded = null;
        if (2 <= target.index()){
            PartChoice manifestation = pool.choosePart(nativeParts, true, null, stableSeed);
            if (null == manifestation || !apply(before, manifestation.stack, StigmataStage.MANIFESTATION)){
                return null;
            }
            excluded = manifestation.id;
        }
        if (3 <= target.index()){
            PartChoice alienation = pool.choosePart(nativeParts, false, null, stableSeed + 1);
            if (null == alienation || !apply(before, alienation.stack, StigmataStage.ALIENATION)){
                return null;
            }
        }

        // enumerate the parts this tool accepts for the target stage
        boolean requireNative = StigmataStage.ALIENATION != target;
        List<ItemStack> parts = new ArrayList<>();
        List<ItemStack> results = new ArrayList<>();
        int minTier = Integer.MAX_VALUE;
        for (int seed = 0; seed < MAX_ATTEMPTS_PER_TOOL && parts.size() < MAX_PARTS_PER_TOOL; seed++) {
            PartChoice choice = pool.choosePart(nativeParts, requireNative, excluded, seed);
            if (null == choice || containsPart(parts, choice.stack)){
                continue;
            }

            ToolStack after = before.copy();
            if (!apply(after, choice.stack, target)){
                continue;
            }

            parts.add(choice.stack.copy());
            results.add(after.createStack());
            minTier = Math.min(minTier, choice.tier);
        }

        if (parts.isEmpty()){
            return null;
        }

        List<ItemStack> materials = pool.materialOptions(minTier, MAX_MATERIALS_PER_TOOL);
        if (materials.isEmpty()){
            return null;
        }

        return new StigmataToolDisplay(before.createStack(), List.copyOf(parts),
                                       List.copyOf(results), materials);
    }

    private static boolean containsPart(List<ItemStack> parts, ItemStack stack) {
        for (ItemStack existing : parts) {
            if (ItemStack.isSameItemSameTags(existing, stack)){
                return true;
            }
        }

        return false;
    }

    private static boolean apply(ToolStack tool, ItemStack part, StigmataStage stage) {
        StigmataMutationResult result = StigmataLogic.applyTarget(tool, part, stage, RandomSource.create(0L), true);
        return result.success();
    }

    private static Set<ResourceLocation> nativePartIds(ToolDefinition definition) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (IToolPart part : ToolPartsHook.parts(definition)) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(part.asItem());
            if (null != id){
                ids.add(id);
            }
        }
        return ids;
    }

    private record PartChoice(ResourceLocation id, ItemStack stack, int tier) {}

    /**
     * Reusable render tool for one tool item. Building it is the most expensive part of a row,
     * so it is built once per tool instead of once per example.
     */
    private record ToolTemplate(ItemStack renderStack, Set<ResourceLocation> nativeParts) {}

    /**
     * Cache key for {@link CandidatePool#choosePart}. Native part sets are stored by identity
     * in practice, as each tool reuses the set built by its {@link ToolTemplate}.
     */
    private record PartKey(Set<ResourceLocation> nativeParts, boolean requireNative,
                           ResourceLocation excluded, int seed) {}

    /**
     * One JEI representative for one underlying TConstruct material identity.
     */
    private record MaterialChoice(MaterialId material, ItemStack stack, int tier,
                                  double unitsPerItem, int requiredCount, double overpay) {}

    private record CandidatePool(
            List<Item> tools,
            List<ToolPartItem> parts,
            Map<ResourceLocation, ToolPartItem> partsById,
            List<IMaterial> materials,
            Map<Integer, List<ItemStack>> materialsByTier,
            Map<Item, ToolTemplate> toolTemplates,
            Map<PartKey, Optional<PartChoice>> partChoices
    ) {

        static CandidatePool build() {
            List<Item> tools = ForgeRegistries.ITEMS.getValues().stream()
                                                    .filter(item -> item instanceof IModifiable)
                                                    .sorted(Comparator.comparing(item -> Objects.toString(ForgeRegistries.ITEMS.getKey(item))))
                                                    .toList();

            List<IMaterial> visibleMaterials = MaterialRegistry.isFullyLoaded()
                                               ? MaterialRegistry.getMaterials().stream()
                                                                 .filter(material -> !material.isHidden())
                                                                 .toList()
                                               : List.of();
            List<ToolPartItem> parts = ForgeRegistries.ITEMS.getValues().stream()
                                                          .filter(item -> item instanceof ToolPartItem)
                                                          .map(item -> (ToolPartItem) item)
                                                          .sorted(Comparator.comparing(part -> Objects.toString(
                                                                  ForgeRegistries.ITEMS.getKey(part))))
                                                          .toList();
            Map<ResourceLocation, ToolPartItem> partsById = new HashMap<>();
            for (ToolPartItem part : parts) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(part);
                if (id != null){
                    partsById.put(id, part);
                }
            }

            double requiredUnits = StigmataConfig.materialUnitsPerSlot();
            Map<MaterialId, MaterialChoice> bestByMaterial = new HashMap<>();
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                ItemStack stack = item.getDefaultInstance();
                StigmataMaterialInput resolved = StigmataMaterialResolver.resolve(stack);
                if (null == resolved || 0.0D >= resolved.unitsPerItem()){
                    continue;
                }

                int count = (int) Math.ceil((requiredUnits - 1.0E-7D) / resolved.unitsPerItem());
                if (1 > count || stack.getMaxStackSize() < count){
                    continue;
                }
                double overpay = count * resolved.unitsPerItem() - requiredUnits;
                stack.setCount(count);
                MaterialChoice candidate = new MaterialChoice(
                        resolved.material(), stack, resolved.tier(), resolved.unitsPerItem(), count, overpay);
                bestByMaterial.merge(resolved.material(), candidate, CandidatePool::betterMaterialChoice);
            }

            Map<Integer, List<ItemStack>> materials = new HashMap<>();
            bestByMaterial.values().stream()
                          .sorted(Comparator
                                          .comparingInt(MaterialChoice::tier)
                                          .thenComparing(choice -> choice.material().toString()))
                          .forEach(choice -> materials
                                  .computeIfAbsent(choice.tier(), ignored -> new ArrayList<>())
                                  .add(choice.stack().copy()));
            materials.replaceAll((tier, stacks) -> List.copyOf(stacks));
            return new CandidatePool(
                    List.copyOf(tools),
                    List.copyOf(parts),
                    Map.copyOf(partsById),
                    List.copyOf(visibleMaterials),
                    Map.copyOf(materials),
                    new HashMap<>(),
                    new HashMap<>());
        }

        /**
         * Gets the reusable render tool for the given item, building it on first use.
         * Matches TConstruct's modifier JEI presentation: use its dedicated ui_render
         * material tool so the Stigmata and material modifiers remain prominent.
         */
        ToolTemplate template(Item item) {
            return toolTemplates.computeIfAbsent(item, CandidatePool::buildTemplate);
        }

        private static ToolTemplate buildTemplate(Item item) {
            if (!(item instanceof IModifiable modifiable)){
                return null;
            }

            ToolDefinition definition = modifiable.getToolDefinition();
            ItemStack renderStack = ToolBuildHandler.buildToolForRendering(item, definition);
            return new ToolTemplate(renderStack, nativePartIds(definition));
        }

        PartChoice choosePart(
                Set<ResourceLocation> nativeParts, boolean requireNative,
                ResourceLocation excluded, int seed) {
            return partChoices.computeIfAbsent(
                    new PartKey(nativeParts, requireNative, excluded, seed),
                    key -> Optional.ofNullable(
                            selectPart(key.nativeParts(), key.requireNative(), key.excluded(), key.seed()))
            ).orElse(null);
        }

        private PartChoice selectPart(
                Set<ResourceLocation> nativeParts, boolean requireNative,
                ResourceLocation excluded, int seed) {
            List<ToolPartItem> candidates;
            if (requireNative){
                candidates = nativeParts.stream()
                                        .sorted(Comparator.comparing(ResourceLocation::toString))
                                        .filter(id -> excluded == null || !excluded.equals(id))
                                        .map(partsById::get)
                                        .filter(Objects::nonNull)
                                        .toList();
            }else {
                candidates = parts.stream()
                                  .filter(part -> {
                                      ResourceLocation id = ForgeRegistries.ITEMS.getKey(part);
                                      return id != null
                                             && !nativeParts.contains(id)
                                             && (excluded == null || !excluded.equals(id));
                                  })
                                  .toList();
            }

            if (candidates.isEmpty() || materials.isEmpty()){
                return null;
            }

            int partStart = Math.floorMod(seed, candidates.size());
            for (int partOffset = 0; partOffset < candidates.size(); partOffset++) {
                ToolPartItem part = candidates.get((partStart + partOffset) % candidates.size());
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(part);
                if (id == null){
                    continue;
                }

                int materialStart = Math.floorMod(seed * 31 + partOffset, materials.size());
                for (int materialOffset = 0; materialOffset < materials.size(); materialOffset++) {
                    IMaterial material = materials.get((materialStart + materialOffset) % materials.size());
                    MaterialVariantId variant = material.getIdentifier();
                    if (!part.canUseMaterial(variant.getId())){
                        continue;
                    }

                    ItemStack stack = part.withMaterial(variant);
                    StigmataMaterialInput resolved = StigmataMaterialResolver.resolvePart(stack);
                    if (resolved != null){
                        return new PartChoice(id, stack, resolved.tier());
                    }
                }
            }

            return null;
        }

        private static MaterialChoice betterMaterialChoice(MaterialChoice first, MaterialChoice second) {
            int overpay = Double.compare(first.overpay(), second.overpay());
            if (0 != overpay){
                return 0 > overpay ? first : second;
            }
            int count = Integer.compare(first.requiredCount(), second.requiredCount());
            if (0 != count){
                return 0 > count ? first : second;
            }
            ResourceLocation firstId = ForgeRegistries.ITEMS.getKey(first.stack().getItem());
            ResourceLocation secondId = ForgeRegistries.ITEMS.getKey(second.stack().getItem());
            String firstName = Objects.toString(firstId, "");
            String secondName = Objects.toString(secondId, "");
            return 0 >= firstName.compareTo(secondName) ? first : second;
        }

        /**
         * Material units that can pay for a part of the given tier, lowest tier first.
         * Every returned stack already carries the count the anvil asks for.
         */
        List<ItemStack> materialOptions(int minTier, int limit) {
            List<ItemStack> options = new ArrayList<>();

            for (Map.Entry<Integer, List<ItemStack>> entry : new TreeMap<>(materialsByTier).entrySet()) {
                if (entry.getKey() < minTier){
                    continue;
                }

                for (ItemStack stack : entry.getValue()) {
                    if (options.size() >= limit){
                        return List.copyOf(options);
                    }

                    options.add(stack.copy());
                }
            }

            return List.copyOf(options);
        }
    }
}
