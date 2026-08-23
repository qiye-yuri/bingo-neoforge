package dev.cleanroom.neobingo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BingoSessionTest {
    private static final PlayerId PLAYER = new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final PlayerId OUTSIDER = new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final TeamId RED = new TeamId("red");

    private static List<ObjectiveId> pool() {
        return IntStream.range(0, 25)
                .mapToObj(index -> new ObjectiveId("test:objective_" + index))
                .toList();
    }

    @Test
    void gameCannotStartWithoutPlayers() {
        BingoSession session = new BingoSession();

        assertThrows(IllegalStateException.class,
                () -> session.start(5, pool(), 42L, GameMode.STANDARD));
    }

    @Test
    void startingFreezesLobbyMembershipChanges() {
        BingoSession session = runningSession();

        assertEquals(SessionState.RUNNING, session.state());
        assertEquals(42L, session.seed().orElseThrow());
        assertThrows(IllegalStateException.class, () -> session.join(OUTSIDER, RED));
        assertThrows(IllegalStateException.class, () -> session.leave(PLAYER));
    }

    @Test
    void playerMustBelongToATeamToClaim() {
        BingoSession session = runningSession();

        assertThrows(IllegalArgumentException.class, () -> session.claim(OUTSIDER, 0));
    }

    @Test
    void winningClaimFinishesTheSession() {
        BingoSession session = runningSession();
        for (int column = 0; column < 5; column++) {
            SessionClaimResult result = session.claim(PLAYER, column);
            if (column < 4) {
                assertEquals(SessionState.RUNNING, result.state());
                assertFalse(result.winner().isPresent());
            }
        }

        assertEquals(SessionState.FINISHED, session.state());
        assertEquals(RED, session.winner().orElseThrow());
        assertThrows(IllegalStateException.class, () -> session.claim(PLAYER, 0));
    }

    @Test
    void rankedModeDoesNotFinishWhenATeamCompletesLine() {
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.startRanked(5, pool(), 42L, 100);

        for (int column = 0; column < 5; column++) {
            session.claim(PLAYER, column);
        }

        assertEquals(SessionState.RUNNING, session.state());
        assertTrue(session.winner().isEmpty());
        assertEquals(5, session.game().orElseThrow().score(RED));
    }

    @Test
    void rankedCountdownFinishesWithUniqueScoreLeader() {
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.startRanked(5, pool(), 42L, 2);
        session.claim(PLAYER, 0);

        assertFalse(session.tickRanked());
        assertEquals(1L, session.remainingTicks().orElseThrow());
        assertTrue(session.tickRanked());
        assertEquals(SessionState.FINISHED, session.state());
        assertEquals(RED, session.winner().orElseThrow());
    }

    @Test
    void operatorCanEndARunningGameWithoutWinner() {
        BingoSession session = runningSession();

        session.end();

        assertEquals(SessionState.FINISHED, session.state());
        assertTrue(session.winner().isEmpty());
    }

    @Test
    void rerollPreservesModeAndRosterWhileClearingClaims() {
        BingoSession session = runningSession();
        session.claim(PLAYER, 0);

        session.reroll(5, pool(), 99L);

        assertEquals(SessionState.RUNNING, session.state());
        assertEquals(99L, session.seed().orElseThrow());
        assertEquals(GameMode.STANDARD, session.game().orElseThrow().mode());
        assertEquals(RED, session.roster().teamOf(PLAYER).orElseThrow());
        assertEquals(0, session.game().orElseThrow().score(RED));
    }

    @Test
    void lobbyAndFinishedSessionsCannotBeRerolled() {
        BingoSession lobby = new BingoSession();
        assertThrows(IllegalStateException.class, () -> lobby.reroll(5, pool(), 1L));

        BingoSession finished = runningSession();
        finished.end();
        assertThrows(IllegalStateException.class, () -> finished.reroll(5, pool(), 1L));
    }

    private static BingoSession runningSession() {
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.start(5, pool(), 42L, GameMode.STANDARD);
        return session;
    }
}
