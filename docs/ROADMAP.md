# Roadmap

[简体中文](ROADMAP.zh-CN.md)

## Phase 0 — foundation

- NeoForge project, CI, licensing, clean-room policy, and architecture records.
- Minimal load test on client and dedicated server.

## Phase 1 — vertical slice

- [x] Loader-independent card generation, team identifiers, standard claims,
  lockout claims, scoring, and line victory rules.
- [x] Unit-test coverage for deterministic generation and the initial rule set.
- [x] Execute the Gradle test suite with Java 21 and Gradle 9.2.1.
- [x] Team membership service and lobby/running/finished session lifecycle.
- [x] Automatic line-victory completion and operator-triggered game termination.
- [x] Immutable snapshots for cards, claims, rosters, lifecycle state, seeds, and winners.
- [x] Validated in-memory restart recovery for running and finished sessions.
- [x] NeoForge world-save storage adapter for 5x5 item-objective sessions.
- [x] `/neobingo start`, `join`, `card`, and `end` commands.
- [x] Standard mode and one independently designed vanilla-compatible card view.
- [x] Server-authoritative inventory checks for manually claiming item objectives.
- [x] GameTests for claim rules, victory, world-save recovery, and reconnect identity.

## Phase 2 — modes and configuration

- [x] Versioned and strictly validated data-pack definition for the default card pool.
- [x] Playable standard and lockout modes selectable from server commands.
- [x] Lobby leave, seed inspection, and operator card-reroll operations.
- [x] Periodic server-authoritative inventory evaluation for online team members.
- [x] Original English and Simplified Chinese command-feedback resources.
- [x] Standard and lockout claim behavior extracted into independent rule strategies.
- [x] Isolated visibility strategy and optional hidden mode.
- [x] Inventory objective completion extracted into an independent strategy.
- [x] Independent score-ranking strategy with deterministic tie handling.
- [x] Line victory and no-automatic-victory behavior extracted into independent strategies.
- [x] Compose ranked-mode domain rules without premature line completion.
- [x] Persistent ranked countdown and unique score-leader resolution in domain snapshots and NBT.
- [x] Complete ranked mode with a persistent time limit, server ticking, and command entry point.
- [x] GameTest coverage for ranked expiration and world-data persistence.
- Lockout, inventory, hidden, and ranked rules as isolated strategy modules.
- [x] Versioned JSON Schema, strict validation, and version 0 to version 1 migration tests.
- [x] Bingo-card definition generator with strict round-trip validation and a Gradle entry point.

## Phase 3 — client experience

- [x] Initial optional NeoForge bingo HUD using an original text-panel design.
- Interactive full card screen and original graphical assets.
- [x] Register a versioned, optional channel for enhanced clients.
- [x] Send the application version only to enhanced clients that negotiated the channel.
- [x] Synchronize structured card snapshots to enhanced clients while retaining chat cards.
- [x] Refresh enhanced-client HUD state after starts, rerolls, and claims.
- [x] Explicit optional protocol negotiation so vanilla clients remain supported.

## Phase 4 — integrations and hardening

- Optional recipe-viewer and voice-chat integrations behind compile-time APIs.
- Permissions, performance tests, compatibility matrix, and release automation.

## Architecture boundaries

- `domain`: loader-independent rules and immutable state.
- `domain.rule`: isolated claim policies selected by game mode.
- `application`: commands and use cases.
- `neoforge`: events, persistence adapters, networking, and lifecycle.
- `client`: optional screens and rendering only.

Domain tests must not start Minecraft. NeoForge GameTests cover adapter behavior.
