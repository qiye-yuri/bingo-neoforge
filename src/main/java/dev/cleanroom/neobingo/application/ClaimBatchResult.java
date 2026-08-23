package dev.cleanroom.neobingo.application;

import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 汇总一次服务端目标检查所产生的格子认领。 */
public record ClaimBatchResult(
        List<Integer> claimedTiles,
        SessionState state,
        Optional<TeamId> winner) {
    public ClaimBatchResult {
        claimedTiles = List.copyOf(claimedTiles);
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(winner, "winner");
    }
}
