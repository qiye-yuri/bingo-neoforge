package dev.cleanroom.neobingo.domain.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.cleanroom.neobingo.domain.ClaimOutcome;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClaimRuleTest {
    private static final TeamId RED = new TeamId("red");
    private static final TeamId BLUE = new TeamId("blue");

    @Test
    void standardRuleAllowsDifferentTeamsButRejectsDuplicates() {
        assertEquals(ClaimOutcome.CLAIMED, StandardClaimRule.INSTANCE.evaluate(RED, Set.of(BLUE)));
        assertEquals(ClaimOutcome.ALREADY_CLAIMED_BY_TEAM,
                StandardClaimRule.INSTANCE.evaluate(RED, Set.of(RED, BLUE)));
    }

    @Test
    void lockoutRuleOnlyAllowsTheFirstTeam() {
        assertEquals(ClaimOutcome.CLAIMED, LockoutClaimRule.INSTANCE.evaluate(RED, Set.of()));
        assertEquals(ClaimOutcome.LOCKED_BY_OTHER_TEAM, LockoutClaimRule.INSTANCE.evaluate(RED, Set.of(BLUE)));
        assertEquals(ClaimOutcome.ALREADY_CLAIMED_BY_TEAM,
                LockoutClaimRule.INSTANCE.evaluate(RED, Set.of(RED)));
    }
}
