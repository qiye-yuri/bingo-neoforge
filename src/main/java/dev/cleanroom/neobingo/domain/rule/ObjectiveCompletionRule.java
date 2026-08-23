package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.util.Set;

/** 根据服务端观察结果判断单个目标是否完成。 */
@FunctionalInterface
public interface ObjectiveCompletionRule {
    boolean isCompleted(ObjectiveId objective, Set<ObjectiveId> observedObjectives);
}
