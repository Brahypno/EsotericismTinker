package org.brahypno.esotericismtinker.common.data.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.esotericismtinker.EsotericismTinker;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import org.brahypno.esotericismtinker.smeltery.block.component.AshenButtonBlock;
import slimeknights.mantle.client.model.builder.ColoredModelBuilder;
import slimeknights.mantle.client.model.builder.ConnectedModelBuilder;
import slimeknights.mantle.client.model.builder.MantleItemLayerBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.component.SearedBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;

import javax.annotation.Nullable;

import static net.minecraftforge.client.model.generators.ModelProvider.BLOCK_FOLDER;

public class EsotericismTinkerBlockStateProvider extends BlockStateProvider {
    private final ModelFile.UncheckedModelFile GENERATED = new ModelFile.UncheckedModelFile("item/generated");

    public EsotericismTinkerBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, EsotericismTinker.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(EsotericismTinkerSmeltery.enderMortar.get(), cubeAll(EsotericismTinkerSmeltery.enderMortar.get()));
        simpleBlockWithItem(EsotericismTinkerSmeltery.ashenBricks.get(),
                            models().cubeAll(itemKey(EsotericismTinkerSmeltery.ashenBricks.get()).getPath(), modLoc("block/transmute/ashen/ashen_bricks")));
        simpleBlockWithItem(EsotericismTinkerSmeltery.ashenRoad.get(),
                            models().cubeAll(itemKey(EsotericismTinkerSmeltery.ashenRoad.get()).getPath(), modLoc("block/transmute/ashen/ashen_road")));
        simpleBlockWithItem(EsotericismTinkerSmeltery.chiseledAshenBricks.get(),
                            models().cubeAll(itemKey(EsotericismTinkerSmeltery.chiseledAshenBricks.get()).getPath(),
                                             modLoc("block/transmute/ashen/chiseled_ashen_bricks")));
        simpleBlockWithItem(EsotericismTinkerSmeltery.ashenStone.get(),
                            models().cubeAll(itemKey(EsotericismTinkerSmeltery.ashenStone.get()).getPath(), modLoc("block/transmute/ashen/ashen_stone")));
        axisBlock(EsotericismTinkerSmeltery.polishedAshenStone.get(), "polished_ashen_stone", modLoc("block/transmute/ashen/polished_ashen_stone"), true);
        this.slabWithItem(EsotericismTinkerSmeltery.ashenBricks.getSlab(), modLoc("block/ashen_bricks"), modLoc("block/transmute/ashen/ashen_bricks"));
        slabWithItem(EsotericismTinkerSmeltery.ashenRoad.getSlab(), modLoc("block/ashen_road"), modLoc("block/transmute/ashen/ashen_road"));
        stairsWithItem(EsotericismTinkerSmeltery.ashenBricks.getStairs(), modLoc("block/transmute/ashen/ashen_bricks"));
        stairsWithItem(EsotericismTinkerSmeltery.ashenRoad.getStairs(), modLoc("block/transmute/ashen/ashen_road"));
        fenceWithItem(EsotericismTinkerSmeltery.ashenBricks.getFence(), modLoc("block/transmute/ashen/ashen_bricks"));
        controllerStates(EsotericismTinkerSmeltery.transmuteController.get(),
                         models().getExistingFile(modLoc("block/transmute/controller/transmute_unformed")),
                         models().getExistingFile(modLoc("block/transmute/controller/transmute_inactive")),
                         models().getExistingFile(modLoc("block/transmute/controller/transmute_active")));
        simpleBlockWithItem(EsotericismTinkerSmeltery.ashenLamp.get(),
                            models().cubeAll(itemKey(EsotericismTinkerSmeltery.ashenLamp.get()).getPath(), modLoc("block/transmute/ashen/ashen_lamp")));

        structureStates(EsotericismTinkerSmeltery.ashenChute.get(),
                        models().getExistingFile(modLoc("block/transmute/io/chute_inactive")),
                        models().getExistingFile(modLoc("block/transmute/io/chute_active")));
        structureStates(EsotericismTinkerSmeltery.ashenDrain.get(),
                        models().getExistingFile(modLoc("block/transmute/io/drain_inactive")),
                        models().getExistingFile(modLoc("block/transmute/io/drain_active")));
        structureStates(EsotericismTinkerSmeltery.ashenDuct.get(),
                        models().getExistingFile(modLoc("block/transmute/io/duct_inactive")),
                        models().getExistingFile(modLoc("block/transmute/io/duct_active")));

