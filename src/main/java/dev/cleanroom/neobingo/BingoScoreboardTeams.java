package dev.cleanroom.neobingo;

import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Locale;

/** 将 Bingo 队伍同步到原版计分板，以便在 Tab 列表中显示队伍颜色。 */
public final class BingoScoreboardTeams {
    private static final List<ChatFormatting> FALLBACK_COLORS = List.of(
            ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD,
            ChatFormatting.DARK_AQUA, ChatFormatting.DARK_PURPLE, ChatFormatting.GRAY);

    private BingoScoreboardTeams() {
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        NeoBingoSavedData.get(player.getServer()).restoreSession()
                .flatMap(session -> session.roster().teamOf(new PlayerId(player.getUUID())))
                .ifPresent(team -> assign(player, team));
    }

    public static void synchronize(BingoSession session, Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            session.roster().teamOf(new PlayerId(player.getUUID()))
                    .ifPresentOrElse(team -> assign(player, team), () -> remove(player));
        }
    }

    public static void assign(ServerPlayer player, TeamId team) {
        ServerScoreboard scoreboard = player.getServer().getScoreboard();
        String scoreboardTeamName = scoreboardTeamName(team);
        PlayerTeam scoreboardTeam = scoreboard.getPlayerTeam(scoreboardTeamName);
        if (scoreboardTeam == null) {
            scoreboardTeam = scoreboard.addPlayerTeam(scoreboardTeamName);
        }
        scoreboardTeam.setDisplayName(Component.literal(team.value()));
        scoreboardTeam.setColor(colorOf(team));
        scoreboard.addPlayerToTeam(player.getScoreboardName(), scoreboardTeam);
    }

    public static void remove(ServerPlayer player) {
        ServerScoreboard scoreboard = player.getServer().getScoreboard();
        PlayerTeam current = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (current != null && current.getName().startsWith("nb_")) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), current);
        }
    }

    private static String scoreboardTeamName(TeamId team) {
        return "nb_" + Integer.toUnsignedString(team.value().toLowerCase(Locale.ROOT).hashCode(), 36);
    }

    private static ChatFormatting colorOf(TeamId team) {
        return switch (team.value().toLowerCase(Locale.ROOT)) {
            case "red" -> ChatFormatting.RED;
            case "blue" -> ChatFormatting.BLUE;
            case "green" -> ChatFormatting.GREEN;
            case "yellow" -> ChatFormatting.YELLOW;
            default -> FALLBACK_COLORS.get(Math.floorMod(team.value().hashCode(), FALLBACK_COLORS.size()));
        };
    }
}
