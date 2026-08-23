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
    private static final int MAX_TEAM_LENGTH = 32;
    private static final int MAX_MODE_LENGTH = 16;
    private static final int MAX_ROW_LENGTH = 2048;
    private static final int MAX_ROWS = 9;
    private static final int MAX_COLUMNS = 9;
    public static final Type<BingoCardPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoBingo.MOD_ID, "bingo_card"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BingoCardPayload> STREAM_CODEC = StreamCodec.of(
            BingoCardPayload::encode,
            BingoCardPayload::decode);

    public BingoCardPayload {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(mode, "mode");
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        if (!team.matches("[a-z][a-z0-9_-]{0,31}")) {
            throw new IllegalArgumentException("队伍标识不符合协议约束");
        }
        if (!mode.matches("[A-Z_]{1," + MAX_MODE_LENGTH + "}")) {
            throw new IllegalArgumentException("模式标识不符合协议约束");
        }
        if (rows.isEmpty() || rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("卡片行数或内容无效");
        }
        int columns = columnCount(rows.getFirst());
        if (columns < 1 || columns > MAX_COLUMNS || rows.stream().anyMatch(row ->
                row.isBlank() || row.length() > MAX_ROW_LENGTH || columnCount(row) != columns)) {
            throw new IllegalArgumentException("卡片必须是大小受限的规则网格");
        }
    }

    private static void encode(RegistryFriendlyByteBuf buffer, BingoCardPayload payload) {
        buffer.writeUtf(payload.team(), MAX_TEAM_LENGTH);
        buffer.writeUtf(payload.mode(), MAX_MODE_LENGTH);
        buffer.writeVarInt(payload.rows().size());
        payload.rows().forEach(row -> buffer.writeUtf(row, MAX_ROW_LENGTH));
    }

    private static BingoCardPayload decode(RegistryFriendlyByteBuf buffer) {
        String team = buffer.readUtf(MAX_TEAM_LENGTH);
        String mode = buffer.readUtf(MAX_MODE_LENGTH);
        int rowCount = buffer.readVarInt();
        if (rowCount < 1 || rowCount > MAX_ROWS) {
            throw new IllegalArgumentException("卡片行数超出协议限制");
        }
        List<String> rows = new ArrayList<>(rowCount);
        for (int index = 0; index < rowCount; index++) {
            rows.add(buffer.readUtf(MAX_ROW_LENGTH));
        }
        return new BingoCardPayload(team, mode, rows);
    }

    private static int columnCount(String row) {
        return row.split(" \\| ", -1).length;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
