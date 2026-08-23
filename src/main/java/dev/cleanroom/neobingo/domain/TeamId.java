package dev.cleanroom.neobingo.domain;

import java.util.Objects;

/** 单局游戏内保持稳定的队伍标识。 */
public record TeamId(String value) {
    public TeamId {
        Objects.requireNonNull(value, "value");
        if (!value.matches("[a-z][a-z0-9_-]{0,31}")) {
            throw new IllegalArgumentException("Invalid team id: " + value);
        }
    }
}
