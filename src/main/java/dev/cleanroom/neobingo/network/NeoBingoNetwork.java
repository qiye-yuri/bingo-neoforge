package dev.cleanroom.neobingo.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

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
                        (payload, context) -> { });
    }
}
