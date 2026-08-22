package dev.cleanroom.neobingo.application;

import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.ClaimOutcome;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionClaimResult;
import dev.cleanroom.neobingo.domain.SessionState;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

/** 根据服务器观察到的目标集合批量认领对应格子。 */
public final class ObjectiveClaimService {
    private ObjectiveClaimService() {
    }

    public static ClaimBatchResult claimCompleted(
            BingoSession session,
            PlayerId player,
            Set<ObjectiveId> completedObjectives) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(completedObjectives, "completedObjectives");
        BingoGame game = session.game().orElseThrow(() -> new IllegalStateException("游戏尚未开始"));
        var claimedTiles = new ArrayList<Integer>();

        for (int tileIndex = 0; tileIndex < game.card().tileCount(); tileIndex++) {
            if (session.state() != SessionState.RUNNING) {
                break;
            }
            if (!completedObjectives.contains(game.card().objectiveAt(tileIndex))) {
                continue;
            }
            SessionClaimResult result = session.claim(player, tileIndex);
            if (result.outcome() == ClaimOutcome.CLAIMED) {
                claimedTiles.add(tileIndex);
            }
        }
        return new ClaimBatchResult(claimedTiles, session.state(), session.winner());
    }
}
