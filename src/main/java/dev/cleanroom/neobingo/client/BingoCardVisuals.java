package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.presentation.BingoObjectiveText;
import java.util.List;

/** 为完整卡片与 HUD 提供统一的显示属性。 */
final class BingoCardVisuals {
    static final int PANEL = 0xFF17191F;
    static final int CELL = 0xFF292C34;
    static final int CELL_DARK = 0xFF202229;
    static final int CLAIMED = 0xFF3B9B5F;
    static final int CLAIMED_OVERLAY = 0x603BFF76;
    static final int HIDDEN = 0xFF121318;
    static final int TEXT = 0xFFF4F4F4;
    static final int MUTED_TEXT = 0xFFB8BBC4;
    static final int FOCUSED = 0xFFFFFFFF;

    private BingoCardVisuals() {
    }

    static int teamColor(String team) {
        return switch (team) {
            case "red" -> 0xFFE05252;
            case "blue" -> 0xFF4D7FE8;
            case "green" -> 0xFF49A85D;
            case "yellow" -> 0xFFE2C94C;
            case "purple" -> 0xFF9B65D4;
            case "orange" -> 0xFFE58A3B;
            case "cyan" -> 0xFF3BBAC5;
            case "pink" -> 0xFFE56FA7;
            default -> 0xFFF2C94C;
        };
    }

    static List<List<Cell>> parse(List<String> rows) {
        return rows.stream()
                .map(row -> List.of(row.split(" \\| ", -1)).stream().map(Cell::from).toList())
                .toList();
    }

    record Cell(String raw, String label, boolean claimed, boolean hidden) {
        private static Cell from(String raw) {
            return new Cell(raw, BingoObjectiveText.displayCell(raw),
                    raw.startsWith("[✓]"), raw.contains("???"));
        }
    }
}
