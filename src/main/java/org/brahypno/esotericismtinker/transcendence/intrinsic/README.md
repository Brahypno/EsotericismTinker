# Noumenon 阳性核心骨架

包路径：`org.brahypno.esotericismtinker.transcendence.intrinsic`

## 当前结构

- `NoumenonCoreModifier`：阳性核心 modifier。
- `NoumenonDatabase`：容受、升华、调律、授勋 GUI 元数据注册表。
- `NoumenonData`：读写工具 persistentData。
- `NoumenonInvestitureLogic`：从来源工具 ToolStack 捕获工具定义 traits 快照。
- `test/NoumenonTestCommands`：测试指令。

## 点数规则

- Substrate（基质）供给：`level * 2 + 1`，由 Reception 和 Tuning 共同消耗。
- Elevation 供给：`level²`，由 Sublimation 和 Investiture traits 共同消耗。
- 已配置项目记录在各自的 map 中，并作为负向点数消耗。
- 最终显示值为等级供给与负向消耗之和。
- `points add/remove` 是独立的 debug 调整量，只叠加到等级供给，不覆盖等级公式。
- `receptionSlots` 直接以真实 TConstruct `SlotType` 名称为键，例如 `upgrades`、`abilities`；不提供旧 Reception entry id
  的兼容映射。

## 授勋规则

授勋采用严格失败规则：

1. 来源必须是有效 TCon tool stack。
2. 来源 `ToolDefinition` 不能是 `ToolDefinition.EMPTY`。
3. 来源 definition 必须已经加载 tool data。
4. 来源 `ToolDefinitionData` 不能是 `ToolDefinitionData.EMPTY`。
5. 来源工具定义必须能通过 `ToolHooks.TOOL_TRAITS` 收集到至少一个 trait。

如果以上任一条件不满足，授勋选择失败，不写入任何 snapshot。

成功后写入当前工具 persistentData：

- `noumenon_invested_definition`：来源工具定义 ID。
- `noumenon_investiture_locked`：锁定。
- `noumenon_invested_traits`：来源工具定义 traits 的快照。
- `noumenon_investiture_rejection`：选择时捕获的排异。

rebuild 时，`NoumenonCoreModifier` 只读取 `noumenon_invested_traits` 并注入 trait，不动态重新解析来源工具。

## 测试方法

主手拿目标工具，副手拿来源工具：

```mcfunction
/esotericism_tinker noumenon_test investiture_from_offhand false
```

强制重选：

```mcfunction
/esotericism_tinker noumenon_test investiture_from_offhand true
```

查看数据：

```mcfunction
/esotericism_tinker noumenon_test dump
```

重建 stats：

```mcfunction
/esotericism_tinker noumenon_test rebuild
```
