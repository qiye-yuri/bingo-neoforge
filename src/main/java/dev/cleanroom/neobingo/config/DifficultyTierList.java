package dev.cleanroom.neobingo.config;

import dev.cleanroom.neobingo.domain.DifficultyTier;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.util.List;
import java.util.Map;

/** 保存数据驱动的目标难度列表及不能同时出现的目标组。 */
public record DifficultyTierList(
        Map<DifficultyTier, List<ObjectiveId>> tiers,
        List<List<ObjectiveId>> exclusionGroups) {
}
