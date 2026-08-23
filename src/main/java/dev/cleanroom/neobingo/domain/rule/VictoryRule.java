package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.TeamId;

/** 判断一次成功认领是否使队伍获胜。 */
@FunctionalInterface
public interface VictoryRule {
    boolean hasWon(BingoGame game, TeamId team);
}
