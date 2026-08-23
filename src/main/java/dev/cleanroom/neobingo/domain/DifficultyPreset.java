package dev.cleanroom.neobingo.domain;

import java.util.List;
import java.util.Locale;

/** 定义 5×5 Bingo 卡从各目标等级抽取的数量。 */
public enum DifficultyPreset {
    EASY(16, 9, 0, 0, 0),
    MEDIUM(12, 10, 3, 0, 0),
    HARD(9, 10, 5, 1, 0),
    EXTREME(6, 7, 7, 5, 0),
    IMPOSSIBLE(2, 5, 6, 7, 5),
    MAX(2, 5, 6, 7, 5);

    private final List<Integer> counts;

    DifficultyPreset(int easy, int medium, int hard, int extreme, int impossible) {
        counts = List.of(easy, medium, hard, extreme, impossible);
        if (counts.stream().mapToInt(Integer::intValue).sum() != 25) {
            throw new IllegalArgumentException("难度预设的目标数量总和必须为 25");
        }
        if (counts.stream().anyMatch(count -> count < 0)) {
            throw new IllegalArgumentException("难度预设的目标数量不能为负数");
        }
    }

    public int count(DifficultyTier tier) {
        return counts.get(tier.ordinal());
    }

    public static DifficultyPreset parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
