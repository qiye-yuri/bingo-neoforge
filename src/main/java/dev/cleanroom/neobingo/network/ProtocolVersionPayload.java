package dev.cleanroom.neobingo.network;

import dev.cleanroom.neobingo.NeoBingo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 声明可选客户端增强功能所使用的应用协议版本。 */
public record ProtocolVersionPayload(int version) implements CustomPacketPayload {
    public static final int CURRENT_VERSION = 1;
    public static final Type<ProtocolVersionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoBingo.MOD_ID, "protocol_version"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProtocolVersionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeVarInt(payload.version()),
                    buffer -> new ProtocolVersionPayload(buffer.readVarInt()));

    public ProtocolVersionPayload {
        if (version < 1) {
            throw new IllegalArgumentException("协议版本必须为正数");
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
