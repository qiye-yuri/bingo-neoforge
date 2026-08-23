package dev.cleanroom.neobingo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** 按难度预设从彼此隔离的目标池生成一张确定性卡片。 */
public final class DifficultyCardGenerator {
    private DifficultyCardGenerator() {
    }

    public static List<ObjectiveId> generate(
            Map<DifficultyTier, List<ObjectiveId>> tiers,
            DifficultyPreset preset,
            long seed) {
        Objects.requireNonNull(tiers, "tiers");
        Objects.requireNonNull(preset, "preset");
        Random random = new Random(seed);
        List<ObjectiveId> selected = new ArrayList<>(25);
        for (DifficultyTier tier : DifficultyTier.values()) {
            List<ObjectiveId> candidates = new ArrayList<>(Objects.requireNonNull(tiers.get(tier), tier.name()));
            Collections.shuffle(candidates, random);
            int count = preset.count(tier);
            if (candidates.size() < count) {
                throw new IllegalArgumentException("难度 " + tier + " 的目标数量不足，需要 " + count + " 个");
            }
            selected.addAll(candidates.subList(0, count));
        }
        if (selected.stream().distinct().count() != selected.size()) {
            throw new IllegalArgumentException("不同难度的目标池不能包含重复目标");
        }
        Collections.shuffle(selected, random);
        return List.copyOf(selected);
    }
}
