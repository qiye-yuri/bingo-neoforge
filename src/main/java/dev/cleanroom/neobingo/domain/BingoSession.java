package dev.cleanroom.neobingo.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 维护大厅、运行中和已结束三个阶段的单局游戏生命周期。 */
public final class BingoSession {
    private final TeamRoster roster = new TeamRoster();
    private SessionState state = SessionState.LOBBY;
    private BingoGame game;
    private long seed;
    private TeamId winner;

    public void join(PlayerId player, TeamId team) {
        requireState(SessionState.LOBBY);
        roster.join(player, team);
    }

    public void leave(PlayerId player) {
        requireState(SessionState.LOBBY);
        roster.leave(player);
    }

    public void start(int cardSize, List<ObjectiveId> objectivePool, long seed, GameMode mode) {
        requireState(SessionState.LOBBY);
        if (roster.playerCount() == 0) {
            throw new IllegalStateException("Cannot start a game without players");
        }
        this.game = new BingoGame(BingoCard.generate(cardSize, objectivePool, seed), mode);
        this.seed = seed;
        this.state = SessionState.RUNNING;
    }

    public SessionClaimResult claim(PlayerId player, int tileIndex) {
        requireState(SessionState.RUNNING);
        TeamId team = roster.teamOf(player)
                .orElseThrow(() -> new IllegalArgumentException("Player has not joined a team"));
        ClaimOutcome outcome = game.claim(team, tileIndex);
        if (outcome == ClaimOutcome.CLAIMED && game.hasWinningLine(team)) {
            winner = team;
            state = SessionState.FINISHED;
        }
        return new SessionClaimResult(outcome, state, winner());
    }

    public void end() {
        requireState(SessionState.RUNNING);
        state = SessionState.FINISHED;
    }

    public SessionState state() {
        return state;
    }

    public TeamRoster roster() {
        return roster;
    }

    public Optional<BingoGame> game() {
        return Optional.ofNullable(game);
    }

    public Optional<Long> seed() {
        return state == SessionState.LOBBY ? Optional.empty() : Optional.of(seed);
    }

    public Optional<TeamId> winner() {
        return Optional.ofNullable(winner);
    }

    private void requireState(SessionState expected) {
        Objects.requireNonNull(expected, "expected");
        if (state != expected) {
            throw new IllegalStateException("Expected state " + expected + " but was " + state);
        }
    }
}
