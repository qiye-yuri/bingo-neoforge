package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.util.Objects;
import java.util.Set;

/** 在服务端物品栏中观察到同名物品时完成目标。 */
public enum InventoryPresenceRule implements ObjectiveCompletionRule {
    INSTANCE;

    @Override
    public boolean isCompleted(ObjectiveId objective, Set<ObjectiveId> observedObjectives) {
        Objects.requireNonNull(objective, "objective");
        Objects.requireNonNull(observedObjectives, "observedObjectives");
        return observedObjectives.contains(objective);
    }
}
