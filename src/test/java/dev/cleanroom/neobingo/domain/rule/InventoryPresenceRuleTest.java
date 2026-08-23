package dev.cleanroom.neobingo.domain.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InventoryPresenceRuleTest {
    @Test
    void completesOnlyAnObjectiveObservedInInventory() {
        ObjectiveId apple = new ObjectiveId("minecraft:apple");
        Set<ObjectiveId> observed = Set.of(apple);

        assertTrue(InventoryPresenceRule.INSTANCE.isCompleted(apple, observed));
        assertFalse(InventoryPresenceRule.INSTANCE.isCompleted(
                new ObjectiveId("minecraft:bread"), observed));
    }
}
