package dev.cleanroom.neobingo.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** 管理单局游戏中玩家与队伍之间的一对一关系。 */
public final class TeamRoster {
    private final Map<PlayerId, TeamId> assignments = new LinkedHashMap<>();

    public Optional<TeamId> join(PlayerId player, TeamId team) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(team, "team");
        return Optional.ofNullable(assignments.put(player, team));
    }

    public Optional<TeamId> leave(PlayerId player) {
        Objects.requireNonNull(player, "player");
        return Optional.ofNullable(assignments.remove(player));
    }

    public Optional<TeamId> teamOf(PlayerId player) {
        Objects.requireNonNull(player, "player");
        return Optional.ofNullable(assignments.get(player));
    }

    public Set<PlayerId> membersOf(TeamId team) {
        Objects.requireNonNull(team, "team");
        return assignments.entrySet().stream()
                .filter(entry -> entry.getValue().equals(team))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public int playerCount() {
        return assignments.size();
    }

    public Map<TeamId, Integer> teamSizes() {
        Map<TeamId, Integer> sizes = new LinkedHashMap<>();
        assignments.values().stream()
                .distinct()
                .sorted(java.util.Comparator.comparing(TeamId::value))
                .forEach(team -> sizes.put(team, membersOf(team).size()));
        return Collections.unmodifiableMap(sizes);
    }

    public Map<PlayerId, TeamId> assignments() {
        return Map.copyOf(assignments);
    }
}
