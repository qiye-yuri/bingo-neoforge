package dev.cleanroom.neobingo.application;

import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.ClaimOutcome;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionClaimResult;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.domain.rule.ObjectiveCompletionRule;
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
            Set<ObjectiveId> observedObjectives,
            ObjectiveCompletionRule completionRule) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(observedObjectives, "observedObjectives");
        Objects.requireNonNull(completionRule, "completionRule");
        BingoGame game = session.game().orElseThrow(() -> new IllegalStateException("游戏尚未开始"));
        TeamId team = session.roster().teamOf(player)
                .orElseThrow(() -> new IllegalArgumentException("玩家尚未加入队伍"));
        var claimedTiles = new ArrayList<Integer>();

        for (int tileIndex = 0; tileIndex < game.card().tileCount(); tileIndex++) {
            if (session.state() != SessionState.RUNNING) {
                break;
            }
            if (game.isClaimedBy(team, tileIndex)) {
                continue;
            }
            if (!completionRule.isCompleted(game.card().objectiveAt(tileIndex), observedObjectives)) {
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
