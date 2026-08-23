package dev.cleanroom.neobingo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RandomTeamAssignmentTest {
    @Test
    void distributesEveryJoinedPlayerEvenly() {
        BingoSession session = sessionWithPlayers(7);

        session.randomizeTeams(3, new Random(42));

        List<Integer> sizes = new ArrayList<>(session.roster().teamSizes().values());
        sizes.sort(Integer::compareTo);
        assertEquals(List.of(2, 2, 3), sizes);
        assertEquals(7, session.roster().assignments().size());
    }

    @Test
    void rejectsMoreTeamsThanPlayers() {
        BingoSession session = sessionWithPlayers(2);

        assertThrows(IllegalArgumentException.class, () -> session.randomizeTeams(3, new Random(42)));
    }

    private static BingoSession sessionWithPlayers(int count) {
        BingoSession session = new BingoSession();
        for (int index = 0; index < count; index++) {
            session.join(new PlayerId(new UUID(0, index + 1)), new TeamId("waiting"));
        }
        return session;
    }
}
