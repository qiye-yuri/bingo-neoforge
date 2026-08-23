package dev.cleanroom.neobingo.domain;

import dev.cleanroom.neobingo.domain.rule.ClaimRule;
import dev.cleanroom.neobingo.domain.rule.AlwaysVisibleRule;
import dev.cleanroom.neobingo.domain.rule.HiddenUntilClaimedRule;
import dev.cleanroom.neobingo.domain.rule.LockoutClaimRule;
import dev.cleanroom.neobingo.domain.rule.LineVictoryRule;
import dev.cleanroom.neobingo.domain.rule.StandardClaimRule;
import dev.cleanroom.neobingo.domain.rule.VisibilityRule;
import dev.cleanroom.neobingo.domain.rule.VictoryRule;

public enum GameMode {
    STANDARD(StandardClaimRule.INSTANCE, AlwaysVisibleRule.INSTANCE, LineVictoryRule.INSTANCE),
    LOCKOUT(LockoutClaimRule.INSTANCE, AlwaysVisibleRule.INSTANCE, LineVictoryRule.INSTANCE),
    HIDDEN(StandardClaimRule.INSTANCE, HiddenUntilClaimedRule.INSTANCE, LineVictoryRule.INSTANCE);

    private final ClaimRule claimRule;
    private final VisibilityRule visibilityRule;
    private final VictoryRule victoryRule;

    GameMode(ClaimRule claimRule, VisibilityRule visibilityRule, VictoryRule victoryRule) {
        this.claimRule = claimRule;
        this.visibilityRule = visibilityRule;
        this.victoryRule = victoryRule;
    }

    public ClaimRule claimRule() {
        return claimRule;
    }

    public VisibilityRule visibilityRule() {
        return visibilityRule;
    }

    public VictoryRule victoryRule() {
        return victoryRule;
    }
}
