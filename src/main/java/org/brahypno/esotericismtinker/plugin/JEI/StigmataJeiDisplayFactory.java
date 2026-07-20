package org.brahypno.esotericismtinker.plugin.JEI;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataLogic;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMaterialInput;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMaterialResolver;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataMutationResult;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataStage;
import org.brahypno.esotericismtinker.transcendence.appearance.config.StigmataConfig;
import org.brahypno.esotericismtinker.transcendence.appearance.recipe.StigmataRecipeAdapter;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Builds a small bounded set of aligned, valid examples for JEI cycling. */
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
      if (display != null) {
        displays.add(display);
      }
    }
    return List.copyOf(displays);
  }

  private static StigmataJeiRecipe create(StigmataRecipeAdapter recipe, CandidatePool pool) {
    List<ItemStack> selectors = List.of(recipe.data().selector().getItems());
    if (selectors.isEmpty() || pool.tools.isEmpty()) {
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
      if (row == null) {
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

    if (before.isEmpty()) {
      return null;
    }
    return new StigmataJeiRecipe(recipe, List.copyOf(before), List.copyOf(parts),
        List.copyOf(material1), List.copyOf(material2), List.copyOf(material3),
        List.copyOf(fixedSelectors), List.copyOf(after));
  }

  private static DisplayRow buildRow(StigmataStage target, List<ItemStack> selectors,
                                     CandidatePool pool, int seed) {
    Item item = pool.tools.get(Math.floorMod(seed, pool.tools.size()));
    if (!(item instanceof IModifiable modifiable)) {
      return null;
    }

    // Match TConstruct's modifier JEI presentation: use its dedicated ui_render
    // material tool so the Stigmata and material modifiers remain prominent.
    ItemStack renderStack = ToolBuildHandler.buildToolForRendering(
        item, modifiable.getToolDefinition());
    ToolStack base = ToolStack.from(renderStack);
    base.ensureHasData();
    Set<ResourceLocation> nativeParts = nativePartIds(base);
    if (nativeParts.size() < 2) {
      return null;
    }

    PartChoice manifestation = pool.choosePart(nativeParts, true, null, seed * 3 + 1);
    PartChoice alienation = pool.choosePart(nativeParts, false, null, seed * 3 + 2);
    PartChoice sealing = pool.choosePart(nativeParts, true,
        manifestation == null ? null : manifestation.id, seed * 3 + 3);
    if (manifestation == null || alienation == null || sealing == null) {
      return null;
    }

    ToolStack before = base.copy();
    if (target.index() >= 2 && !apply(before, manifestation.stack, StigmataStage.MANIFESTATION)) {
      return null;
    }
    if (target.index() >= 3 && !apply(before, alienation.stack, StigmataStage.ALIENATION)) {
      return null;
    }

    PartChoice current = switch (target) {
      case MANIFESTATION -> manifestation;
      case ALIENATION -> alienation;
      case SEALING -> sealing;
    };
    List<ItemStack> tierMaterials = pool.chooseMaterials(current.tier, seed);
    if (tierMaterials.size() < 3) {
      return null;
    }

    ToolStack after = before.copy();
    if (!apply(after, current.stack, target)) {
      return null;
    }

    ItemStack selector = selectors.get(Math.floorMod(seed, selectors.size())).copy();
    selector.setCount(1);
    return new DisplayRow(before.createStack(), current.stack.copy(), tierMaterials,
        selector, after.createStack());
  }

  private static boolean apply(ToolStack tool, ItemStack part, StigmataStage stage) {
    StigmataMutationResult result = StigmataLogic.applyTarget(tool, part, stage);
    return result.success();
  }

  private static Set<ResourceLocation> nativePartIds(ToolStack tool) {
    Set<ResourceLocation> ids = new HashSet<>();
    for (IToolPart part : ToolPartsHook.parts(tool.getDefinition())) {
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(part.asItem());
      if (id != null) {
        ids.add(id);
      }
    }
    return ids;
  }

  private record DisplayRow(ItemStack before, ItemStack part, List<ItemStack> materials,
                            ItemStack selector, ItemStack after) {}

  private record PartChoice(ResourceLocation id, ItemStack stack, int tier) {}

  /** One JEI representative for one underlying TConstruct material identity. */
  private record MaterialChoice(MaterialId material, ItemStack stack, int tier,
                                double unitsPerItem, int requiredCount, double overpay) {}

  private static final class CandidatePool {
    private final List<Item> tools;
    private final List<PartChoice> parts;
    private final Map<Integer, List<ItemStack>> materialsByTier;

    private CandidatePool(List<Item> tools, List<PartChoice> parts,
                          Map<Integer, List<ItemStack>> materialsByTier) {
      this.tools = tools;
      this.parts = parts;
      this.materialsByTier = materialsByTier;
    }

    static CandidatePool build() {
      List<Item> tools = ForgeRegistries.ITEMS.getValues().stream()
          .filter(item -> item instanceof IModifiable)
          .sorted(Comparator.comparing(item -> Objects.toString(ForgeRegistries.ITEMS.getKey(item))))
          .toList();

      List<PartChoice> parts = new ArrayList<>();
      if (MaterialRegistry.isFullyLoaded()) {
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
          if (!(item instanceof ToolPartItem part)) {
            continue;
          }
          ResourceLocation partId = ForgeRegistries.ITEMS.getKey(item);
          if (partId == null) {
            continue;
          }
          for (IMaterial material : MaterialRegistry.getMaterials()) {
            MaterialVariantId variant = material.getIdentifier();
            if (!material.isHidden() && part.canUseMaterial(variant.getId())) {
              ItemStack stack = part.withMaterial(variant);
              StigmataMaterialInput resolved = StigmataMaterialResolver.resolvePart(stack);
              if (resolved != null) {
                parts.add(new PartChoice(partId, stack, resolved.tier()));
              }
            }
          }
        }
      }

      double requiredUnits = StigmataConfig.materialUnitsPerSlot();
      Map<MaterialId, MaterialChoice> bestByMaterial = new HashMap<>();
      for (Item item : ForgeRegistries.ITEMS.getValues()) {
        ItemStack stack = item.getDefaultInstance();
        StigmataMaterialInput resolved = StigmataMaterialResolver.resolve(stack);
        if (resolved == null || resolved.unitsPerItem() <= 0.0D) {
          continue;
        }

        int count = (int)Math.ceil((requiredUnits - 1.0E-7D) / resolved.unitsPerItem());
        if (count < 1 || count > stack.getMaxStackSize()) {
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
      return new CandidatePool(List.copyOf(tools), List.copyOf(parts), Map.copyOf(materials));
    }

    PartChoice choosePart(Set<ResourceLocation> nativeParts, boolean requireNative,
                          ResourceLocation excluded, int seed) {
      List<PartChoice> matching = parts.stream()
          .filter(choice -> nativeParts.contains(choice.id) == requireNative)
          .filter(choice -> excluded == null || !excluded.equals(choice.id))
          .toList();
      return matching.isEmpty() ? null : matching.get(Math.floorMod(seed, matching.size()));
    }

    private static MaterialChoice betterMaterialChoice(MaterialChoice first, MaterialChoice second) {
      int overpay = Double.compare(first.overpay(), second.overpay());
      if (overpay != 0) {
        return overpay < 0 ? first : second;
      }
      int count = Integer.compare(first.requiredCount(), second.requiredCount());
      if (count != 0) {
        return count < 0 ? first : second;
      }
      ResourceLocation firstId = ForgeRegistries.ITEMS.getKey(first.stack().getItem());
      ResourceLocation secondId = ForgeRegistries.ITEMS.getKey(second.stack().getItem());
      String firstName = Objects.toString(firstId, "");
      String secondName = Objects.toString(secondId, "");
      return firstName.compareTo(secondName) <= 0 ? first : second;
    }

    List<ItemStack> chooseMaterials(int tier, int seed) {
      List<ItemStack> source = materialsByTier.getOrDefault(tier, List.of());
      if (source.isEmpty()) {
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
