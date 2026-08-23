package dev.cleanroom.neobingo.network;

import java.util.Optional;

/** 保存当前客户端连接已确认的增强协议版本。 */
public final class ClientProtocolState {
    private static volatile int negotiatedVersion;
    private static volatile BingoCardPayload latestCard;
    private static volatile boolean hudVisible = true;

    private ClientProtocolState() {
    }

    public static int negotiatedVersion() {
        return negotiatedVersion;
    }

    static void accept(ProtocolVersionPayload payload) {
        negotiatedVersion = payload.version();
    }

    public static Optional<BingoCardPayload> latestCard() {
        return Optional.ofNullable(latestCard);
    }

    static void accept(BingoCardPayload payload) {
        latestCard = payload;
    }

    public static void clear() {
        negotiatedVersion = 0;
        latestCard = null;
        hudVisible = true;
    }

    public static boolean hudVisible() {
        return hudVisible;
    }

    public static void toggleHud() {
        hudVisible = !hudVisible;
    }
}
