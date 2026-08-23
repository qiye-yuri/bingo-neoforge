package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.domain.TeamStanding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 按分数降序排列，并使用竞赛排名处理并列队伍。 */
public enum ScoreRankingRule implements RankingRule {
    INSTANCE;

    @Override
    public List<TeamStanding> rank(Map<TeamId, Integer> scores) {
        Objects.requireNonNull(scores, "scores");
        var ordered = scores.entrySet().stream()
                .peek(entry -> {
                    Objects.requireNonNull(entry.getKey(), "team");
                    if (Objects.requireNonNull(entry.getValue(), "score") < 0) {
                        throw new IllegalArgumentException("分数不能为负数");
                    }
                })
                .sorted(Map.Entry.<TeamId, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().value()))
                .toList();
        List<TeamStanding> standings = new ArrayList<>(ordered.size());
        Integer previousScore = null;
        int rank = 0;
        for (int index = 0; index < ordered.size(); index++) {
            var entry = ordered.get(index);
            if (!entry.getValue().equals(previousScore)) {
                rank = index + 1;
                previousScore = entry.getValue();
            }
            standings.add(new TeamStanding(rank, entry.getKey(), entry.getValue()));
        }
        return List.copyOf(standings);
    }
}
