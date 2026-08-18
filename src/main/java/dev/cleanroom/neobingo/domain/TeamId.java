package dev.cleanroom.neobingo.domain;

import java.util.Objects;

/** Stable identifier for a team within one game. */
public record TeamId(String value) {
    public TeamId {
        Objects.requireNonNull(value, "value");
        if (!value.matches("[a-z][a-z0-9_-]{0,31}")) {
            throw new IllegalArgumentException("Invalid team id: " + value);
        }
    }
}
