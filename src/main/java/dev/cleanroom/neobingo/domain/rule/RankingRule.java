package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.domain.TeamStanding;
import java.util.List;
import java.util.Map;

/** 根据队伍分数生成稳定的名次列表。 */
@FunctionalInterface
public interface RankingRule {
    List<TeamStanding> rank(Map<TeamId, Integer> scores);
}
