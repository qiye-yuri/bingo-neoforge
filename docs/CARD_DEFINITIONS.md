# Bingo card definitions

[简体中文](CARD_DEFINITIONS.zh-CN.md)

Neo Bingo loads its default card pool from
`data/neo_bingo/bingo_cards/default.json`. A data pack can replace this file.
The definition is revalidated whenever server resources reload.

## Format version 1

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

- `schema_version` must be the integer `1`.
- `size` must be an integer from 1 through 9.
- `objectives` must contain unique, namespaced item identifiers.
- The pool must contain at least `size × size` entries.
- Every identifier must resolve to a non-air item on the server.
- Unknown fields are rejected to catch spelling mistakes early.

Reload fails rather than silently accepting an invalid definition. Existing
running games retain the card saved in their world data; the new definition is
used the next time a game starts.

The machine-readable schema is bundled at
`data/neo_bingo/bingo_cards/schema.json`. Version 0 definitions using
`card_size` are migrated to version 1 when loaded.

## Generator

Generate a definition with the Gradle task below. Quote the comma-separated
property when using PowerShell.

```text
./gradlew generateBingoCardDefinition -PbingoOutput=card.json -PbingoSize=2 "-PbingoObjectives=minecraft:stone,minecraft:dirt,minecraft:apple,minecraft:bread"
```

The task creates parent directories, validates the size and unique objective
count, and writes deterministic UTF-8 JSON in the current schema version.

## Difficulty tiers

The first 50 objectives are grouped in easy-to-impossible order with group sizes 16, 10, 8, 8, and 8. Existing start commands default to `medium`. A difficulty can be selected explicitly:

```text
/neobingo start standard difficulty easy 42
/neobingo start lockout difficulty max 42
```

The presets are `easy`, `medium`, `hard`, `extreme`, `impossible`, and `max`. `max` initially matches impossible and can be manually adjusted in `DifficultyPreset.MAX`. Its five counts must total 25 and fit their target pools.
