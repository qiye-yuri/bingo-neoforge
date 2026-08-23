package dev.cleanroom.neobingo.network;

/** 保存当前客户端连接已确认的增强协议版本。 */
public final class ClientProtocolState {
    private static volatile int negotiatedVersion;

    private ClientProtocolState() {
    }

    public static int negotiatedVersion() {
        return negotiatedVersion;
    }

    static void accept(ProtocolVersionPayload payload) {
        negotiatedVersion = payload.version();
    }
}
