package dev.cleanroom.neobingo.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 可安全交给持久化适配器的不可变游戏状态。 */
public record BingoGameSnapshot(
        BingoCard card,
        GameMode mode,
        Map<Integer, Set<TeamId>> claims) {
    public BingoGameSnapshot {
        Objects.requireNonNull(card, "card");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(claims, "claims");

        Map<Integer, Set<TeamId>> copiedClaims = new LinkedHashMap<>();
        claims.forEach((tileIndex, teams) -> {
            Objects.requireNonNull(tileIndex, "tileIndex");
            card.objectiveAt(tileIndex);
            Objects.requireNonNull(teams, "teams");
            if (teams.isEmpty() || teams.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Claimed tiles must contain at least one valid team");
            }
            if (mode == GameMode.LOCKOUT && teams.size() > 1) {
                throw new IllegalArgumentException("Lockout tiles cannot be claimed by multiple teams");
            }
            copiedClaims.put(tileIndex, Set.copyOf(teams));
        });
        claims = Map.copyOf(copiedClaims);
    }
}
