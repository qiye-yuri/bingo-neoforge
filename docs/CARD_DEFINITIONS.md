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
