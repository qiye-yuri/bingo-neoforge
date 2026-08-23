package dev.cleanroom.neobingo.domain.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VisibilityRuleTest {
    @Test
    void publicRuleAlwaysRevealsObjective() {
        assertTrue(AlwaysVisibleRule.INSTANCE.isVisible(false));
        assertTrue(AlwaysVisibleRule.INSTANCE.isVisible(true));
    }

    @Test
    void hiddenRuleRevealsObjectiveOnlyAfterViewerClaimsIt() {
        assertFalse(HiddenUntilClaimedRule.INSTANCE.isVisible(false));
        assertTrue(HiddenUntilClaimedRule.INSTANCE.isVisible(true));
    }
}
