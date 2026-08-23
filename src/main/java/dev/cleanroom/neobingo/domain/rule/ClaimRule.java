package dev.cleanroom.neobingo.domain.rule;

import dev.cleanroom.neobingo.domain.ClaimOutcome;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.Set;

/** 决定一个队伍能否认领已有归属状态的格子。 */
public interface ClaimRule {
    ClaimOutcome evaluate(TeamId team, Set<TeamId> existingClaims);
}
