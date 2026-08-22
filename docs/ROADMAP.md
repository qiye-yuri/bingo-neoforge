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
- Lockout, inventory, hidden, and ranked rules as isolated strategy modules.
- Versioned JSON schema, validation, data generation, and migration tests.

## Phase 3 — client experience

- Optional NeoForge HUD and card screen with original assets.
- Explicit protocol negotiation so vanilla clients remain supported.

## Phase 4 — integrations and hardening

- Optional recipe-viewer and voice-chat integrations behind compile-time APIs.
- Permissions, performance tests, compatibility matrix, and release automation.

## Architecture boundaries

- `domain`: loader-independent rules and immutable state.
- `application`: commands and use cases.
- `neoforge`: events, persistence adapters, networking, and lifecycle.
- `client`: optional screens and rendering only.

Domain tests must not start Minecraft. NeoForge GameTests cover adapter behavior.
