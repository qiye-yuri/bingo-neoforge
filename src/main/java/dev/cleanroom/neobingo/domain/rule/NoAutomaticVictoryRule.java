package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.Objects;

/** 禁止由认领操作自动结束游戏，供计时类模式使用。 */
public enum NoAutomaticVictoryRule implements VictoryRule {
    INSTANCE;

    @Override
    public boolean hasWon(BingoGame game, TeamId team) {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(team, "team");
        return false;
    }
}
