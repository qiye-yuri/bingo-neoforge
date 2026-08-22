package dev.cleanroom.neobingo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamRosterTest {
    private static final PlayerId PLAYER = new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TeamId RED = new TeamId("red");
    private static final TeamId BLUE = new TeamId("blue");

    @Test
    void joiningAnotherTeamMovesThePlayer() {
        TeamRoster roster = new TeamRoster();

        assertTrue(roster.join(PLAYER, RED).isEmpty());
        assertEquals(RED, roster.join(PLAYER, BLUE).orElseThrow());
        assertTrue(roster.membersOf(RED).isEmpty());
        assertEquals(BLUE, roster.teamOf(PLAYER).orElseThrow());
    }

    @Test
    void returnedCollectionsCannotMutateTheRoster() {
        TeamRoster roster = new TeamRoster();
        roster.join(PLAYER, RED);

        assertThrows(UnsupportedOperationException.class,
                () -> roster.assignments().put(PLAYER, BLUE));
        assertThrows(UnsupportedOperationException.class,
                () -> roster.membersOf(RED).clear());
    }
}
