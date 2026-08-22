package dev.cleanroom.neobingo;

import dev.cleanroom.neobingo.application.ClaimBatchResult;
import dev.cleanroom.neobingo.application.ObjectiveClaimService;
import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 定期根据在线玩家的服务端物品栏自动认领目标。 */
public final class InventoryClaimTicker {
    private static final int CHECK_INTERVAL_TICKS = 20;

    private InventoryClaimTicker() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS == 0) {
            evaluatePlayers(server, server.getPlayerList().getPlayers());
        }
    }

    static void evaluatePlayers(MinecraftServer server, List<ServerPlayer> players) {
        NeoBingoSavedData data = NeoBingoSavedData.get(server);
        BingoSession session = data.restoreSession().orElse(null);
        if (session == null || session.state() != SessionState.RUNNING) {
            return;
        }

        boolean changed = false;
        for (ServerPlayer player : players) {
            PlayerId playerId = new PlayerId(player.getUUID());
            if (session.roster().teamOf(playerId).isEmpty()) {
                continue;
            }
            ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                    session,
                    playerId,
                    ServerInventoryObjectiveReader.read(player));
            if (result.claimedTiles().isEmpty()) {
                continue;
            }
            changed = true;
            player.sendSystemMessage(Component.literal("自动认领了 " + result.claimedTiles().size() + " 个宾果格子"));
            if (result.state() == SessionState.FINISHED) {
                result.winner().ifPresent(team -> server.getPlayerList().broadcastSystemMessage(
                        Component.literal("队伍 " + team.value() + " 完成连线并获胜"),
                        false));
                break;
            }
        }
        if (changed) {
            data.store(session);
        }
    }
}
