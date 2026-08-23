package dev.cleanroom.neobingo.domain.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.domain.TeamStanding;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScoreRankingRuleTest {
    @Test
    void ordersByScoreAndAssignsCompetitionRanks() {
        TeamId red = new TeamId("red");
        TeamId blue = new TeamId("blue");
        TeamId green = new TeamId("green");

        assertEquals(java.util.List.of(
                new TeamStanding(1, blue, 4),
                new TeamStanding(1, red, 4),
                new TeamStanding(3, green, 2)),
                ScoreRankingRule.INSTANCE.rank(Map.of(red, 4, blue, 4, green, 2)));
    }

    @Test
    void rejectsNegativeScores() {
        assertThrows(IllegalArgumentException.class,
                () -> ScoreRankingRule.INSTANCE.rank(Map.of(new TeamId("red"), -1)));
    }
}
