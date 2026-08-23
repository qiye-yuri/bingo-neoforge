package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.ClaimOutcome;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.Objects;
import java.util.Set;

/** 允许不同队伍分别认领同一格的标准规则。 */
public final class StandardClaimRule implements ClaimRule {
    public static final StandardClaimRule INSTANCE = new StandardClaimRule();

    private StandardClaimRule() {
    }

    @Override
    public ClaimOutcome evaluate(TeamId team, Set<TeamId> existingClaims) {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(existingClaims, "existingClaims");
        return existingClaims.contains(team)
                ? ClaimOutcome.ALREADY_CLAIMED_BY_TEAM
                : ClaimOutcome.CLAIMED;
    }
}
