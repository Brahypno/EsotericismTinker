package org.brahypno.esotericismtinker.plugin.JEI;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import org.brahypno.esotericismtinker.transcendence.appearance.*;
import org.brahypno.esotericismtinker.transcendence.appearance.config.StigmataConfig;
import org.brahypno.esotericismtinker.transcendence.appearance.recipe.StigmataRecipeAdapter;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import java.util.*;

/**
 * Builds a small bounded set of aligned, valid examples for JEI cycling.
 */
public final class StigmataJeiDisplayFactory {
    private static final int MAX_EXAMPLES = 24;

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
        List<ItemStack> selectors = List.of(recipe.data().selector().getItems());
        if (selectors.isEmpty() || pool.tools.isEmpty()){
            return null;
        }

        List<ItemStack> before = new ArrayList<>();
        List<ItemStack> parts = new ArrayList<>();
        List<ItemStack> material1 = new ArrayList<>();
        List<ItemStack> material2 = new ArrayList<>();
        List<ItemStack> material3 = new ArrayList<>();
        List<ItemStack> fixedSelectors = new ArrayList<>();
        List<ItemStack> after = new ArrayList<>();

        int attempts = Math.max(MAX_EXAMPLES * 8, pool.tools.size() * 4);
        for (int seed = 0; seed < attempts && before.size() < MAX_EXAMPLES; seed++) {
            DisplayRow row = buildRow(recipe.data().targetStage(), selectors, pool, seed);
            if (null == row){
                continue;
            }
            before.add(row.before);
            parts.add(row.part);
            material1.add(row.materials.get(0));
            material2.add(row.materials.get(1));
            material3.add(row.materials.get(2));
            fixedSelectors.add(row.selector);
            after.add(row.after);
        }

        if (before.isEmpty()){
            return null;
        }
        return new StigmataJeiRecipe(recipe, List.copyOf(before), List.copyOf(parts),
                                     new ModifierEntry(EsotericismTinkerModifiers.STIGMATA, recipe.data().targetStage().index()),
                                     List.copyOf(material1), List.copyOf(material2), List.copyOf(material3),
                                     List.copyOf(fixedSelectors), List.copyOf(after));
    }

    private static DisplayRow buildRow(
            StigmataStage target, List<ItemStack> selectors,
            CandidatePool pool, int seed) {
        Item item = pool.tools.get(Math.floorMod(seed, pool.tools.size()));
        if (!(item instanceof IModifiable modifiable)){
            return null;
        }

        // Match TConstruct's modifier JEI presentation: use its dedicated ui_render
        // material tool so the Stigmata and material modifiers remain prominent.
        ItemStack renderStack = ToolBuildHandler.buildToolForRendering(
                item, modifiable.getToolDefinition());
        ToolStack base = ToolStack.from(renderStack);
        base.ensureHasData();
        Set<ResourceLocation> nativeParts = nativePartIds(base);
        if (2 > nativeParts.size()){
            return null;
        }

        PartChoice manifestation = pool.choosePart(nativeParts, true, null, seed * 3 + 1);
        PartChoice alienation = pool.choosePart(nativeParts, false, null, seed * 3 + 2);
        PartChoice sealing = pool.choosePart(nativeParts, true,
                                             null == manifestation ? null : manifestation.id, seed * 3 + 3);
        if (null == manifestation || null == alienation || null == sealing){
            return null;
        }

        ToolStack before = base.copy();
        if (2 <= target.index() && !apply(before, manifestation.stack, StigmataStage.MANIFESTATION)){
            return null;
        }
        if (3 <= target.index() && !apply(before, alienation.stack, StigmataStage.ALIENATION)){
            return null;
        }

        PartChoice current = switch (target) {
            case MANIFESTATION -> manifestation;
            case ALIENATION -> alienation;
            case SEALING -> sealing;
        };
        List<ItemStack> tierMaterials = pool.chooseMaterials(current.tier, seed);
        if (3 > tierMaterials.size()){
            return null;
        }

        ToolStack after = before.copy();
        if (!apply(after, current.stack, target)){
            return null;
        }

        ItemStack selector = selectors.get(Math.floorMod(seed, selectors.size())).copy();
        selector.setCount(1);
        return new DisplayRow(before.createStack(), current.stack.copy(), tierMaterials,
                              selector, after.createStack());
    }

    private static boolean apply(ToolStack tool, ItemStack part, StigmataStage stage) {
        StigmataMutationResult result = StigmataLogic.applyTarget(tool, part, stage, RandomSource.create(0L), true);
        return result.success();
    }

    private static Set<ResourceLocation> nativePartIds(ToolStack tool) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (IToolPart part : ToolPartsHook.parts(tool.getDefinition())) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(part.asItem());
            if (null != id){
                ids.add(id);
            }
        }
        return ids;
    }

    private record DisplayRow(ItemStack before, ItemStack part, List<ItemStack> materials,
                              ItemStack selector, ItemStack after) {}

    private record PartChoice(ResourceLocation id, ItemStack stack, int tier) {}

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
            Map<Integer, List<ItemStack>> materialsByTier
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
                    Map.copyOf(materials));
        }

        PartChoice choosePart(
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

        List<ItemStack> chooseMaterials(int tier, int seed) {
            List<ItemStack> source = materialsByTier.getOrDefault(tier, List.of());
            if (source.isEmpty()){
                return List.of();
            }
            int start = Math.floorMod(seed * 3, source.size());
            return List.of(
                    source.get(start).copy(),
                    source.get((start + 1) % source.size()).copy(),
                    source.get((start + 2) % source.size()).copy());
        }
    }
}
