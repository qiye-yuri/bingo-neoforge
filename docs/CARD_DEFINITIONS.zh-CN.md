# 宾果卡定义

[English](CARD_DEFINITIONS.md)

Neo Bingo 从 `data/neo_bingo/bingo_cards/default.json` 加载默认宾果卡目标池，数据包可以覆盖此文件。每次服务端资源重载时，模组都会重新校验该定义。

## 格式版本 1

```json
{
  "schema_version": 1,
  "size": 5,
  "objectives": [
    "minecraft:stone",
    "minecraft:iron_ingot"
  ]
}
```

- `schema_version` 必须是整数 `1`。
- `size` 必须是 1 到 9 之间的整数。
- `objectives` 必须包含互不重复且带命名空间的物品标识。
- 目标池至少需要包含 `size × size` 个条目。
- 每个标识都必须能在服务端解析为非空气物品。
- 为尽早发现拼写错误，定义中不允许出现未知字段。

定义无效时，资源重载会直接失败，不会静默采用错误配置。正在运行的游戏会继续使用世界存档中的卡片；新定义将在下一次开始游戏时生效。

机器可读的 Schema 位于 `data/neo_bingo/bingo_cards/schema.json`。加载使用 `card_size` 字段的版本 0 定义时，会将其迁移到版本 1。

## 生成器

使用以下 Gradle 任务生成定义。在 PowerShell 中使用时，请为逗号分隔的属性参数加引号。

```text
./gradlew generateBingoCardDefinition -PbingoOutput=card.json -PbingoSize=2 "-PbingoObjectives=minecraft:stone,minecraft:dirt,minecraft:apple,minecraft:bread"
```

该任务会创建父目录、校验卡片尺寸和目标唯一数量，并按照当前 Schema 版本写出确定性的 UTF-8 JSON。
