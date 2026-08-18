# Functional specification

This document records behavior only. It intentionally contains no upstream
implementation details.

## Core game

- An operator can create or reroll a square card from configurable objectives.
- Players can join teams in a pre-game lobby.
- Objective completion is evaluated server-side and is deterministic for a
  recorded seed and configuration.
- Standard mode awards a tile to a team when an eligible member completes it.
- Lockout mode prevents other teams from claiming an already claimed tile.
- Inventory mode continuously derives ownership from eligible inventory state.
- Hidden mode conceals objective identity until its discovery condition occurs.
- Ranked mode continues until a time limit and orders teams by score.
- A configurable line or score victory condition ends non-ranked games.

## Presentation

- The server remains authoritative.
- Vanilla clients receive a usable card through vanilla-compatible mechanisms.
- Modded clients may display a separate HUD and screen.
- All new art and text must be created independently or obtained under a
  documented compatible license.

## Operations

- Commands cover game lifecycle, seed inspection, rerolling, team membership,
  and emergency termination.
- Configuration and card definitions are data-driven and schema-versioned.
- Server state survives restart and handles disconnect/reconnect safely.

## Compatibility exclusions for v1

- No promise of upstream save-file, packet, config, translation-key, or API ABI
  compatibility.
- No upstream tier lists, lobby structures, textures, or translations are bundled.
