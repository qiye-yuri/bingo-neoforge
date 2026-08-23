package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.ClaimOutcome;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.Objects;
import java.util.Set;

/** 只允许首支队伍占有格子的锁定规则。 */
public final class LockoutClaimRule implements ClaimRule {
    public static final LockoutClaimRule INSTANCE = new LockoutClaimRule();

    private LockoutClaimRule() {
    }

    @Override
    public ClaimOutcome evaluate(TeamId team, Set<TeamId> existingClaims) {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(existingClaims, "existingClaims");
        if (existingClaims.contains(team)) {
            return ClaimOutcome.ALREADY_CLAIMED_BY_TEAM;
        }
        return existingClaims.isEmpty()
                ? ClaimOutcome.CLAIMED
                : ClaimOutcome.LOCKED_BY_OTHER_TEAM;
    }
}
