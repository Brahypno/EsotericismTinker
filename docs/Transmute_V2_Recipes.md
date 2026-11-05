# 嬗变 v2 熔炼配方

烬灰熔炼开关依次切换：嬗变 → 冶炼炉 → 铸造炉 → 嬗变 v2。
v2 优先查找 `esotericism_tinker:transmute_v2` 配方；没有匹配时回退到匠魂的 `tconstruct:melting` 配方。
其他三个模式仍查找 `tconstruct:melting`，匠魂自己的炉子也继续使用原配方。

## 数据包示例

在数据包的 `data/<namespace>/recipes/<path>.json` 中使用一个独立的配方 ID，例如
`data/example/recipes/transmute_v2/gold_to_water.json`：

```json
{
  "type": "esotericism_tinker:transmute_v2",
  "ingredient": { "item": "minecraft:gold_ingot" },
  "result": { "fluid": "minecraft:water", "amount": 90 },
  "temperature": 800,
  "time": 60
}
```

这只是配置示例，没有作为正式游戏配方加入。将 `result.fluid` 换成所需的流体 ID 即可。
同一个金锭在 v2 中产出示例中的流体，在其他模式和匠魂中仍产出熔融金。
使用独立的配方 ID；不要使用已有匠魂配方的 ID，因为数据包按 ID 覆盖配方，与配方类型无关。

## 其他模组的 Java datagen

在开发依赖中加入 EsotericismTinker、TConstruct 和 Mantle 后，可在自己的
`RecipeProvider#buildRecipes(Consumer<FinishedRecipe> consumer)` 中直接调用公开 Builder：

```java
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.brahypno.esotericismtinker.smeltery.data.TransmuteMeltingRecipeBuilder;

// 放在 buildRecipes 方法内；将 example 换成自己的模组 ID。
TransmuteMeltingRecipeBuilder.melting(
        Ingredient.of(Items.GOLD_INGOT),
        new FluidStack(Fluids.WATER, 90),
        800, 60)
    .addByproduct(new FluidStack(Fluids.LAVA, 10))
    .group("example:transmute_v2")
    .save(consumer, new ResourceLocation("example", "transmute_v2/gold_to_water"));
```

此示例生成 `data/example/recipes/transmute_v2/gold_to_water.json`，
配方 `type` 始终为 `esotericism_tinker:transmute_v2`。
示例仅用于说明调用方式，不会自动加入正式配方。

- `melting` 的输出接受 `FluidStack` 或 Mantle 的 `FluidOutput`；流体数量以 mB 为单位，保留 NBT。
- `Ingredient.of(itemTag)` 可使用物品标签作为输入。
- `addByproduct` 同样接受 `FluidStack` 或 `FluidOutput`，可多次调用，也可完全省略。
- 温度必须大于等于 0；时间必须大于 0，使用匠魂熔炼步数，不是游戏 tick。
- `group(...)` 和 `unlockedBy(...)` 沿用 Mantle Builder；不添加解锁条件也能生成配方。
- 建议始终使用 `save(consumer, id)` 明确自己的命名空间和路径。
  无 ID 的 `save(consumer)` 沿用匠魂约定，使用主产物流体的注册 ID，可能与其他配方重名。

## 整组材料形态的辅助方法

`smeltery/data/TransmuteMeltingRecipeHelper` 是匠魂 `SmelteryRecipeBuilder` 专供 v2 的版本：
同样按「流体 + 通用物品标签」批量生成整组材料形态（`metal()`、`ore()`、`gear()` 等），
区别是它只生成 `transmute_v2` 熔炼配方、不做铸造，并多了一个整体缩放产出量的比例。

```java
import org.brahypno.esotericismtinker.smeltery.data.TransmuteMeltingRecipeHelper;

// 放在 buildRecipes 方法内；iron 是材料名，同时用于物品标签与配方路径。
TransmuteMeltingRecipeHelper.fluid(consumer, "iron", EsotericismTinkerFluids.molten_ender_ash)
    .divideBy(9)   // 等价于 ratio(1, 9)：所有产出量乘以 1/9
    .metal()       // storage_blocks/iron、ingots/iron、nuggets/iron
    .gear()        // gears/iron
    .plate();      // plates/iron
```

上例中 `storage_blocks/iron` 只产出 `810 / 9 = 90 mB`，正好是一锭的量；
`ingots/iron` 产出 `90 / 9 = 10 mB`，`nuggets/iron` 产出 `10 / 9 → 1 mB`（向下取整）。
缩放后为 0 的形态直接跳过，不会生成空配方。

### 调用顺序

1. 用 `fluid(consumer, name, fluid)` 创建，`name` 可用 `String`（本模组命名空间）或 `ResourceLocation`（自定义命名空间）。
   `name` 的路径同时决定 `forge:<前缀>/<name>` 标签和配方文件路径。
