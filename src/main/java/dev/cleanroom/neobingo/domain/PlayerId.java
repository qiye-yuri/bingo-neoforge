package dev.cleanroom.neobingo.domain;

import java.util.Objects;
import java.util.UUID;

/** 与玩家名称变化无关的稳定玩家标识。 */
public record PlayerId(UUID value) {
    public PlayerId {
        Objects.requireNonNull(value, "value");
    }
}
