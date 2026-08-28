package dev.cleanroom.neobingo.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** 定义一张 5×5 卡片中各难度目标的精确数量。 */
public record DifficultyDistribution(Map<DifficultyTier, Integer> counts) {
    public static final int CARD_SLOTS = 25;

    public DifficultyDistribution {
        Objects.requireNonNull(counts, "counts");
        EnumMap<DifficultyTier, Integer> normalized = new EnumMap<>(DifficultyTier.class);
        for (DifficultyTier tier : DifficultyTier.values()) {
            int count = counts.getOrDefault(tier, 0);
            if (count < 0 || count > CARD_SLOTS) {
                throw new IllegalArgumentException("各难度数量必须介于 0 到 25");
            }
            normalized.put(tier, count);
        }
        if (normalized.values().stream().mapToInt(Integer::intValue).sum() != CARD_SLOTS) {
            throw new IllegalArgumentException("各难度数量之和必须为 25");
        }
        counts = Map.copyOf(normalized);
    }

    public static DifficultyDistribution uniform(DifficultyTier tier) {
        return new DifficultyDistribution(Map.of(tier, CARD_SLOTS));
    }

    public int count(DifficultyTier tier) {
        return counts.get(tier);
    }
}
