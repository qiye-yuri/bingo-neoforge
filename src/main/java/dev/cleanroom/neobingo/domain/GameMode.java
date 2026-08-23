package dev.cleanroom.neobingo.domain;

import dev.cleanroom.neobingo.domain.rule.ClaimRule;
import dev.cleanroom.neobingo.domain.rule.AlwaysVisibleRule;
import dev.cleanroom.neobingo.domain.rule.HiddenUntilClaimedRule;
import dev.cleanroom.neobingo.domain.rule.LockoutClaimRule;
import dev.cleanroom.neobingo.domain.rule.StandardClaimRule;
import dev.cleanroom.neobingo.domain.rule.VisibilityRule;

public enum GameMode {
    STANDARD(StandardClaimRule.INSTANCE, AlwaysVisibleRule.INSTANCE),
    LOCKOUT(LockoutClaimRule.INSTANCE, AlwaysVisibleRule.INSTANCE),
    HIDDEN(StandardClaimRule.INSTANCE, HiddenUntilClaimedRule.INSTANCE);

    private final ClaimRule claimRule;
    private final VisibilityRule visibilityRule;

    GameMode(ClaimRule claimRule, VisibilityRule visibilityRule) {
        this.claimRule = claimRule;
        this.visibilityRule = visibilityRule;
    }

    public ClaimRule claimRule() {
        return claimRule;
    }

    public VisibilityRule visibilityRule() {
        return visibilityRule;
    }
}