        simpleBlockWithItem(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.FUEL_TANK),
                            models().getExistingFile(modLoc("block/transmute/tank/fuel_tank")));
        simpleBlockWithItem(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.FUEL_GAUGE),
                            models().getExistingFile(modLoc("block/transmute/tank/fuel_gauge")));
        simpleBlockWithItem(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.INGOT_TANK),
                            models().getExistingFile(modLoc("block/transmute/tank/ingot_tank")));
        simpleBlockWithItem(EsotericismTinkerSmeltery.ashenTank.get(SearedTankBlock.TankType.INGOT_GAUGE),
                            models().getExistingFile(modLoc("block/transmute/tank/ingot_gauge")));

        RenderType translucent = RenderType.translucent();
        glassBlock(EsotericismTinkerSmeltery.ashenGlass.get(), EsotericismTinkerSmeltery.ashenGlassPane.get(), "transmute/glass/",
                   EsotericismTinker.getLocation("block/transmute/glass"), -1,
                   true, null);
        glassBlock(EsotericismTinkerSmeltery.ashenSoulGlass.get(), EsotericismTinkerSmeltery.ashenSoulGlassPane.get(), "transmute/soul_glass/",
                   EsotericismTinker.getLocation("block/transmute/soul_glass"), EsotericismTinker.getLocation("block/transmute/glass_top"), -1, true,
                   translucent);

        structureBlock(EsotericismTinkerSmeltery.ashenHeater.get(),
                       models().cubeAll(itemKey(EsotericismTinkerSmeltery.ashenHeater.get()).getPath(), modLoc("block/transmute/ashen/ashen_heater")),
                       models().getExistingFile(modLoc("block/transmute/ashen/ashen_heater_active")));

        structureBlock(EsotericismTinkerSmeltery.ashenAccel.get(),
                       models().cubeAll(itemKey(EsotericismTinkerSmeltery.ashenAccel.get()).getPath(), modLoc("block/transmute/ashen/ashen_accelerator")),
                       models().getExistingFile(modLoc("block/transmute/ashen/ashen_accelerator_active")));

        frontTextureIntStructureStates(EsotericismTinkerSmeltery.ashenMeltSwitch.get(), "transmute/ashen_melt_switch",
                                       AshenButtonBlock.Function_Set, 2, modLoc("block/transmute/ashen/ashen_bricks"));
    }

    protected void slabWithItem(SlabBlock slab, ResourceLocation doubleSlabModel, ResourceLocation texture) {
        // 1) blockstate + block models
        slabBlock(slab, doubleSlabModel, texture);

        // 2) item model: item/<slabname>.json -> parent: block/<slabname>
        String name = ForgeRegistries.BLOCKS.getKey(slab).getPath();
        itemModels().withExistingParent(name, modLoc("block/" + name));
    }

    protected void stairsWithItem(StairBlock stair, ResourceLocation texture) {
        // 1) blockstate + block models
        stairsBlock(stair, texture);

        // 2) item model: item/<slabname>.json -> parent: block/<slabname>
        String name = ForgeRegistries.BLOCKS.getKey(stair).getPath();
        itemModels().withExistingParent(name, modLoc("block/" + name));
    }

    protected void fenceWithItem(FenceBlock block, ResourceLocation texture) {
        // 1) blockstate + block models
        fenceBlock(block, texture);

        // 2) item model: item/<slabname>.json -> parent: block/<slabname>
        String name = ForgeRegistries.BLOCKS.getKey(block).getPath();
        ModelFile inv = models().fenceInventory(name + "_inventory", texture);
        itemModels().withExistingParent(name, modLoc("block/" + name + "_inventory"));
    }

    public void axisBlock(Block block, String location, ResourceLocation texture, boolean horizontal) {
        ResourceLocation endTexture = horizontal ? texture.withSuffix("_top") : texture;
        ModelFile model = models().cubeColumn(location, texture, endTexture);
        axisBlock(block, model, horizontal ? models().cubeColumnHorizontal(location + "_horizontal", texture, endTexture) : model);
        simpleBlockItem(block, model);
    }

    public void axisBlock(Block block, ModelFile vertical, ModelFile horizontal) {
        getVariantBuilder(block)
                .partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Y).modelForState().modelFile(vertical).addModel()
                .partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Z).modelForState().modelFile(horizontal).rotationX(90).addModel()
                .partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.X).modelForState().modelFile(horizontal).rotationX(90).rotationY(90).addModel();
    }

    private ResourceLocation itemKey(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem());
    }

    private void controllerStates(Block block, ModelFile unformed, ModelFile inactive, ModelFile active) {
        var vb = getVariantBuilder(block);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int y = ((int) dir.toYRot() + 180) % 360;
            vb.partialState().with(SearedBlock.IN_STRUCTURE, false).with(HorizontalDirectionalBlock.FACING, dir)
              .addModels(new ConfiguredModel(unformed, 0, y, false));
            vb.partialState().with(SearedBlock.IN_STRUCTURE, true).with(ControllerBlock.ACTIVE, false).with(HorizontalDirectionalBlock.FACING, dir)
              .addModels(new ConfiguredModel(inactive, 0, y, false));
            vb.partialState().with(SearedBlock.IN_STRUCTURE, true).with(ControllerBlock.ACTIVE, true).with(HorizontalDirectionalBlock.FACING, dir)
              .addModels(new ConfiguredModel(active, 0, y, false));
        }
        itemModels().withExistingParent(itemKey(block).getPath(), inactive.getLocation());
    }

    private void structureStates(Block block, ModelFile inactive, ModelFile active) {
        var vb = getVariantBuilder(block);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int y = ((int) dir.toYRot()) % 360;
            vb.partialState().with(SearedBlock.IN_STRUCTURE, false).with(HorizontalDirectionalBlock.FACING, dir)
              .addModels(new ConfiguredModel(inactive, 0, y, false));
            vb.partialState().with(SearedBlock.IN_STRUCTURE, true).with(HorizontalDirectionalBlock.FACING, dir)
              .addModels(new ConfiguredModel(active, 0, y, false));
        }
        itemModels().withExistingParent(itemKey(block).getPath(), inactive.getLocation());
    }

    private void structureBlock(Block block, ModelFile inactive, ModelFile active) {
        getVariantBuilder(block)
                .partialState().with(SearedBlock.IN_STRUCTURE, false).addModels(new ConfiguredModel(inactive))
                .partialState().with(SearedBlock.IN_STRUCTURE, true).addModels(new ConfiguredModel(active));
        itemModels().withExistingParent(itemKey(block).getPath(), inactive.getLocation());
    }

    private void frontTextureIntStructureStates(Block block, String name, net.minecraft.world.level.block.state.properties.IntegerProperty textureProperty, int maxValue, ResourceLocation sideTexture) {
        ModelFile[] builtModels = new ModelFile[maxValue + 1];
        for (int i = 0; i <= maxValue; i++) {
            builtModels[i] = models().withExistingParent(name + "_" + i, mcLoc("block/cube"))
                                     .texture("down", sideTexture).texture("up", sideTexture)
                                     .texture("north", modLoc("block/" + name + "_" + i))
                                     .texture("south", sideTexture).texture("east", sideTexture).texture("west", sideTexture)
                                     .texture("particle", sideTexture);
        }
        var vb = getVariantBuilder(block);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int y = ((int) dir.toYRot() + 180) % 360;
            for (int textureIndex : textureProperty.getPossibleValues()) {
                ModelFile model = builtModels[Math.min(textureIndex, maxValue)];
                vb.partialState().with(SearedBlock.IN_STRUCTURE, false).with(textureProperty, textureIndex).with(HorizontalDirectionalBlock.FACING, dir)
                  .addModels(new ConfiguredModel(model, 0, y, false));
                vb.partialState().with(SearedBlock.IN_STRUCTURE, true).with(textureProperty, textureIndex).with(HorizontalDirectionalBlock.FACING, dir)
                  .addModels(new ConfiguredModel(model, 0, y, false));
            }
        }
        simpleBlockItem(block, builtModels[0]);
    }

    /**
     * Adds models for a glass block with a glass pane
     */
    public void glassBlock(Block glass, IronBarsBlock pane, String baseName, ResourceLocation front, int tint, boolean solidEdge, @Nullable RenderType renderType) {
        glassBlock(glass, pane, baseName, front, front.withSuffix("_top"), tint, solidEdge, renderType);
    }

    /**
     * Adds models for a glass block with a glass pane
     */
    public void glassBlock(Block glass, IronBarsBlock pane, String baseName, ResourceLocation front, ResourceLocation edge, int tint, boolean solidEdge, @Nullable RenderType renderType) {
        // make block model
        BlockModelBuilder block = models().cubeAll(BLOCK_FOLDER + "/" + baseName + "block", front);
        ConnectedModelBuilder<BlockModelBuilder> cBuilder = block.customLoader(ConnectedModelBuilder::new);
        cBuilder.connected("all", "cornerless_full");
        if (tint != -1){
            cBuilder.color(tint);
        }
        if (renderType != null){
            block.renderType(renderType.name);
        }else {
            // glass generally wants cutout
            block.renderType(RenderType.cutout().name);
        }
        simpleBlockWithItem(glass, block);
        // make pane models
        paneBlock(pane, baseName + "pane_", front, edge, true, tint, solidEdge, renderType);
    }

    /**
     * Creates a new pane block with all relevant models
     */
    public void paneBlock(IronBarsBlock block, String baseName, ResourceLocation pane, ResourceLocation edge, boolean connected, int tint, boolean solidEdge, @Nullable RenderType renderType) {
        // build block models
        ModelFile post = paneModel(baseName, "post", pane, edge, renderType, connected, tint);
        ModelFile side = paneModel(baseName, "side", pane, edge, renderType, connected, tint);
        ModelFile sideAlt = paneModel(baseName, "side_alt", pane, edge, renderType, connected, tint);
        ModelFile noSide = paneModel(baseName, "noside", pane, null, renderType, connected, tint);
        ModelFile noSideAlt = paneModel(baseName, "noside_alt", pane, null, renderType, connected, tint);
        if (solidEdge && !pane.equals(edge)){
            ModelFile noSideEdge = paneModel(baseName, "noside_edge", pane, edge, renderType, false, tint);
            paneBlockWithEdge(block, post, side, sideAlt, noSide, noSideAlt, noSideEdge);
        }else {
            paneBlock(block, post, side, sideAlt, noSide, noSideAlt);
        }
        // build item model
        ItemModelBuilder item = itemModels().getBuilder(itemKey(block).toString()).parent(GENERATED).texture("layer0", pane);
        if (tint != -1){
            item.customLoader(MantleItemLayerBuilder::new).color(tint);
        }
        if (renderType != null){
            item.renderType(renderType.name);
        }

    }

    /**
     * Creates a new pane block state
     */
    private void paneBlockWithEdge(IronBarsBlock block, ModelFile post, ModelFile side, ModelFile sideAlt, ModelFile noSide, ModelFile noSideAlt, ModelFile noSideEdge) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block)
                .part().modelFile(post).addModel().end();
        PipeBlock.PROPERTY_BY_DIRECTION.forEach((dir, value) -> {
            if (dir.getAxis().isHorizontal()){
                boolean alt = dir == Direction.SOUTH;
                builder.part().modelFile(alt || dir == Direction.WEST ? sideAlt : side).rotationY(dir.getAxis() == Direction.Axis.X ? 90 : 0).addModel()
                       .condition(value, true).end()
                       .part().modelFile(alt || dir == Direction.EAST ? noSideAlt : noSide)
                       .rotationY(dir == Direction.WEST ? 270 : dir == Direction.SOUTH ? 90 : 0).addModel()
                       .condition(value, false).end()
                       .part().modelFile(noSideEdge).rotationY((int) dir.getOpposite().toYRot()).addModel()
                       .condition(value, false)
                       .condition(PipeBlock.PROPERTY_BY_DIRECTION.get(dir.getClockWise()), false)
                       .condition(PipeBlock.PROPERTY_BY_DIRECTION.get(dir.getCounterClockWise()), false).end();
            }
        });
    }

    /**
     * Creates a pane model using the TConstruct templates
     */
    private BlockModelBuilder paneModel(String baseName, String variant, ResourceLocation pane, @Nullable ResourceLocation edge, @Nullable RenderType renderType, boolean connected, int tint) {
        BlockModelBuilder builder =
                models().withExistingParent(BLOCK_FOLDER + "/" + baseName + variant, TConstruct.getResource("block/template/pane/" + variant));
        builder.texture("pane", pane);
        if (edge != null){
            builder.texture("edge", edge);
        }
        if (renderType != null){
            builder.renderType(renderType.name);
        }
        if (connected){
            ConnectedModelBuilder<BlockModelBuilder> cBuilder = builder.customLoader(ConnectedModelBuilder::new);
            cBuilder.connected("pane", "cornerless_full").setPredicate("pane");
            if (tint != -1){
                cBuilder.color(tint);
            }
        }else if (tint != -1){
            builder.customLoader(ColoredModelBuilder::new).color(tint);
        }
        return builder;
    }
}
