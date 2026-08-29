package dev.cleanroom.neobingo.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.LinkedHashMap;

/** 保存大厅中下一局游戏的模式与各难度目标数量。 */
public final class LobbyGameSettings {
    private final EnumMap<DifficultyTier, Integer> counts = new EnumMap<>(DifficultyTier.class);
    private GameMode mode;
    private int timedSeconds;
    private int teamSpawnDistanceChunks;
    private boolean nightVision;
    private boolean keepInventory;
    private boolean teamChest;
    private int teamChestRows;
    private final Map<String, Integer> starterItems = new LinkedHashMap<>();

    public LobbyGameSettings() {
        this(GameMode.STANDARD, Map.of(
                DifficultyTier.MAX, 0,
                DifficultyTier.S, 3,
                DifficultyTier.A, 4,
                DifficultyTier.B, 5,
                DifficultyTier.C, 6,
                DifficultyTier.D, 7), 900, 8, false, false, false, 3, Map.of());
    }

    public LobbyGameSettings(GameMode mode, Map<DifficultyTier, Integer> counts) {
        this(mode, counts, 900, 8, false, false, false, 3, Map.of());
    }

    public LobbyGameSettings(
            GameMode mode,
            Map<DifficultyTier, Integer> counts,
            int timedSeconds,
            int teamSpawnDistanceChunks) {
        this(mode, counts, timedSeconds, teamSpawnDistanceChunks, false, false, false, 3, Map.of());
    }

    public LobbyGameSettings(
            GameMode mode, Map<DifficultyTier, Integer> counts,
            int timedSeconds, int teamSpawnDistanceChunks,
            boolean nightVision, boolean keepInventory, boolean teamChest,
            int teamChestRows,
            Map<String, Integer> starterItems) {
        this.mode = mode;
        for (DifficultyTier tier : DifficultyTier.values()) {
            this.counts.put(tier, Math.clamp(counts.getOrDefault(tier, 0), 0, DifficultyDistribution.CARD_SLOTS));
        }
        this.timedSeconds = Math.clamp(timedSeconds, 60, 86_400);
        this.teamSpawnDistanceChunks = Math.clamp(teamSpawnDistanceChunks, 1, 128);
        this.nightVision = nightVision;
        this.keepInventory = keepInventory;
        this.teamChest = teamChest;
        this.teamChestRows = Math.clamp(teamChestRows, 1, 6);
        starterItems.forEach((item, count) -> {
            if (count > 0) this.starterItems.put(item, Math.clamp(count, 1, 64));
        });
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

    public int timedSeconds() {
        return timedSeconds;
    }

    public int adjustTimedSeconds(int delta) {
        timedSeconds = Math.clamp(timedSeconds + delta, 60, 86_400);
        return timedSeconds;
    }

    public int teamSpawnDistanceChunks() {
        return teamSpawnDistanceChunks;
    }

    public int adjustTeamSpawnDistanceChunks(int delta) {
        teamSpawnDistanceChunks = Math.clamp(teamSpawnDistanceChunks + delta, 1, 128);
        return teamSpawnDistanceChunks;
    }

    public boolean nightVision() { return nightVision; }
    public boolean keepInventory() { return keepInventory; }
    public boolean teamChest() { return teamChest; }
    public void toggleNightVision() { nightVision = !nightVision; }
    public void toggleKeepInventory() { keepInventory = !keepInventory; }
    public void toggleTeamChest() { teamChest = !teamChest; }
    public int teamChestRows() { return teamChestRows; }
    public int adjustTeamChestRows(int delta) {
        teamChestRows = Math.clamp(teamChestRows + delta, 1, 6);
        return teamChestRows;
    }

    public int adjustStarterItem(String itemId, int delta) {
        int value = Math.clamp(starterItems.getOrDefault(itemId, 0) + delta, 0, 64);
        if (value == 0) starterItems.remove(itemId); else starterItems.put(itemId, value);
        return value;
    }

    public Map<String, Integer> starterItems() { return Map.copyOf(starterItems); }
}
