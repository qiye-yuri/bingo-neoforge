package dev.cleanroom.neobingo.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.domain.rule.InventoryPresenceRule;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ObjectiveClaimServiceTest {
    private static final PlayerId PLAYER =
            new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TeamId RED = new TeamId("red");

    @Test
    void claimsOnlyObjectivesObservedByServer() {
        BingoSession session = runningSession();
        ObjectiveId first = session.game().orElseThrow().card().objectiveAt(0);
        ObjectiveId third = session.game().orElseThrow().card().objectiveAt(2);

        ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                session, PLAYER, Set.of(first, third), InventoryPresenceRule.INSTANCE);

        assertEquals(List.of(0, 2), result.claimedTiles());
        assertEquals(SessionState.RUNNING, result.state());
        assertEquals(2, session.game().orElseThrow().score(RED));
    }

    @Test
    void stopsAfterAClaimCompletesWinningLine() {
        BingoSession session = runningSession();
        Set<ObjectiveId> completed = Set.copyOf(session.game().orElseThrow().card().objectives());

        ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                session, PLAYER, completed, InventoryPresenceRule.INSTANCE);

        assertEquals(List.of(0, 1, 2, 3, 4), result.claimedTiles());
        assertEquals(SessionState.FINISHED, result.state());
        assertEquals(RED, result.winner().orElseThrow());
    }

    @Test
    void delegatesCompletionDecisionToSelectedRule() {
        BingoSession session = runningSession();
        ObjectiveId selected = session.game().orElseThrow().card().objectiveAt(7);

        ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                session,
                PLAYER,
                Set.of(),
                (objective, observed) -> objective.equals(selected));

        assertEquals(List.of(7), result.claimedTiles());
    }

    @Test
    void skipsCompletionEvaluationForAlreadyClaimedTiles() {
        BingoSession session = runningSession();
        session.claim(PLAYER, 0);
        AtomicInteger evaluations = new AtomicInteger();

        ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                session,
                PLAYER,
                Set.of(),
                (objective, observed) -> {
                    evaluations.incrementAndGet();
                    return false;
                });

        assertEquals(List.of(), result.claimedTiles());
        assertEquals(24, evaluations.get());
    }

    private static BingoSession runningSession() {
        List<ObjectiveId> objectives = IntStream.range(0, 25)
                .mapToObj(index -> new ObjectiveId("minecraft:item_" + index))
                .toList();
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.start(5, objectives, 42L, GameMode.STANDARD);
        return session;
    }
}
