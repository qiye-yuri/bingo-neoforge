package dev.cleanroom.neobingo.domain;

import java.util.Objects;

/** 与模组加载器无关且保持稳定的 Bingo 目标标识。 */
public record ObjectiveId(String value) {
    public ObjectiveId {
        Objects.requireNonNull(value, "value");
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Objective id must be namespaced: " + value);
        }
    }
}
