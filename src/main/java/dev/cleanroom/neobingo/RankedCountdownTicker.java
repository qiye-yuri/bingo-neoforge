package dev.cleanroom.neobingo;

import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import dev.cleanroom.neobingo.network.NeoBingoNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 推进排位游戏倒计时，并在到期时广播最终结果。 */
public final class RankedCountdownTicker {
    private RankedCountdownTicker() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        tick(event.getServer());
    }

    static boolean tick(MinecraftServer server) {
        NeoBingoSavedData data = NeoBingoSavedData.get(server);
        BingoSession session = data.restoreSession().orElse(null);
        if (session == null
                || session.state() != SessionState.RUNNING
                || session.game().orElseThrow().mode() != GameMode.RANKED) {
            return false;
        }
        boolean finished = session.tickRanked();
        data.store(session);
        if (finished || session.remainingTicks().orElseThrow() % 20 == 0) {
            NeoBingoNetwork.syncAllCards(session, server.getPlayerList().getPlayers());
        }
        if (!finished) {
            return false;
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("commands.neo_bingo.ranked.finished"), false);
        session.winner().ifPresentOrElse(
                team -> server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("commands.neo_bingo.win", team.value()), false),
                () -> server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("commands.neo_bingo.ranked.tie"), false));
        return true;
    }
}
