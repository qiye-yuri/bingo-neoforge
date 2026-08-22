package dev.cleanroom.neobingo.domain;

import java.util.Objects;
import java.util.Optional;

/** 描述一次认领操作及其是否结束游戏。 */
public record SessionClaimResult(
        ClaimOutcome outcome,
        SessionState state,
        Optional<TeamId> winner) {
    public SessionClaimResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(winner, "winner");
    }
}
