package dev.cleanroom.neobingo.domain;

import dev.cleanroom.neobingo.domain.rule.ScoreRankingRule;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

/** 由服务器权威管理格子认领和连线胜利判定的领域聚合。 */
public final class BingoGame {
    private final BingoCard card;
    private final GameMode mode;
    private final Map<Integer, Set<TeamId>> claims = new HashMap<>();

    public BingoGame(BingoCard card, GameMode mode) {
        this.card = Objects.requireNonNull(card, "card");
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public ClaimOutcome claim(TeamId team, int tileIndex) {
        Objects.requireNonNull(team, "team");
        card.checkedIndex(tileIndex);

        Set<TeamId> tileClaims = claims.computeIfAbsent(tileIndex, ignored -> new HashSet<>());
        ClaimOutcome outcome = mode.claimRule().evaluate(team, Set.copyOf(tileClaims));
        if (outcome == ClaimOutcome.CLAIMED) {
            tileClaims.add(team);
        }
        return outcome;
    }

    public boolean isClaimedBy(TeamId team, int tileIndex) {
        Objects.requireNonNull(team, "team");
        card.checkedIndex(tileIndex);
        return claims.getOrDefault(tileIndex, Set.of()).contains(team);
    }

    public int score(TeamId team) {
        Objects.requireNonNull(team, "team");
        return (int) claims.values().stream().filter(teams -> teams.contains(team)).count();
    }

    public List<TeamStanding> standings(Collection<TeamId> teams) {
        Objects.requireNonNull(teams, "teams");
        Map<TeamId, Integer> scores = teams.stream()
                .distinct()
                .collect(Collectors.toMap(team -> team, this::score));
        return ScoreRankingRule.INSTANCE.rank(scores);
    }

    public BingoCard card() {
        return card;
    }

    public GameMode mode() {
        return mode;
    }

    public BingoGameSnapshot snapshot() {
        Map<Integer, Set<TeamId>> copiedClaims = new LinkedHashMap<>();
        claims.forEach((tileIndex, teams) -> copiedClaims.put(tileIndex, Set.copyOf(teams)));
        return new BingoGameSnapshot(card, mode, copiedClaims);
    }

    public static BingoGame restore(BingoGameSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        BingoGame restored = new BingoGame(snapshot.card(), snapshot.mode());
        snapshot.claims().forEach((tileIndex, teams) -> teams.forEach(team -> {
            ClaimOutcome outcome = restored.claim(team, tileIndex);
            if (outcome != ClaimOutcome.CLAIMED) {
                throw new IllegalArgumentException("Snapshot contains conflicting claims");
            }
        }));
        return restored;
    }

    public boolean hasWinningLine(TeamId team) {
        Objects.requireNonNull(team, "team");
        int size = card.size();

        for (int row = 0; row < size; row++) {
            if (isComplete(team, row * size, 1)) {
                return true;
            }
        }
        for (int column = 0; column < size; column++) {
            if (isComplete(team, column, size)) {
                return true;
            }
        }
        return isComplete(team, 0, size + 1)
                || isComplete(team, size - 1, size - 1);
    }

    private boolean isComplete(TeamId team, int start, int step) {
        for (int offset = 0; offset < card.size(); offset++) {
            if (!isClaimedBy(team, start + offset * step)) {
                return false;
            }
        }
        return true;
    }
}
