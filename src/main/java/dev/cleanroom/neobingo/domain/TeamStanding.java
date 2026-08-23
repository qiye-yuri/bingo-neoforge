package dev.cleanroom.neobingo.domain;

import java.util.Objects;

/** 表示队伍在计分榜中的名次和分数。 */
public record TeamStanding(int rank, TeamId team, int score) {
    public TeamStanding {
        if (rank < 1) {
            throw new IllegalArgumentException("名次必须为正数");
        }
        Objects.requireNonNull(team, "team");
        if (score < 0) {
            throw new IllegalArgumentException("分数不能为负数");
        }
    }
}
