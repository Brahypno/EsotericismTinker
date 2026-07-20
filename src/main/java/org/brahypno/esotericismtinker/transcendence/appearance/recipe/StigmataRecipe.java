package org.brahypno.esotericismtinker.transcendence.appearance.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataStage;

/** Recipe data for selecting a Stigmata target stage. */
public record StigmataRecipe(ResourceLocation id, Ingredient selector, StigmataStage targetStage) {}
