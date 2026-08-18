package dev.cleanroom.neobingo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BingoCardTest {
    private static List<ObjectiveId> pool(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> new ObjectiveId("test:objective_" + index))
                .toList();
    }

    @Test
    void generationIsDeterministicForSeed() {
        assertEquals(
                BingoCard.generate(5, pool(40), 1234L).objectives(),
                BingoCard.generate(5, pool(40), 1234L).objectives());
    }

    @Test
    void differentSeedsProduceDifferentCards() {
        assertNotEquals(
                BingoCard.generate(5, pool(40), 1L).objectives(),
                BingoCard.generate(5, pool(40), 2L).objectives());
    }

    @Test
    void rejectsAnInsufficientPool() {
        assertThrows(IllegalArgumentException.class, () -> BingoCard.generate(5, pool(24), 1L));
    }
}
