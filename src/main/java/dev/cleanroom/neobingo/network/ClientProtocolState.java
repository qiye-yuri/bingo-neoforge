package dev.cleanroom.neobingo.network;

import java.util.Optional;

/** 保存当前客户端连接已确认的增强协议版本。 */
public final class ClientProtocolState {
    private static volatile int negotiatedVersion;
    private static volatile BingoCardPayload latestCard;
    private static volatile boolean hudVisible = true;
    private static volatile int focusedCell = -1;

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
        if (focusedCell >= cellCount(payload)) {
            focusedCell = -1;
        }
    }

    public static void clear() {
        negotiatedVersion = 0;
        latestCard = null;
        hudVisible = true;
        focusedCell = -1;
    }

    public static boolean hudVisible() {
        return hudVisible;
    }

    public static void toggleHud() {
        hudVisible = !hudVisible;
    }

    public static int focusedCell() {
        return focusedCell;
    }

    public static void toggleFocusedCell(int cellIndex) {
        validateCellIndex(cellIndex);
        focusedCell = focusedCell == cellIndex ? -1 : cellIndex;
    }

    public static void focusCell(int cellIndex) {
        validateCellIndex(cellIndex);
        focusedCell = cellIndex;
    }

    public static Optional<String> focusedObjective() {
        if (latestCard == null || focusedCell < 0) {
            return Optional.empty();
        }
        int remaining = focusedCell;
        for (String row : latestCard.rows()) {
            String[] cells = row.split(" \\| ", -1);
            if (remaining < cells.length) {
                return Optional.of(cells[remaining].replaceFirst("^\\[(?: |✓)]\\s*", ""));
            }
            remaining -= cells.length;
        }
        return Optional.empty();
    }

    private static int cellCount(BingoCardPayload payload) {
        return payload.rows().stream()
                .mapToInt(row -> row.split(" \\| ", -1).length)
                .sum();
    }

    private static void validateCellIndex(int cellIndex) {
        if (latestCard == null || cellIndex < 0 || cellIndex >= cellCount(latestCard)) {
            throw new IllegalArgumentException("格子索引超出当前卡片范围");
        }
    }
}
