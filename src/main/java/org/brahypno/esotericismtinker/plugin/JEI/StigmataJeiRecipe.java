package org.brahypno.esotericismtinker.plugin.JEI;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.transcendence.appearance.recipe.StigmataRecipeAdapter;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * One JEI display recipe for one actually loaded Stigmata recipe.
 * <p>
 * Each tool carries the options that are valid for it, so the category can keep the slots around
 * the tool in step with whichever tool JEI is currently displaying instead of baking one aligned
 * row per tool. The flat lists on this record gather the same options across every tool, so recipe
 * viewers that cycle the whole recipe at once, such as the book pages, keep working.
 */
public record StigmataJeiRecipe(
    StigmataRecipeAdapter source,
    List<StigmataToolDisplay> tools,
    List<ItemStack> selectors,
    ModifierEntry modifier
) {
  public ResourceLocation id() {
    return source.getId();
  }

  /** Every part offered by any tool, without duplicates. */
  public List<ItemStack> parts() {
    return union(StigmataToolDisplay::parts);
  }

  /** Every material unit offered by any tool, without duplicates. */
  public List<ItemStack> materials() {
    return union(StigmataToolDisplay::materials);
  }

  /** Every result offered by any tool, without duplicates. */
  public List<ItemStack> results() {
    return union(StigmataToolDisplay::results);
  }

  /** Every tool this recipe applies to, one stack per tool. */
  public List<ItemStack> toolsBefore() {
    return tools.stream().map(StigmataToolDisplay::tool).toList();
  }

  /** Gets the options of the tool with the given item, or null when this recipe has no such tool. */
  public StigmataToolDisplay displayFor(ItemStack tool) {
    if (tool.isEmpty()) {
      return null;
    }

    for (StigmataToolDisplay display : tools) {
      if (display.tool().is(tool.getItem())) {
        return display;
      }
    }

    return null;
  }

  /** Gets the options of a tool that can produce the given result, or null when none can. */
  public StigmataToolDisplay displayForResult(ItemStack result) {
    if (result.isEmpty()) {
      return null;
    }

    for (StigmataToolDisplay display : tools) {
      for (ItemStack produced : display.results()) {
        if (ItemStack.isSameItemSameTags(produced, result)) {
          return display;
        }
      }
    }

    return null;
  }

  private List<ItemStack> union(Function<StigmataToolDisplay, List<ItemStack>> getter) {
    List<ItemStack> stacks = new ArrayList<>();

    for (StigmataToolDisplay display : tools) {
      for (ItemStack stack : getter.apply(display)) {
        if (stacks.stream().noneMatch(existing -> ItemStack.isSameItemSameTags(existing, stack))) {
          stacks.add(stack);
        }
      }
    }

    return List.copyOf(stacks);
  }

  /**
   * Rotates an option list so parallel slots do not all show the same entry.
   * Every slot of a recipe cycles with one shared index, so shifting the list is what keeps
   * neighbouring slots apart.
   */
  public static List<ItemStack> rotate(List<ItemStack> stacks, int offset) {
    int size = stacks.size();
    if (size < 2 || offset <= 0) {
      return stacks;
    }

    int start = Math.floorMod(offset * Math.max(1, size / 3), size);
    List<ItemStack> rotated = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      rotated.add(stacks.get((start + i) % size));
    }

    return rotated;
  }

  /**
   * Options valid for one tool.
   * {@code results} is aligned with {@code parts}, and every material can pay for the lowest tier
   * part in {@code parts}.
   */
  public record StigmataToolDisplay(
      ItemStack tool,
      List<ItemStack> parts,
      List<ItemStack> results,
      List<ItemStack> materials
  ) {}
}
