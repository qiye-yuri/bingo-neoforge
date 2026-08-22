# Neo Bingo

[简体中文](README.zh-CN.md)

Neo Bingo is a new, independent NeoForge mod that implements a multiplayer
team bingo game mode from a public behavioral specification.

Status: the first playable server-side slice is in development. Team lobbies,
5x5 standard cards, world-save persistence, and basic commands are available.

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

## Commands

- `/neobingo join <team>` joins or changes team while the lobby is open.
- `/neobingo start [seed]` starts a standard 5x5 game (operator only).
- `/neobingo card` displays the team's card in vanilla chat.
- `/neobingo claim` checks the player's server-side inventory and claims matching objectives.
- `/neobingo end` ends the running game (operator only).

After a game ends, the first player to join creates a new lobby.

## Clean-room rule

Do not copy or mechanically translate source, resources, translations, UI
layouts, names, or documentation from Yet Another Bingo. See
`docs/CLEAN_ROOM.md` before contributing.

This project is not affiliated with or endorsed by HorrificDev. "Yet Another
Bingo" is used only to identify the compatibility target discussed in the
clean-room records.

## Roadmap

See `docs/ROADMAP.md` and `docs/FUNCTIONAL_SPEC.md`.
