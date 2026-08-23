package dev.cleanroom.neobingo.domain;

import dev.cleanroom.neobingo.domain.rule.ClaimRule;
import dev.cleanroom.neobingo.domain.rule.LockoutClaimRule;
import dev.cleanroom.neobingo.domain.rule.StandardClaimRule;

public enum GameMode {
    STANDARD(StandardClaimRule.INSTANCE),
    LOCKOUT(LockoutClaimRule.INSTANCE);

    private final ClaimRule claimRule;

    GameMode(ClaimRule claimRule) {
        this.claimRule = claimRule;
    }

    public ClaimRule claimRule() {
        return claimRule;
    }
}
