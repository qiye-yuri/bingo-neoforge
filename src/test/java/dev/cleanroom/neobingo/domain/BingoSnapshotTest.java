package dev.cleanroom.neobingo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BingoSnapshotTest {
    private static final PlayerId PLAYER = new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TeamId RED = new TeamId("red");

    private static List<ObjectiveId> pool() {
        return IntStream.range(0, 25)
                .mapToObj(index -> new ObjectiveId("test:objective_" + index))
                .toList();
    }

    @Test
    void restoresRunningSessionWithoutLosingClaims() {
        BingoSession original = runningSession();
        original.claim(PLAYER, 0);
        original.claim(PLAYER, 6);

        BingoSession restored = BingoSession.restore(original.snapshot());

        assertEquals(SessionState.RUNNING, restored.state());
        assertEquals(42L, restored.seed().orElseThrow());
        assertEquals(2, restored.game().orElseThrow().score(RED));
        assertEquals(ClaimOutcome.ALREADY_CLAIMED_BY_TEAM,
                restored.claim(PLAYER, 0).outcome());
    }

    @Test
    void restoresFinishedSessionAndWinner() {
        BingoSession original = runningSession();
        for (int tileIndex = 0; tileIndex < 5; tileIndex++) {
            original.claim(PLAYER, tileIndex);
        }

        BingoSession restored = BingoSession.restore(original.snapshot());

        assertEquals(SessionState.FINISHED, restored.state());
        assertEquals(RED, restored.winner().orElseThrow());
        assertTrue(restored.game().orElseThrow().hasWinningLine(RED));
    }

    @Test
    void rejectsLockoutSnapshotWithConflictingClaims() {
        BingoCard card = BingoCard.generate(5, pool(), 42L);

        assertThrows(IllegalArgumentException.class, () -> new BingoGameSnapshot(
                card,
                GameMode.LOCKOUT,
                Map.of(0, Set.of(RED, new TeamId("blue")))));
    }

    @Test
    void rejectsWinnerWithoutACompletedLine() {
        BingoSession original = runningSession();
        original.claim(PLAYER, 0);
        BingoSessionSnapshot running = original.snapshot();
        BingoSessionSnapshot invalid = new BingoSessionSnapshot(
                SessionState.FINISHED,
                running.assignments(),
                running.game(),
                running.seed(),
                Optional.of(RED),
                Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> BingoSession.restore(invalid));
    }

    private static BingoSession runningSession() {
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.start(5, pool(), 42L, GameMode.STANDARD);
        return session;
    }
}
