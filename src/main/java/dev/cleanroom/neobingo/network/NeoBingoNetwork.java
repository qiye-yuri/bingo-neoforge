package dev.cleanroom.neobingo.network;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.server.level.ServerPlayer;
import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.presentation.BingoCardTextRenderer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/** 注册可选且带版本号的客户端增强协议。 */
public final class NeoBingoNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private NeoBingoNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .optional()
                .playToClient(
                        ProtocolVersionPayload.TYPE,
                        ProtocolVersionPayload.STREAM_CODEC,
                        (payload, context) -> ClientProtocolState.accept(payload))
                .playToClient(
                        BingoCardPayload.TYPE,
                        BingoCardPayload.STREAM_CODEC,
                        (payload, context) -> ClientProtocolState.accept(payload));
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var setup = ChannelAttributes.getPayloadSetup(player.connection.getConnection());
        if (setup == null
                || setup.getChannel(ConnectionProtocol.PLAY, ProtocolVersionPayload.TYPE.id()) == null) {
            return;
        }
        PacketDistributor.sendToPlayer(
                player,
                new ProtocolVersionPayload(ProtocolVersionPayload.CURRENT_VERSION));
    }

    public static void sendCardIfSupported(ServerPlayer player, BingoGame game, TeamId team) {
        var setup = ChannelAttributes.getPayloadSetup(player.connection.getConnection());
        if (setup == null || setup.getChannel(ConnectionProtocol.PLAY, BingoCardPayload.TYPE.id()) == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new BingoCardPayload(
                team.value(),
                game.mode().name(),
                BingoCardTextRenderer.render(game, team)));
    }

    public static void syncTeamCard(BingoSession session, TeamId team, Iterable<ServerPlayer> players) {
        BingoGame game = session.game().orElseThrow(() -> new IllegalStateException("游戏尚未开始"));
        for (ServerPlayer player : players) {
            if (session.roster().teamOf(new PlayerId(player.getUUID())).filter(team::equals).isPresent()) {
                sendCardIfSupported(player, game, team);
            }
        }
    }

    public static void syncAllCards(BingoSession session, Iterable<ServerPlayer> players) {
        for (TeamId team : session.roster().assignments().values().stream().distinct().toList()) {
            syncTeamCard(session, team, players);
        }
    }
}
