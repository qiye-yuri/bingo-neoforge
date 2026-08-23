package dev.cleanroom.neobingo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DifficultyCardGeneratorTest {
    @Test
    void presetsAlwaysGenerateTwentyFiveUniqueObjectives() {
        Map<DifficultyTier, List<ObjectiveId>> tiers = tiers(25);
        for (DifficultyPreset preset : DifficultyPreset.values()) {
            List<ObjectiveId> card = DifficultyCardGenerator.generate(tiers, preset, 42L);
            assertEquals(25, card.size());
            assertEquals(25, card.stream().distinct().count());
        }
    }

    @Test
    void sameSeedAndPresetAreDeterministic() {
        Map<DifficultyTier, List<ObjectiveId>> tiers = tiers(25);
        assertEquals(
                DifficultyCardGenerator.generate(tiers, DifficultyPreset.HARD, 42L),
                DifficultyCardGenerator.generate(tiers, DifficultyPreset.HARD, 42L));
    }

    @Test
    void insufficientTierIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> DifficultyCardGenerator.generate(tiers(1), DifficultyPreset.EASY, 42L));
    }

    private static Map<DifficultyTier, List<ObjectiveId>> tiers(int count) {
        Map<DifficultyTier, List<ObjectiveId>> tiers = new EnumMap<>(DifficultyTier.class);
        for (DifficultyTier tier : DifficultyTier.values()) {
            tiers.put(tier, IntStream.range(0, count)
                    .mapToObj(index -> new ObjectiveId("test:" + tier.name().toLowerCase() + "_" + index))
                    .toList());
        }
        return tiers;
    }
}
