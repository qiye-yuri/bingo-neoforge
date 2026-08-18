package dev.cleanroom.neobingo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BingoGameTest {
    private static final TeamId RED = new TeamId("red");
    private static final TeamId BLUE = new TeamId("blue");

    private static BingoCard card() {
        List<ObjectiveId> objectives = IntStream.range(0, 25)
                .mapToObj(index -> new ObjectiveId("test:objective_" + index))
                .toList();
        return new BingoCard(5, objectives);
    }

    @Test
    void standardModeAllowsMultipleTeamsToClaimOneTile() {
        BingoGame game = new BingoGame(card(), GameMode.STANDARD);

        assertEquals(ClaimOutcome.CLAIMED, game.claim(RED, 0));
        assertEquals(ClaimOutcome.CLAIMED, game.claim(BLUE, 0));
        assertEquals(1, game.score(RED));
        assertEquals(1, game.score(BLUE));
    }

    @Test
    void lockoutModeRejectsASecondTeam() {
        BingoGame game = new BingoGame(card(), GameMode.LOCKOUT);

        assertEquals(ClaimOutcome.CLAIMED, game.claim(RED, 0));
        assertEquals(ClaimOutcome.LOCKED_BY_OTHER_TEAM, game.claim(BLUE, 0));
        assertFalse(game.isClaimedBy(BLUE, 0));
    }

    @Test
    void repeatedClaimIsIdempotent() {
        BingoGame game = new BingoGame(card(), GameMode.STANDARD);

        assertEquals(ClaimOutcome.CLAIMED, game.claim(RED, 3));
        assertEquals(ClaimOutcome.ALREADY_CLAIMED_BY_TEAM, game.claim(RED, 3));
        assertEquals(1, game.score(RED));
    }

    @Test
    void detectsRowsColumnsAndDiagonals() {
        assertWinningLine(0, 1, 2, 3, 4);
        assertWinningLine(2, 7, 12, 17, 22);
        assertWinningLine(0, 6, 12, 18, 24);
        assertWinningLine(4, 8, 12, 16, 20);
    }

    private static void assertWinningLine(int... tiles) {
        BingoGame game = new BingoGame(card(), GameMode.STANDARD);
        for (int index = 0; index < tiles.length; index++) {
            game.claim(RED, tiles[index]);
            if (index < tiles.length - 1) {
                assertFalse(game.hasWinningLine(RED));
            }
        }
        assertTrue(game.hasWinningLine(RED));
    }
}
