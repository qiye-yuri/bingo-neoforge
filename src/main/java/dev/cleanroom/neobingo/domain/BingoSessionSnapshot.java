package dev.cleanroom.neobingo.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** 包含恢复单局会话所需全部信息的不可变快照。 */
public record BingoSessionSnapshot(
        SessionState state,
        Map<PlayerId, TeamId> assignments,
        Optional<BingoGameSnapshot> game,
        Optional<Long> seed,
        Optional<TeamId> winner,
        Optional<Long> remainingTicks) {
    public BingoSessionSnapshot {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(assignments, "assignments");
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(seed, "seed");
        Objects.requireNonNull(winner, "winner");
        Objects.requireNonNull(remainingTicks, "remainingTicks");
        remainingTicks.ifPresent(value -> {
            if (value < 0) {
                throw new IllegalArgumentException("Remaining ticks cannot be negative");
            }
        });
        if (assignments.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
            throw new IllegalArgumentException("Team assignments cannot contain null values");
        }
        assignments = Map.copyOf(assignments);

        if (state == SessionState.LOBBY) {
            if (game.isPresent() || seed.isPresent() || winner.isPresent() || remainingTicks.isPresent()) {
                throw new IllegalArgumentException("Lobby snapshots cannot contain game results");
            }
        } else if (game.isEmpty() || seed.isEmpty()) {
            throw new IllegalArgumentException("Started sessions require a game and seed");
        }

        if (state == SessionState.RUNNING && winner.isPresent()) {
            throw new IllegalArgumentException("Running sessions cannot contain a winner");
        }
        if (game.map(BingoGameSnapshot::mode).orElse(null) == GameMode.RANKED) {
            if (remainingTicks.isEmpty()) {
                throw new IllegalArgumentException("Ranked sessions require remaining ticks");
            }
        } else if (remainingTicks.isPresent()) {
            throw new IllegalArgumentException("Only ranked sessions may contain remaining ticks");
        }
        if (winner.isPresent() && !assignments.containsValue(winner.orElseThrow())) {
            throw new IllegalArgumentException("Winner must be represented in the team roster");
        }
    }
}
