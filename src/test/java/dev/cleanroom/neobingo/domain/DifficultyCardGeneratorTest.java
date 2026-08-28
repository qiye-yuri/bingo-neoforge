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
    void configuredTiersGenerateTwentyFiveUniqueObjectives() {
        Map<DifficultyTier, List<ObjectiveId>> tiers = tiers(25);
        for (DifficultyTier tier : DifficultyTier.values()) {
            List<ObjectiveId> card = DifficultyCardGenerator.generate(tiers, tier, 42L);
            assertEquals(25, card.size());
            assertEquals(25, card.stream().distinct().count());
        }
    }

    @Test
    void sameSeedAndPresetAreDeterministic() {
        Map<DifficultyTier, List<ObjectiveId>> tiers = tiers(25);
        assertEquals(
                DifficultyCardGenerator.generate(tiers, DifficultyTier.S, 42L),
                DifficultyCardGenerator.generate(tiers, DifficultyTier.S, 42L));
    }

    @Test
    void insufficientTierIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> DifficultyCardGenerator.generate(tiers(1), DifficultyTier.MAX, 42L));
    }

    @Test
    void distributionSelectsExactCountFromEveryTier() {
        Map<DifficultyTier, List<ObjectiveId>> tiers = tiers(25);
        DifficultyDistribution distribution = new DifficultyDistribution(Map.of(
                DifficultyTier.MAX, 1,
                DifficultyTier.S, 2,
                DifficultyTier.A, 3,
                DifficultyTier.B, 4,
                DifficultyTier.C, 5,
                DifficultyTier.D, 10));

        List<ObjectiveId> card = DifficultyCardGenerator.generate(tiers, List.of(), distribution, 42L);

        assertEquals(25, card.size());
        for (DifficultyTier tier : DifficultyTier.values()) {
            String prefix = "test:" + tier.name().toLowerCase() + "_";
            assertEquals(distribution.count(tier), card.stream()
                    .filter(objective -> objective.value().startsWith(prefix))
                    .count());
        }
    }

    @Test
    void distributionMustContainExactlyTwentyFiveSlots() {
        assertThrows(IllegalArgumentException.class,
                () -> new DifficultyDistribution(Map.of(DifficultyTier.C, 24)));
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
