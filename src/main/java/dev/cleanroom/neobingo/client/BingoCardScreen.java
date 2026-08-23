package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.network.BingoCardPayload;
import dev.cleanroom.neobingo.network.ClientProtocolState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 以居中面板展示服务端同步的宾果卡快照。 */
public final class BingoCardScreen extends Screen {
    private static final int PANEL_WIDTH = 700;
    private static final int CELL_HEIGHT = 28;
    private static final int BACKGROUND = 0xE0182029;
    private static final int BORDER = 0xFFF2C94C;
    private static final int TEXT = 0xFFF2F2F2;
    private static final int CLAIMED = 0xD0286B45;
    private static final int UNCLAIMED = 0xD0242D38;
    private static final int HIDDEN = 0xD0161B22;
    private static final int FOCUSED = 0xFFFFFFFF;
    private final BingoCardPayload card;
    private int gridLeft;
    private int gridTop;
    private int gridWidth;
    private int columns;

    public BingoCardScreen(BingoCardPayload card) {
        super(Component.translatable("screen.neo_bingo.card"));
        this.card = card;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int width = Math.min(PANEL_WIDTH, this.width - 24);
        java.util.List<String[]> grid = card.rows().stream()
                .map(row -> row.split(" \\| ", -1))
                .toList();
        columns = grid.stream().mapToInt(row -> row.length).max().orElse(1);
        int height = 38 + card.rows().size() * CELL_HEIGHT;
        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;
        gridLeft = left;
        gridTop = top + 32;
        gridWidth = width;
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, BORDER);
        graphics.fill(left, top, left + width, top + height, BACKGROUND);
        graphics.drawCenteredString(font,
                Component.translatable("screen.neo_bingo.card.title", card.team(), card.mode()),
                this.width / 2, top + 10, BORDER);
        int cellWidth = width / columns;
        String hovered = null;
        for (int rowIndex = 0; rowIndex < grid.size(); rowIndex++) {
            String[] row = grid.get(rowIndex);
            for (int column = 0; column < row.length; column++) {
                String value = row[column];
                int cellLeft = left + column * cellWidth;
                int cellTop = top + 32 + rowIndex * CELL_HEIGHT;
                int color = value.startsWith("[✓]") ? CLAIMED : value.contains("???") ? HIDDEN : UNCLAIMED;
                graphics.fill(cellLeft + 1, cellTop + 1,
                        cellLeft + cellWidth - 1, cellTop + CELL_HEIGHT - 1, color);
                int cellIndex = rowIndex * columns + column;
                if (ClientProtocolState.focusedCell() == cellIndex) {
                    graphics.renderOutline(cellLeft + 1, cellTop + 1,
                            cellWidth - 2, CELL_HEIGHT - 2, FOCUSED);
                }
                String visible = font.plainSubstrByWidth(value, Math.max(1, cellWidth - 8));
                graphics.drawString(font, visible, cellLeft + 4, cellTop + 10, TEXT, false);
                if (mouseX >= cellLeft && mouseX < cellLeft + cellWidth
                        && mouseY >= cellTop && mouseY < cellTop + CELL_HEIGHT) {
                    hovered = value;
                }
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) {
            graphics.renderTooltip(font, Component.literal(hovered), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= gridLeft && mouseX < gridLeft + gridWidth && mouseY >= gridTop) {
            int row = (int) (mouseY - gridTop) / CELL_HEIGHT;
            int column = (int) (mouseX - gridLeft) / Math.max(1, gridWidth / columns);
            if (row >= 0 && row < card.rows().size() && column >= 0 && column < columns) {
                ClientProtocolState.toggleFocusedCell(row * columns + column);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