2. 按需配置，配置对之后调用的形态生效：
   - `ratio(分子, 分母)`、`divideBy(n)`、`multiplyBy(n)`：产出量比例，向下取整。
   - `baseUnit(mB)`：一个「单位」的流体量，默认 `FluidValues.INGOT`（90 mB）。
   - `temperature(int)`：温度，默认取流体自身温度。
   - `time(int)`：固定熔炼步数；不设置时按匠魂公式由产出量与温度推算。
   - `optional()`：给标签配方加上「标签非空」条件，用于兼容模组。
   - `byproduct(...)`：给之后所有配方追加副产物，可多次调用。
   - `meltingFolder("path/")`：输出目录，默认 `smeltery/melting/transmute/`。
3. 调用形态方法，每个方法立刻写出配方并返回自身，可以连续链式调用。

### 各形态的默认单位数

| 方法 | 标签 | 单位数 |
| --- | --- | --- |
| `block()` | `storage_blocks/<name>` | 9 |
| `ingot()` | `ingots/<name>` | 1 |
| `nugget()` | `nuggets/<name>` | 1/9 |
| `gear()` | `gears/<name>` | 4 |
| `plate()` | `plates/<name>` | 1 |
| `rod()` | `rods/<name>` | 0.5 |
| `coin()` | `coins/<name>` | 1/3 |
| `dust()` | `dusts/<name>` | 1 |
| `sheetmetal()` | `sheetmetals/<name>` | 1 |
| `wire()` | `wires/<name>` | 0.5 |
| `ore()` / `ore(n)` | `ores/<name>` | 2（可指定） |
| `rawOre()` | `raw_materials/<name>`、`storage_blocks/raw_<name>` | 1、9 |
| `gem(n)` / `smallGem()` / `largeGem()` | `storage_blocks/<name>`、`gems/<name>` | n（4、9）、1 |

`metal()` 等于 `block()` + `ingot()` + `nugget()`，并会把单位重置为锭；`gem(n)` 会把单位设为宝石。
`gear()`、`plate()` 等非基础形态默认是 optional 的，标签为空时不会加载。

### 自定形态

```java
TransmuteMeltingRecipeHelper.fluid(consumer, "example", myFluid)
    .ratio(1, 9)
    .melting("ore_rich", 3, EsotericismTinker.forgeItemTag("ores/example"), false) // 3 单位 × 比例
    .melting("nugget", 1 / 9f, Items.GOLD_NUGGET, false);                          // 单个物品
```

- `melting(变体名, 单位数, TagKey<Item>, forceOptional)`：按标签生成。
- `melting(变体名, 单位数, Ingredient, forceOptional)`：按自定义 Ingredient 生成，无法附加标签条件。
- `melting(变体名, 单位数, ItemLike, forceOptional)`：单个物品。

配方 ID 为 `<name 的命名空间>:<meltingFolder><name 的路径>/<变体名>`，
默认即 `esotericism_tinker:smeltery/melting/transmute/iron/block` 这样的形式。

## 字段与行为

`TransmuteMeltingRecipe` 继承匠魂 `MeltingRecipe`，复用其字段校验、JSON/网络序列化与配方匹配。
`ingredient`、`result`、`temperature`、`time`、可选 `group` 和 `byproducts` 与普通匠魂熔炼配方一致。
`time` 沿用匠魂的熔炼步数单位，不是游戏 tick。
可选副产物示例：`"byproducts": [{ "fluid": "minecraft:lava", "amount": 10 }]`。
v2 对专属配方和回退配方都按原有主产物、副产物关系输出，不进行旧嬗变模式的数量交换；每槽两个物品时数量翻倍。
专属配方使用普通 `MeltingRecipe` 的字段；回退配方仍保留匠魂自身的矿石倍率、耐久缩放等行为。

v2 配方不会写入匠魂的全局 `MeltingRecipeLookup`，因此不会改变使用该查询的匠魂强化效果。
配方优先查找、加热完成和流体输出均由 `SuperByproductMeltingModuleInventory` 直接处理，v2 使用独立分支，不需要修改匠魂的槽位模块。
库存类维护每槽最近匹配的配方缓存，包含回退到匠魂配方的结果；物品的数量和 NBT 参与缓存校验，并重新验证配方是否匹配。模式切换、库存读取或大小改变时清理缓存，配方重载或客户端配方同步后自动失效。缓存只保留配方，不缓存流体结果，副产物和动态输出仍根据当前槽位计算。
进入或离开 v2 会清除已有加热进度并重新计算温度和时间；模式随物品库存一起保存。
JEI 的“嬗变 v2”分类只展示该类型的配方。
