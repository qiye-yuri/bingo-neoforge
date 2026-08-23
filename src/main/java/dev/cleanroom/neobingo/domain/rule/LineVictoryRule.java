package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.Objects;

/** 在队伍完成任意横线、竖线或对角线时判定获胜。 */
public enum LineVictoryRule implements VictoryRule {
    INSTANCE;

    @Override
    public boolean hasWon(BingoGame game, TeamId team) {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(team, "team");
        return game.hasWinningLine(team);
    }
}
