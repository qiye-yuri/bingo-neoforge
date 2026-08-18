package dev.cleanroom.neobingo.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        if (tileClaims.contains(team)) {
            return ClaimOutcome.ALREADY_CLAIMED_BY_TEAM;
        }
        if (mode == GameMode.LOCKOUT && !tileClaims.isEmpty()) {
            return ClaimOutcome.LOCKED_BY_OTHER_TEAM;
        }
        tileClaims.add(team);
        return ClaimOutcome.CLAIMED;
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
