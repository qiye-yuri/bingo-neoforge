package dev.cleanroom.neobingo.presentation;

import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/** 将宾果卡渲染为原版客户端可接收的聊天文本。 */
public final class BingoCardTextRenderer {
    private BingoCardTextRenderer() {
    }

    public static List<String> render(BingoGame game, TeamId team) {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(team, "team");
        List<String> rows = new ArrayList<>(game.card().size());
        for (int row = 0; row < game.card().size(); row++) {
            int rowStart = row * game.card().size();
            rows.add(IntStream.range(rowStart, rowStart + game.card().size())
                    .mapToObj(index -> marker(game, team, index) + game.card().objectiveAt(index).value())
                    .reduce((left, right) -> left + " | " + right)
                    .orElse(""));
        }
        return List.copyOf(rows);
    }

    private static String marker(BingoGame game, TeamId team, int tileIndex) {
        return game.isClaimedBy(team, tileIndex) ? "[✓] " : "[ ] ";
    }
}
