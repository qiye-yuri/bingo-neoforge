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
        return generate(tiers, exclusionGroups, DifficultyDistribution.uniform(tier), seed);
    }

    public static List<ObjectiveId> generate(
            Map<DifficultyTier, List<ObjectiveId>> tiers,
            List<List<ObjectiveId>> exclusionGroups,
            DifficultyDistribution distribution,
            long seed) {
        Objects.requireNonNull(tiers, "tiers");
        Objects.requireNonNull(distribution, "distribution");
        Random random = new Random(seed);
        List<ObjectiveId> selected = new ArrayList<>(25);
        for (DifficultyTier tier : DifficultyTier.values()) {
            int required = distribution.count(tier);
            List<ObjectiveId> candidates = new ArrayList<>(Objects.requireNonNull(tiers.get(tier), tier.name()));
            Collections.shuffle(candidates, random);
            int before = selected.size();
            for (ObjectiveId candidate : candidates) {
                if (selected.size() - before >= required) {
                    break;
                }
                if (!selected.contains(candidate) && exclusionGroups.stream().noneMatch(group -> group.contains(candidate)
                        && group.stream().anyMatch(selected::contains))) {
                    selected.add(candidate);
                }
            }
            if (selected.size() - before < required) {
                throw new IllegalArgumentException("难度 " + tier + " 在应用互斥组后没有足够的可用目标，需要 " + required + " 个");
            }
        }
        Collections.shuffle(selected, random);
        return List.copyOf(selected);
    }
}
