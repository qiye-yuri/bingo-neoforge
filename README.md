# Neo Bingo

[简体中文](README.zh-CN.md)

Neo Bingo is a new, independent NeoForge mod that implements a multiplayer
team bingo game mode from a public behavioral specification.

Status: foundation only. The project loads on NeoForge but gameplay is not yet
implemented.

## Target

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- Dedicated server and integrated server
- Vanilla clients supported where practical; an optional NeoForge client adds UI

## Build

```text
./gradlew build
```

The first build downloads Minecraft and NeoForge dependencies.

## Clean-room rule

Do not copy or mechanically translate source, resources, translations, UI
layouts, names, or documentation from Yet Another Bingo. See
`docs/CLEAN_ROOM.md` before contributing.

This project is not affiliated with or endorsed by HorrificDev. "Yet Another
Bingo" is used only to identify the compatibility target discussed in the
clean-room records.

## Roadmap

See `docs/ROADMAP.md` and `docs/FUNCTIONAL_SPEC.md`.
