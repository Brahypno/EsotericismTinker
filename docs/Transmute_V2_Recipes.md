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
