package dev.cleanroom.neobingo.domain;

import java.util.Objects;

/** Stable, loader-independent identifier for a bingo objective. */
public record ObjectiveId(String value) {
    public ObjectiveId {
        Objects.requireNonNull(value, "value");
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Objective id must be namespaced: " + value);
        }
    }
}
