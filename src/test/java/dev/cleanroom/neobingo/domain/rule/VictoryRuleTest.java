package dev.cleanroom.neobingo.domain.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.cleanroom.neobingo.domain.BingoCard;
import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class VictoryRuleTest {
    private static final TeamId RED = new TeamId("red");

    @Test
    void lineRuleRecognizesCompletedLine() {
        BingoGame game = game();
        IntStream.range(0, 5).forEach(index -> game.claim(RED, index));

        assertTrue(LineVictoryRule.INSTANCE.hasWon(game, RED));
    }

    @Test
    void noAutomaticRuleNeverEndsGameFromClaimState() {
        BingoGame game = game();
        IntStream.range(0, 5).forEach(index -> game.claim(RED, index));

        assertFalse(NoAutomaticVictoryRule.INSTANCE.hasWon(game, RED));
    }

    private static BingoGame game() {
        List<ObjectiveId> objectives = IntStream.range(0, 25)
                .mapToObj(index -> new ObjectiveId("test:objective_" + index))
                .toList();
        return new BingoGame(new BingoCard(5, objectives), GameMode.STANDARD);
    }
}
