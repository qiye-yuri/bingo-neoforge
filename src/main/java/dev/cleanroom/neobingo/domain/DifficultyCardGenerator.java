package dev.cleanroom.neobingo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** 从指定难度目标池生成一张确定性卡片，并应用目标互斥组。 */
public final class DifficultyCardGenerator {
    private DifficultyCardGenerator() {
    }

    public static List<ObjectiveId> generate(
            Map<DifficultyTier, List<ObjectiveId>> tiers,
            DifficultyTier tier,
            long seed) {
        return generate(tiers, List.of(), tier, seed);
    }

    public static List<ObjectiveId> generate(
            Map<DifficultyTier, List<ObjectiveId>> tiers,
            List<List<ObjectiveId>> exclusionGroups,
            DifficultyTier tier,
            long seed) {
        Objects.requireNonNull(tiers, "tiers");
        Objects.requireNonNull(tier, "tier");
        Random random = new Random(seed);
        List<ObjectiveId> selected = new ArrayList<>(25);
        List<ObjectiveId> candidates = new ArrayList<>(Objects.requireNonNull(tiers.get(tier), tier.name()));
        Collections.shuffle(candidates, random);
        for (ObjectiveId candidate : candidates) {
            if (selected.size() >= 25) {
                break;
            }
            if (exclusionGroups.stream().noneMatch(group -> group.contains(candidate)
                    && group.stream().anyMatch(selected::contains))) {
                selected.add(candidate);
            }
        }
        if (selected.size() < 25) {
            throw new IllegalArgumentException("难度 " + tier + " 在应用互斥组后至少需要 25 个可用目标");
        }
        Collections.shuffle(selected, random);
        return List.copyOf(selected);
    }
}
