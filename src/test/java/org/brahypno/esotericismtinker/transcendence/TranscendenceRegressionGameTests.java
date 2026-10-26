package org.brahypno.esotericismtinker.transcendence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.esotericismtinker.library.modifiers.EsotericismTinkerHook;
import org.brahypno.esotericismtinker.tools.EsotericismTinkerModifiers;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataConsequence;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataData;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataEntry;
import org.brahypno.esotericismtinker.transcendence.appearance.StigmataStage;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonData;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonKeys;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonLogic;
import org.brahypno.esotericismtinker.transcendence.intrinsic.recipe.NoumenonLevelRecipe;
import org.brahypno.esotericismtinker.transcendence.table.EsotericismTinkerTranscendenceTable;
import org.brahypno.esotericismtinker.transcendence.table.block.TranscendenceAnvilBlockEntity;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.lang.reflect.Proxy;
import java.util.List;

@GameTestHolder("esotericism_tinker")
@PrefixGameTestTemplate(false)
public final class TranscendenceRegressionGameTests {
    @GameTest(template = "selenic_empty")
    public static void reloadedAnvilPreservesCommittedSublimation(GameTestHelper helper) {
        ToolStack tool = tool("pickaxe");
        NoumenonData data = NoumenonData.read(tool);
        data.level = 2;
        data.sublimations.put(NoumenonKeys.id("harvest_speed"), 1);
        data.write(tool.getPersistentData());
        tool.addModifier(EsotericismTinkerModifiers.NOUMENON_CROWN, 1);

        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var state = EsotericismTinkerTranscendenceTable.transcendenceAnvil.get().defaultBlockState();
        var original = new TranscendenceAnvilBlockEntity(pos, state);
        original.setItem(0, tool.createStack());
        var reloaded = new TranscendenceAnvilBlockEntity(pos, state);
        reloaded.load(original.saveWithoutMetadata());
        reloaded.setLevel(helper.getLevel());

        helper.assertTrue(!reloaded.adjustSublimation(NoumenonKeys.id("harvest_endurance"), 1),
                "Reload must retain the committed path lock");
        helper.assertTrue(reloaded.adjustTuning(1), "Tuning an upgraded tool should remain possible after reload");
        var result = reloaded.getResult();
        helper.assertTrue(result != null, "Tuning must produce a valid preview");
        helper.assertTrue(NoumenonData.read(result.getTool()).sublimations.equals(data.sublimations),
                "Changing tuning after reload must preserve all committed sublimations");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void crownUpgradesImmediatelyRefreshDerivedData(GameTestHelper helper) {
        ToolStack tool = tool("pickaxe");
        var recipe = new NoumenonLevelRecipe(NoumenonKeys.id("regression_crown_upgrade"), List.of(),
                Ingredient.of(tool.getItem()), 1, EsotericismTinkerModifiers.NOUMENON_CROWN,
                new IntRange(1, 4), null, false, false);
        for (int level = 1; level <= 4; level++) {
            ToolStack input = tool;
            ITinkerStationContainer container = (ITinkerStationContainer) Proxy.newProxyInstance(
                    ITinkerStationContainer.class.getClassLoader(), new Class<?>[]{ITinkerStationContainer.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getTinkerable" -> input;
                        case "getTinkerableStack" -> input.createStack();
                        default -> throw new AssertionError("Unexpected container call: " + method.getName());
                    });
            var result = recipe.getValidatedResult(container, helper.getLevel().registryAccess());
            helper.assertTrue(result.isSuccess(), "Crown upgrade should validate at level " + level);
            tool = result.getResult().getTool();
            helper.assertTrue(NoumenonData.read(tool).level == level, "Upgrade must persist its new crown level");
            helper.assertTrue(tool.getVolatileData().getInt(NoumenonKeys.REJECTION) == level * level,
                    "Derived rejection must use the new level immediately");
            helper.assertTrue(tool.getModifierLevel(EsotericismTinkerModifiers.NOUMENON_CROWN) == 1,
                    "Upgrades must retain a single crown marker");
        }
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void tuningReducesRejectionWithoutChargingItself(GameTestHelper helper) {
        ToolStack tool = tool("pickaxe");
        NoumenonData data = new NoumenonData();
        data.level = 3;
        data.receptionSlots.put("upgrades", 1);
        data.sublimations.put(NoumenonKeys.id("harvest_speed"), 1);
        data.investitureRejection = 4;
        int original = NoumenonLogic.computeRejection(tool, data);
        data.tuning = 2;
        helper.assertTrue(NoumenonLogic.computeRejection(tool, data) == original - 2,
                "Each tuning point must lower rejection by one while other allocations stay fixed");
        data.tuning = original + 1;
        helper.assertTrue(NoumenonLogic.computeRejection(tool, data) == 0, "Rejection must not become negative");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void arrowsRetainTheirConsequenceAfterSwitchingTools(GameTestHelper helper) {
        Player shooter = helper.makeMockPlayer();
        shooter.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8, 2, 8)));
        Zombie target = new Zombie(helper.getLevel());
        target.setPos(shooter.position());
        ToolStack bow = markedTool("longbow", StigmataConsequence.EXALTATION);
        ModifierEntry modifier = bow.getModifier(EsotericismTinkerModifiers.STIGMATA);
        Arrow arrow = new Arrow(helper.getLevel(), shooter);
        ModDataNBT snapshot = new ModDataNBT();
        modifier.getHook(ModifierHooks.PROJECTILE_LAUNCH)
                .onProjectileLaunch(bow, modifier, shooter, arrow, arrow, snapshot, true);
        bow.getPersistentData().remove(StigmataData.KEY);

        var hit = new EntityHitResult(target);
        modifier.getHook(ModifierHooks.PROJECTILE_HIT).onProjectileHitEntity(
                bow.getModifiers(), snapshot, modifier, arrow, hit, shooter, target, false);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(AreaEffectCloud.class,
                target.getBoundingBox().inflate(3)).isEmpty(), "Shield-blocked arrows must not trigger consequences");
        modifier.getHook(ModifierHooks.PROJECTILE_HIT).onProjectileHitEntity(
                bow.getModifiers(), snapshot, modifier, arrow, hit, shooter, target, true);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(AreaEffectCloud.class,
                target.getBoundingBox().inflate(3)).size() == 1,
                "An unblocked arrow must trigger its captured consequence without the original tool state");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void arrowDamageRespectsImprintConditions(GameTestHelper helper) {
        Player shooter = helper.makeMockPlayer();
        ToolStack bow = markedTool("longbow", StigmataConsequence.DOMINION);
        ModifierEntry modifier = bow.getModifier(EsotericismTinkerModifiers.STIGMATA);
        Arrow arrow = new Arrow(helper.getLevel(), shooter);
        ModDataNBT snapshot = new ModDataNBT();
        modifier.getHook(ModifierHooks.PROJECTILE_LAUNCH)
                .onProjectileLaunch(bow, modifier, shooter, arrow, arrow, snapshot, true);
        var hook = modifier.getHook(EsotericismTinkerHook.PROJECTILE_HURT);
        var source = shooter.damageSources().arrow(arrow, shooter);
        float unmounted = hook.modifyProjectileHurt(bow.getModifiers(), snapshot, modifier, arrow,
                source, shooter, shooter, 10);
        helper.assertTrue(unmounted < 10, "An unmounted Dominion shot must suffer its damage penalty");
        shooter.startRiding(EntityType.PIG.create(helper.getLevel()), true);
        float mounted = hook.modifyProjectileHurt(bow.getModifiers(), snapshot, modifier, arrow,
                source, shooter, shooter, 10);
        helper.assertTrue(mounted > 10, "A mounted Dominion shot must receive its conditional bonus");
        helper.assertTrue(hook.modifyProjectileHurt(ModifierNBT.EMPTY, new ModDataNBT(), modifier, arrow,
                source, shooter, shooter, 10) == 10, "An arrow without an active snapshot must retain its damage");
        helper.succeed();
    }

    @GameTest(template = "selenic_empty")
    public static void heldArmorDoesNotDuplicateWornArmorConsequences(GameTestHelper helper) {
        Zombie wearer = new Zombie(helper.getLevel());
        wearer.setNoAi(true);
        wearer.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8, 2, 8)));
        ToolStack armor = markedTool("travelers_helmet", StigmataConsequence.EXALTATION);
        wearer.setItemSlot(EquipmentSlot.HEAD, armor.createStack());
        wearer.setItemSlot(EquipmentSlot.OFFHAND, armor.createStack());
        ModifierEntry modifier = armor.getModifier(EsotericismTinkerModifiers.STIGMATA);
        modifier.getHook(ModifierHooks.EQUIPMENT_CHANGE).onEquip(armor, modifier,
                new EquipmentChangeContext(wearer, EquipmentSlot.HEAD, ItemStack.EMPTY,
                        wearer.getItemBySlot(EquipmentSlot.HEAD)));
        helper.assertTrue(!armor.isBroken() && StigmataData.read(armor).overload(armor) > 0,
                "The worn helmet must be intact and overloaded for this regression");
        modifier.getHook(ModifierHooks.ON_ATTACKED).onAttacked(armor, modifier,
                new EquipmentContext(wearer), EquipmentSlot.HEAD, wearer.damageSources().magic(), 1, false);
        int clouds = helper.getLevel().getEntitiesOfClass(AreaEffectCloud.class,
                wearer.getBoundingBox().inflate(3)).size();
        helper.assertTrue(clouds == 1,
                "Only the worn helmet may trigger; expected 1 cloud, got " + clouds);
        helper.succeed();
    }

    private static ToolStack tool(String path) {
        IModifiable item = (IModifiable) BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct", path));
        if (!item.getToolDefinition().hasMaterials()) {
            return ToolStack.createTool(item.asItem(), item.getToolDefinition(),
                    slimeknights.tconstruct.library.tools.nbt.MaterialNBT.EMPTY);
        }
        MaterialId material = new MaterialId(item.asItem() instanceof ArmorItem
                ? "tconstruct:rose_gold" : "tconstruct:wood");
        ItemStack stack = ToolBuildHandler.createSingleMaterial(item,
                MaterialVariant.of(MaterialRegistry.getMaterial(material)));
        if (stack.isEmpty()) {
            throw new AssertionError("Could not build test tool: " + path);
        }
        return ToolStack.from(stack);
    }

    private static ToolStack markedTool(String path, StigmataConsequence consequence) {
        ToolStack tool = tool(path);
        StigmataEntry entry = new StigmataEntry(new ResourceLocation("tconstruct", "small_blade"),
                new MaterialId("tconstruct:manyullyn"), new MaterialStatsId("tconstruct:head"));
        CompoundTag tag = new CompoundTag();
        tag.putInt("consequence_seed", consequence.ordinal());
        StigmataData data = StigmataData.deserialize(tag);
        for (StigmataStage stage : StigmataStage.values()) {
            data.set(stage, entry);
        }
        data.write(tool);
        tool.addModifier(EsotericismTinkerModifiers.STIGMATA, 3);
        return tool;
    }
}
