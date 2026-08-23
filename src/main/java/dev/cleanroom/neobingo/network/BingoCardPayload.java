package dev.cleanroom.neobingo.network;

import dev.cleanroom.neobingo.NeoBingo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 向增强客户端同步可直接展示的宾果卡文本快照。 */
public record BingoCardPayload(String team, String mode, List<String> rows) implements CustomPacketPayload {
    private static final int MAX_TEXT_LENGTH = 32767;
    private static final int MAX_ROWS = 9;
    public static final Type<BingoCardPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoBingo.MOD_ID, "bingo_card"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BingoCardPayload> STREAM_CODEC = StreamCodec.of(
            BingoCardPayload::encode,
            BingoCardPayload::decode);

    public BingoCardPayload {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(mode, "mode");
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        if (team.isBlank() || mode.isBlank()) {
            throw new IllegalArgumentException("队伍和模式不能为空");
        }
        if (rows.isEmpty() || rows.size() > MAX_ROWS || rows.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("卡片行数或内容无效");
        }
    }

    private static void encode(RegistryFriendlyByteBuf buffer, BingoCardPayload payload) {
        buffer.writeUtf(payload.team(), MAX_TEXT_LENGTH);
        buffer.writeUtf(payload.mode(), MAX_TEXT_LENGTH);
        buffer.writeVarInt(payload.rows().size());
        payload.rows().forEach(row -> buffer.writeUtf(row, MAX_TEXT_LENGTH));
    }

    private static BingoCardPayload decode(RegistryFriendlyByteBuf buffer) {
        String team = buffer.readUtf(MAX_TEXT_LENGTH);
        String mode = buffer.readUtf(MAX_TEXT_LENGTH);
        int rowCount = buffer.readVarInt();
        if (rowCount < 1 || rowCount > MAX_ROWS) {
            throw new IllegalArgumentException("卡片行数超出协议限制");
        }
        List<String> rows = new ArrayList<>(rowCount);
        for (int index = 0; index < rowCount; index++) {
            rows.add(buffer.readUtf(MAX_TEXT_LENGTH));
        }
        return new BingoCardPayload(team, mode, rows);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
