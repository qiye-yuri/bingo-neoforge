# Neo Bingo

[简体中文](README.zh-CN.md)

Neo Bingo is a new, independent NeoForge mod that implements a multiplayer
team bingo game mode from a public behavioral specification.

Status: the first playable server-side slice is in development. Team lobbies,
5x5 standard, lockout, and hidden cards, world-save persistence, and basic commands are available.

## Target

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- Dedicated server and integrated server
- Vanilla clients supported where practical; an optional NeoForge client adds UI
- English and Simplified Chinese command feedback

Enhanced NeoForge clients use the optional versioned `neo_bingo:protocol_version`
channel. Servers do not require this channel, so vanilla clients remain supported.

The enhanced client shows a compact 5x5 icon card in the top-right corner. Press `B` for a full team-colored card with completion, hidden-tile, item-icon, and hover states; press `H` to toggle the HUD. The HUD defaults are tuned for 1920x1080 and follow GUI scaling. Press `P` to cycle through all four corners, or `[` / `]` to resize it from 50% to 200% in 10% steps. Client settings are persisted.

Both the HUD and full card show the current team score. Ranked mode also shows server-synchronized remaining time, refreshed once per second.

## Installation and first game

1. Install NeoForge 21.1.248 for Minecraft 1.21.1 and run the server with Java 21.
2. Copy `build/libs/neo_bingo-<version>.jar` into the server's `mods` directory.
3. Clients may play without the mod through chat. For the HUD, full card, and item icons, copy the same JAR into the client's `mods` directory.
4. Start the server and have players run `/neobingo join <team>`.
5. A player who joined a team, or an operator, can run `/neobingo start` for standard mode or select another mode below.

On login, each player receives a vanilla interactive Bingo settings book for choosing a team, game mode, and MAX-to-D difficulty. It also includes random teams and a clickable chat-based administrator panel for assigning or removing online players, rerolling the card, and ending a game. Team assignments color player names in the vanilla Tab list. Existing books are not duplicated, outdated copies are upgraded, and reconnecting or running `/neobingo book` replaces a lost copy.

The book now places all six exact tier counts on one page. Each click changes one item without leaving the current book page; when the total is 25, the upcoming card can be generated, refreshed, and started from the lobby. Refreshing remains unavailable after a game starts, and the separate full-card screen and `/neobingo card` command have been removed.

Its match-options page also adjusts timed-mode duration in minutes and team respawn spacing in chunks. Team spawn and death-respawn points use a grid centered on `(0,0)`. Matches use the server's continuously loaded vanilla dimensions for full obsidian-portal compatibility.

Additional book toggles enable night vision for everyone, per-match keep-inventory behavior, and a separate shared 54-slot chest for each team. The starter-kit page adjusts bread, logs, cobblestone, torches, iron, and cooked beef distributed to every participant at match start. A five-second portal fallback performs the verified match-local Nether transition if vanilla portal timing does not fire.

Starting does not restart the server. Participants enter the match region centered on `(0,0)` and use the server's vanilla Overworld, Nether, and End. Ending, winning, or reaching the ranked timeout returns participants to the lobby.

A successful server load logs `Neo Bingo initialized` and the loaded 5×5 Bingo card definition.

## Build

### One-click Windows build

After installing Java 21, double-click [`构建模组.bat`](构建模组.bat) in the repository root. It runs the full build and unit tests, verifies the NeoForge metadata, then copies the installable JAR and its SHA-256 file to `dist`.

Install `dist/neo_bingo-<version>.jar` into the `mods` directory of a Minecraft 1.21.1 instance running NeoForge 21.1.248. The first build downloads dependencies; later builds reuse the Gradle cache.

The same helper can be run from PowerShell:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-jar.ps1
```

### Regular build

```text
./gradlew build
```

The first build downloads Minecraft and NeoForge dependencies.

Run loader-aware integration tests with:

```text
./gradlew runGameTestServer
```

## Commands

- `/neobingo join <team>` joins or changes team while the lobby is open.
- `/neobingo leave` leaves the current team while the lobby is open.
- `/neobingo book` gives a replacement interactive Bingo settings book.
- `/neobingo randomteams [count]` randomly and evenly assigns joined lobby players across 2 to 8 teams (default 2; operator only).
- `/neobingo team assign <player> <team>` assigns an online player to a team during the lobby (operator only).
- `/neobingo team remove <player>` removes an online player from the Bingo lobby (operator only).
- `/neobingo start [seed]` starts a standard 5x5 game for a joined player or operator.
- `/neobingo start <standard|lockout|hidden> [seed]` starts an explicitly selected mode.
- `/neobingo start ranked <seconds> [seed]` starts a timed score-ranked game.
- `/neobingo reroll [seed]` prepares or refreshes the next card in the lobby; rerolling is forbidden after the game starts (operator only).
- `/neobingo reroll mix <MAX> <S> <A> <B> <C> <D>` refreshes with exact per-tier counts; all six counts must total 25.
- `/neobingo start <mode> mix <MAX> <S> <A> <B> <C> <D>` starts with exact per-tier item counts totaling 25.
- `/neobingo claim` immediately checks the player's server-side inventory and claims matching objectives.
- `/neobingo status` displays the current lifecycle, mode, seed, teams, and winner.
- `/neobingo end` ends the running game (operator only).

After a game ends, the first player to join creates a new lobby.
During a running game, the server also checks online team members' inventories
once per second and automatically claims new matching objectives.

## Roadmap

See `docs/ROADMAP.md` and `docs/FUNCTIONAL_SPEC.md`.

Data-pack authors should also see `docs/CARD_DEFINITIONS.md`.
