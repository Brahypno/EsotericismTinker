package org.brahypno.esotericismtinker.tools.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.ReversedListBuilder;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMapManager;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.client.model.tools.MaterialBlockModel;
import slimeknights.tconstruct.library.client.model.tools.NestedOverrides;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/** Material-block geometry with the ritual blade's existing constant tank overlays. */
public class RitualBladeModel implements IUnbakedGeometry<RitualBladeModel> {
    public static final IGeometryLoader<RitualBladeModel> LOADER = RitualBladeModel::new;
    private static final int BLADE_PROFILE = -2;
    private static final int HANDLE_PROFILE = -3;

    private final MaterialBlockModel geometry;
    private final List<ResourceLocation> modifierMaps;
    private final float tankMaxY;

    private RitualBladeModel(JsonObject json, JsonDeserializationContext context) {
        // Carry the part identity through material retexturing. These temporary tint tags
        // are consumed when shaping the mesh and never reach the item color handler.
        JsonObject tagged = json.deepCopy();
        for (var element : tagged.getAsJsonArray("elements")) {
            for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                JsonObject data = face.getValue().getAsJsonObject();
                data.addProperty("tintindex", data.get("texture").getAsString().equals("#blade") ? BLADE_PROFILE : HANDLE_PROFILE);
            }
        }
        geometry = MaterialBlockModel.deserialize(tagged, context);
        modifierMaps = JsonHelper.parseList(json, "modifier_maps", Loadables.RESOURCE_LOCATION);
        tankMaxY = GsonHelper.getAsFloat(json, "tank_max_y", 16) / 16f;
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        geometry.resolveParents(modelGetter, context);
    }

    @Override
    public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState transform, ItemOverrides overrides, ResourceLocation location) {
        BakedModel materialModel = geometry.bake(owner, baker, spriteGetter, transform, ItemOverrides.EMPTY, location);
        List<ModifierModel> overlays = ModifierModelMapManager.INSTANCE.getModelsForTool(spriteGetter, modifierMaps).sortedConstant();
        IQuadTransformer meshTransform = QuadTransformers.applying(owner.getRootTransform());
        ItemOverrides handler = new NestedOverrides(overrides) {
            private final Cache<List<Object>, BakedModel> cache = CacheBuilder.newBuilder().maximumSize(256).build();

            @Nullable
            @Override
            public BakedModel resolve(BakedModel original, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                BakedModel resolved = super.resolve(original, stack, level, entity, seed);
                if (resolved != original) {
                    return resolved;
                }
                BakedModel materials = materialModel.getOverrides().resolve(materialModel, stack, level, entity, seed);
                ToolStack tool = ToolStack.from(stack);
                List<Object> key = new ArrayList<>();
                key.add(materials);
                for (ModifierModel overlay : overlays) {
                    key.add(overlay.getCacheKey(tool, ModifierEntry.EMPTY));
                }
                BakedModel cached = cache.getIfPresent(key);
                if (cached != null) {
                    return cached;
                }

                SimpleBakedModel.Builder builder = SimpleBlockModel.bakedBuilder(owner, ItemOverrides.EMPTY)
                                                                         .particle(materials.getParticleIcon());
                for (BakedQuad quad : materials.getQuads(null, null, RandomSource.create(seed), ModelData.EMPTY, null)) {
                    sculptPart(quad).forEach(builder::addUnculledFace);
                }
                ItemLayerPixels pixels = new ItemLayerPixels();
                ReversedListBuilder<Collection<BakedQuad>> quads = new ReversedListBuilder<>();
                for (ModifierModel overlay : overlays) {
                    overlay.addQuads(tool, ModifierEntry.EMPTY, Material::sprite, Transformation.identity(), false, 0, quads::add, pixels);
                }
                List<Collection<BakedQuad>> layers = new ArrayList<>();
                quads.build(layers::add);
                for (int layer = 0; layer < layers.size(); layer++) {
                    for (BakedQuad quad : layers.get(layer)) {
                        for (BakedQuad projected : tankFaces(quad, layer, tankMaxY)) {
                            builder.addUnculledFace(meshTransform.process(projected));
                        }
                    }
                }
                BakedModel result = builder.build(SimpleBlockModel.getRenderTypeGroup(owner));
                cache.put(key, result);
                return result;
            }
        };
        return new BakedModelWrapper<>(materialModel) {
            @Override
            public ItemOverrides getOverrides() {
                return handler;
            }

            @Override
            public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
                super.applyTransform(context, pose, leftHand);
                return this;
            }

            @Override
            public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
                return List.of(this);
            }
        };
    }

    /** Diamond blade and faceted grip, in the original 16-pixel model coordinates. */
    static float halfDepth(boolean blade, float x, float y) {
        float across = Math.abs(x - y - 1);
        if (blade) {
            float taper = Math.max(0.05f, 1 - Math.max(0, x + y - 22) / 3);
            return Math.max(0.12f, 1.8f - across * 0.82f) * taper;
        }
        return 1.55f * (float) Math.sqrt(Math.max(0.02f, 1 - across * across / 2.25f));
    }

    private record Vertex(float x, float y, float u, float v) {
        Vertex towards(Vertex other, float fraction) {
            return new Vertex(x + (other.x - x) * fraction, y + (other.y - y) * fraction,
                              u + (other.u - u) * fraction, v + (other.v - v) * fraction);
        }
    }

    /** Clip the pixel patches to continuous blade edges instead of retaining the voxel staircase. */
    static List<BakedQuad> sculptPart(BakedQuad quad) {
        if (quad.getDirection().getAxis() != Direction.Axis.Z) {
            return List.of(); // Rebuild the walls along the new outline below.
        }
        boolean blade = quad.getTintIndex() == BLADE_PROFILE;
        List<Vertex> outline = new ArrayList<>();
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * IQuadTransformer.STRIDE;
            int[] data = quad.getVertices();
            outline.add(new Vertex(Float.intBitsToFloat(data[offset]) * 16, Float.intBitsToFloat(data[offset + 1]) * 16,
                                   Float.intBitsToFloat(data[offset + 4]), Float.intBitsToFloat(data[offset + 5])));
        }
        float edge = blade ? 1 : 0.5f;
        outline = clip(outline, -1, 1, edge);
        outline = clip(outline, 1, -1, blade ? 3 : 2.5f);
        if (blade) {
            outline = clip(outline, 5, -1, 53); // Sharpen the tip along two continuous diagonals.
            outline = clip(outline, -1, 5, 47);
        }
        List<BakedQuad> result = new ArrayList<>();
        float sign = quad.getDirection() == Direction.SOUTH ? 1 : -1;
        float step = blade ? 2 : 0.5f;
        float start = blade ? -1 : -0.5f;
        float end = blade ? 3 : 2.5f;
        for (float band = start; band < end; band += step) {
            List<Vertex> patch = clip(clip(outline, -1, 1, -band), 1, -1, band + step);
            for (int vertex = 1; vertex + 1 < patch.size(); vertex++) {
                Vertex a = patch.get(0), b = patch.get(vertex), c = patch.get(vertex + 1);
                if (Math.abs((b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)) > 0.00001f) {
                    result.add(surface(quad, blade, new Vertex[]{a, b, c, c}, new float[]{sign, sign, sign, sign}));
                }
            }
        }
        if (sign > 0 && outline.size() >= 3) {
            for (int vertex = 0; vertex < outline.size(); vertex++) {
                Vertex a = outline.get(vertex), b = outline.get((vertex + 1) % outline.size());
                List<Float> splits = new ArrayList<>(List.of(0f, 1f));
                float from = a.x - a.y, to = b.x - b.y;
                for (float band = start + step; band < end; band += step) {
                    float fraction = (band - from) / (to - from);
                    if (fraction > 0.00001f && fraction < 0.99999f) {
                        splits.add(fraction);
                    }
                }
                splits.sort(Float::compare);
                for (int segment = 0; segment + 1 < splits.size(); segment++) {
                    Vertex p = a.towards(b, splits.get(segment)), q = a.towards(b, splits.get(segment + 1));
                    result.add(surface(quad, blade, new Vertex[]{p, p, q, q}, new float[]{1, -1, -1, 1}));
                }
            }
        }
        return result;
    }

    private static List<Vertex> clip(List<Vertex> polygon, float a, float b, float limit) {
        List<Vertex> result = new ArrayList<>();
        if (polygon.isEmpty()) {
            return result;
        }
        Vertex previous = polygon.get(polygon.size() - 1);
        float previousDistance = a * previous.x + b * previous.y - limit;
        for (Vertex current : polygon) {
            float distance = a * current.x + b * current.y - limit;
            if ((distance <= 0) != (previousDistance <= 0)) {
                appendVertex(result, previous.towards(current, previousDistance / (previousDistance - distance)));
            }
            if (distance <= 0) {
                appendVertex(result, current);
            }
            previous = current;
            previousDistance = distance;
        }
        if (result.size() > 1 && samePosition(result.get(0), result.get(result.size() - 1))) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    private static boolean samePosition(Vertex a, Vertex b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y) < 0.00001f;
    }

    private static void appendVertex(List<Vertex> vertices, Vertex vertex) {
        if (vertices.isEmpty() || !samePosition(vertices.get(vertices.size() - 1), vertex)) {
            vertices.add(vertex);
        }
    }

    private static BakedQuad surface(BakedQuad template, boolean blade, Vertex[] vertices, float[] signs) {
        int[] data = template.getVertices().clone();
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * IQuadTransformer.STRIDE;
            Vertex point = vertices[vertex];
            data[offset] = Float.floatToRawIntBits(point.x / 16);
            data[offset + 1] = Float.floatToRawIntBits(point.y / 16);
            data[offset + 2] = Float.floatToRawIntBits(0.5f + signs[vertex] * halfDepth(blade, point.x, point.y) / 16);
            data[offset + 4] = Float.floatToRawIntBits(point.u);
            data[offset + 5] = Float.floatToRawIntBits(point.v);
        }
        return rebuild(template, data, -1);
    }

    private static final float TANK_BODY_DEPTH = 0.6f;
    private static final float TANK_FLUID_DEPTH = 1.65f;

    /** The fluid reaches the blade opening, with exposed rims giving it visible volume. */
    static List<BakedQuad> tankFaces(BakedQuad quad, int layer, float maxY) {
        BakedQuad cap = projectTank(quad, layer, maxY);
        if (cap == null) {
            return List.of();
        }
        List<BakedQuad> result = new ArrayList<>();
        if (layer == 0) {
            result.add(cap);
        } else {
            int[] data = cap.getVertices();
            float diagonal02 = Math.abs(Float.intBitsToFloat(data[2]) - 0.5f)
                               + Math.abs(Float.intBitsToFloat(data[18]) - 0.5f);
            float diagonal13 = Math.abs(Float.intBitsToFloat(data[10]) - 0.5f)
                               + Math.abs(Float.intBitsToFloat(data[26]) - 0.5f);
            int start = diagonal13 > diagonal02 ? 1 : 0;
            result.add(capTriangle(cap, start, start + 1, start + 2));
            result.add(capTriangle(cap, start, start + 2, start + 3));
        }
        if (layer > 0 && quad.getDirection() == Direction.SOUTH) {
            for (int edge = 0; edge < 4; edge++) {
                int next = (edge + 1) % 4;
                for (int side : new int[]{-1, 1}) {
                    int[] data = cap.getVertices().clone();
                    int[] order = side > 0 ? new int[]{edge, edge, next, next} : new int[]{next, next, edge, edge};
                    for (int vertex = 0; vertex < 4; vertex++) {
                        System.arraycopy(cap.getVertices(), order[vertex] * IQuadTransformer.STRIDE,
                                         data, vertex * IQuadTransformer.STRIDE, IQuadTransformer.STRIDE);
                        float outer = Math.abs(Float.intBitsToFloat(data[vertex * IQuadTransformer.STRIDE + 2]) - 0.5f);
                        float depth = vertex == 0 || vertex == 3 ? outer : TANK_BODY_DEPTH / 16;
                        data[vertex * IQuadTransformer.STRIDE + 2] = Float.floatToRawIntBits(0.5f + side * depth);
                    }
                    result.add(rebuild(cap, data, cap.getTintIndex()));
                }
            }
        }
        return result;
    }

    private static BakedQuad capTriangle(BakedQuad cap, int a, int b, int c) {
        int[] data = cap.getVertices().clone();
        int[] order = {a, b, c, c};
        for (int vertex = 0; vertex < 4; vertex++) {
            System.arraycopy(cap.getVertices(), (order[vertex] % 4) * IQuadTransformer.STRIDE,
                             data, vertex * IQuadTransformer.STRIDE, IQuadTransformer.STRIDE);
        }
        return rebuild(cap, data, cap.getTintIndex());
    }

    @Nullable
    static BakedQuad projectTank(BakedQuad quad, int layer, float maxY) {
        // Mask baking produces overlapping side walls. Only the body owns those walls.
        if (layer > 0 && quad.getDirection().getAxis() != Direction.Axis.Z) {
            return null;
        }
        int[] data = quad.getVertices().clone();
        float low = Float.POSITIVE_INFINITY;
        float high = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            float y = Float.intBitsToFloat(data[vertex * IQuadTransformer.STRIDE + 1]);
            low = Math.min(low, y);
            high = Math.max(high, y);
        }
        if (low >= maxY && !(low == maxY && high == maxY && quad.getDirection() == Direction.UP)) {
            return null;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * IQuadTransformer.STRIDE;
            float y = Float.intBitsToFloat(data[offset + 1]);
            if (y > maxY) {
                // Clip the full-sprite body cap, interpolating UVs along its vertical edge.
                int other = ((vertex + 1) % 4) * IQuadTransformer.STRIDE;
                if (Float.intBitsToFloat(quad.getVertices()[other + 1]) == y) {
                    other = ((vertex + 3) % 4) * IQuadTransformer.STRIDE;
                }
                float otherY = Float.intBitsToFloat(quad.getVertices()[other + 1]);
                float fraction = (maxY - otherY) / (y - otherY);
                float v = Float.intBitsToFloat(data[offset + 5]);
                float otherV = Float.intBitsToFloat(quad.getVertices()[other + 5]);
                data[offset + 1] = Float.floatToRawIntBits(maxY);
                data[offset + 5] = Float.floatToRawIntBits(otherV + fraction * (v - otherV));
            }
            float z = Float.intBitsToFloat(data[offset + 2]);
            float x = Float.intBitsToFloat(data[offset]) * 16;
            float surfaceY = Float.intBitsToFloat(data[offset + 1]) * 16;
            float fluidDepth = Math.max(TANK_BODY_DEPTH + 0.06f, Math.min(TANK_FLUID_DEPTH, halfDepth(true, x, surfaceY) + 0.06f));
            float depth = (layer == 0 ? TANK_BODY_DEPTH : fluidDepth) / 16;
            data[offset + 2] = Float.floatToRawIntBits(0.5f + Math.copySign(depth, z - 0.5f));
        }
        return rebuild(quad, data, quad.getTintIndex());
    }

    private static BakedQuad rebuild(BakedQuad original, int[] vertices, int tint) {
        ForgeHooksClient.fillNormal(vertices, original.getDirection(), true);
        int normal = vertices[7];
        Direction facing = Direction.getNearest((byte) normal, (byte) (normal >> 8), (byte) (normal >> 16));
        return new BakedQuad(vertices, tint, facing, original.getSprite(), original.isShade(), original.hasAmbientOcclusion());
    }
}
