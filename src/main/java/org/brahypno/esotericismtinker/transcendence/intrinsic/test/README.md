# Noumenon test commands

主手拿目标工具，副手拿来源工具。

## 基础

```mcfunction
/esotericism_tinker noumenon_test dump
/esotericism_tinker noumenon_test clear
/esotericism_tinker noumenon_test rebuild
```

## 阳性点数

```mcfunction
/esotericism_tinker noumenon_test level 3
```

点数由等级自动提供：Substrate（基质）为 `level * 2 + 1`，Elevation 为 `level²`。
Substrate 由容受和调律消耗；Elevation 由升华和授勋词条消耗。
已配置的项目作为负向消耗，`dump` 显示供给、消耗和最终剩余值。

`points` 仅用于 debug，通过增量添加或移除额外点数，不会覆盖等级公式：

```mcfunction
/esotericism_tinker noumenon_test points add 2 3
/esotericism_tinker noumenon_test points remove 2 3
```

## 容受 / 升华 / 调律

```mcfunction
/esotericism_tinker noumenon_test reception upgrades 2
/esotericism_tinker noumenon_test reception abilities 1
/esotericism_tinker noumenon_test sublimation esotericism_tinker:broad_melee_sweep 3
/esotericism_tinker noumenon_test tuning esotericism_tinker:softened_rejection 2
```

Reception 直接保存真实的 TConstruct `SlotType` 名称；不接受或迁移旧的 Reception entry id。

## 授勋

主手目标工具，副手来源工具：

```mcfunction
/esotericism_tinker noumenon_test investiture_from_offhand false
```

强制清除旧授勋并重新捕获：

```mcfunction
/esotericism_tinker noumenon_test investiture_from_offhand true
```

授勋严格失败：来源工具没有 loaded tool definition data、definition data 为空、或没有 tool definition traits 时，不写入 snapshot。
