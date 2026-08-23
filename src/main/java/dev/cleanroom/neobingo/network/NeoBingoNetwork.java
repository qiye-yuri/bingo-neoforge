package dev.cleanroom.neobingo.network;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.server.level.ServerPlayer;
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
}
