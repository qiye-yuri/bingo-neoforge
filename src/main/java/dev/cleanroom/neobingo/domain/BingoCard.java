package dev.cleanroom.neobingo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Immutable square card containing unique objective identifiers. */
public final class BingoCard {
    private final int size;
    private final List<ObjectiveId> objectives;

    public BingoCard(int size, List<ObjectiveId> objectives) {
        if (size < 1) {
            throw new IllegalArgumentException("Card size must be positive");
        }
        Objects.requireNonNull(objectives, "objectives");
        if (objectives.size() != size * size) {
            throw new IllegalArgumentException("Expected " + size * size + " objectives");
        }
        if (objectives.stream().distinct().count() != objectives.size()) {
            throw new IllegalArgumentException("Card objectives must be unique");
        }
        this.size = size;
        this.objectives = List.copyOf(objectives);
    }

    public static BingoCard generate(int size, List<ObjectiveId> objectivePool, long seed) {
        Objects.requireNonNull(objectivePool, "objectivePool");
        int required = Math.multiplyExact(size, size);
        if (objectivePool.stream().distinct().count() < required) {
            throw new IllegalArgumentException("Objective pool does not contain enough unique entries");
        }

        var shuffled = new ArrayList<>(objectivePool.stream().distinct().toList());
        Collections.shuffle(shuffled, new Random(seed));
        return new BingoCard(size, shuffled.subList(0, required));
    }

    public int size() {
        return size;
    }

    public int tileCount() {
        return objectives.size();
    }

    public ObjectiveId objectiveAt(int tileIndex) {
        return objectives.get(checkedIndex(tileIndex));
    }

    public List<ObjectiveId> objectives() {
        return objectives;
    }

    int checkedIndex(int tileIndex) {
        if (tileIndex < 0 || tileIndex >= tileCount()) {
            throw new IndexOutOfBoundsException("Invalid tile index: " + tileIndex);
        }
        return tileIndex;
    }
}
