package dev.cleanroom.neobingo.domain;

import java.util.EnumMap;
import java.util.Map;

/** 保存大厅中下一局游戏的模式与各难度目标数量。 */
public final class LobbyGameSettings {
    private final EnumMap<DifficultyTier, Integer> counts = new EnumMap<>(DifficultyTier.class);
    private GameMode mode;

    public LobbyGameSettings() {
        this(GameMode.STANDARD, Map.of(
                DifficultyTier.MAX, 0,
                DifficultyTier.S, 3,
                DifficultyTier.A, 4,
                DifficultyTier.B, 5,
                DifficultyTier.C, 6,
                DifficultyTier.D, 7));
    }

    public LobbyGameSettings(GameMode mode, Map<DifficultyTier, Integer> counts) {
        this.mode = mode;
        for (DifficultyTier tier : DifficultyTier.values()) {
            this.counts.put(tier, Math.clamp(counts.getOrDefault(tier, 0), 0, DifficultyDistribution.CARD_SLOTS));
        }
    }

    public GameMode mode() {
        return mode;
    }

    public void mode(GameMode mode) {
        this.mode = mode;
    }

    public int count(DifficultyTier tier) {
        return counts.get(tier);
    }

    public int adjust(DifficultyTier tier, int delta) {
        int value = Math.clamp(count(tier) + delta, 0, DifficultyDistribution.CARD_SLOTS);
        counts.put(tier, value);
        return value;
    }

    public int total() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<DifficultyTier, Integer> counts() {
        return Map.copyOf(counts);
    }

    public DifficultyDistribution distribution() {
        return new DifficultyDistribution(counts);
    }
}
