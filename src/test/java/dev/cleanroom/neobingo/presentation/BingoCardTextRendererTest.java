package dev.cleanroom.neobingo.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.cleanroom.neobingo.domain.BingoCard;
import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BingoCardTextRendererTest {
    private static final TeamId RED = new TeamId("red");

    @Test
    void rendersRowsAndTeamClaimMarkers() {
        List<ObjectiveId> objectives = IntStream.range(0, 4)
                .mapToObj(index -> new ObjectiveId("minecraft:item_" + index))
                .toList();
        BingoGame game = new BingoGame(new BingoCard(2, objectives), GameMode.STANDARD);
        game.claim(RED, 1);

        assertEquals(List.of(
                "[ ] minecraft:item_0 | [✓] minecraft:item_1",
                "[ ] minecraft:item_2 | [ ] minecraft:item_3"),
                BingoCardTextRenderer.render(game, RED));
    }

    @Test
    void hiddenModeRevealsOnlyObjectivesClaimedByViewingTeam() {
        List<ObjectiveId> objectives = IntStream.range(0, 4)
                .mapToObj(index -> new ObjectiveId("minecraft:item_" + index))
                .toList();
        BingoGame game = new BingoGame(new BingoCard(2, objectives), GameMode.HIDDEN);
        game.claim(RED, 1);
        game.claim(new TeamId("blue"), 2);

        assertEquals(List.of(
                "[ ] ??? | [✓] minecraft:item_1",
                "[ ] ??? | [ ] ???"),
                BingoCardTextRenderer.render(game, RED));
    }
}
