package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.network.BingoCardPayload;
import dev.cleanroom.neobingo.network.ClientProtocolState;
import dev.cleanroom.neobingo.presentation.BingoModeText;
import dev.cleanroom.neobingo.presentation.BingoObjectiveText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 以居中面板展示服务端同步的 Bingo 卡快照。 */
public final class BingoCardScreen extends Screen {
    private static final int PANEL_WIDTH = 700;
    private static final int CELL_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 18;
    private static final int BACKGROUND = 0xE0182029;
    private static final int BORDER = 0xFFF2C94C;
    private static final int TEXT = 0xFFF2F2F2;
    private static final int CLAIMED = 0xD0286B45;
    private static final int UNCLAIMED = 0xD0242D38;
    private static final int HIDDEN = 0xD0161B22;
    private static final int FOCUSED = 0xFFFFFFFF;
    private final BingoCardPayload initialCard;
    private BingoCardPayload displayedCard;
    private int gridLeft;
    private int gridTop;
    private int gridWidth;
    private int columns;

    public BingoCardScreen(BingoCardPayload card) {
        super(Component.translatable("screen.neo_bingo.card"));
        this.initialCard = card;
        this.displayedCard = card;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        displayedCard = ClientProtocolState.latestCard().orElse(initialCard);
        int width = Math.min(PANEL_WIDTH, this.width - 24);
        java.util.List<String[]> grid = displayedCard.rows().stream()
                .map(row -> row.split(" \\| ", -1))
                .toList();
        columns = grid.stream().mapToInt(row -> row.length).max().orElse(1);
        int height = 38 + displayedCard.rows().size() * CELL_HEIGHT + FOOTER_HEIGHT;
        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;
        gridLeft = left;
        gridTop = top + 32;
        gridWidth = width;
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, BORDER);
        graphics.fill(left, top, left + width, top + height, BACKGROUND);
        graphics.drawCenteredString(font,
                Component.translatable("screen.neo_bingo.card.title",
                        displayedCard.team(), BingoModeText.displayName(displayedCard.mode())),
                this.width / 2, top + 10, BORDER);
        int cellWidth = width / columns;
        String hovered = null;
        int cellIndexBase = 0;
        for (int rowIndex = 0; rowIndex < grid.size(); rowIndex++) {
            String[] row = grid.get(rowIndex);
            for (int column = 0; column < row.length; column++) {
                String value = row[column];
                String displayValue = BingoObjectiveText.displayCell(value);
                int cellLeft = left + column * cellWidth;
                int cellTop = top + 32 + rowIndex * CELL_HEIGHT;
                int color = value.startsWith("[✓]") ? CLAIMED : value.contains("???") ? HIDDEN : UNCLAIMED;
                graphics.fill(cellLeft + 1, cellTop + 1,
                        cellLeft + cellWidth - 1, cellTop + CELL_HEIGHT - 1, color);
                int cellIndex = cellIndexBase + column;
                if (ClientProtocolState.focusedCell() == cellIndex) {
                    graphics.renderOutline(cellLeft + 1, cellTop + 1,
                            cellWidth - 2, CELL_HEIGHT - 2, FOCUSED);
                }
                var item = BingoObjectiveText.itemForCell(value);
                boolean drawIcon = item.isPresent() && cellWidth >= 40;
                int textOffset = drawIcon ? 24 : 4;
                if (drawIcon) {
                    graphics.renderItem(item.orElseThrow().getDefaultInstance(), cellLeft + 4, cellTop + 6);
                }
                String visible = font.plainSubstrByWidth(
                        displayValue, Math.max(1, cellWidth - textOffset - 4));
                graphics.drawString(font, visible, cellLeft + textOffset, cellTop + 10, TEXT, false);
                if (mouseX >= cellLeft && mouseX < cellLeft + cellWidth
                        && mouseY >= cellTop && mouseY < cellTop + CELL_HEIGHT) {
                    hovered = displayValue;
                }
            }
            cellIndexBase += row.length;
        }
        String hint = font.plainSubstrByWidth(
                Component.translatable("screen.neo_bingo.card.hint").getString(), Math.max(1, width - 8));
        graphics.drawCenteredString(font, hint, this.width / 2, top + height - 13, TEXT);
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
            java.util.List<String[]> grid = displayedCard.rows().stream()
                    .map(value -> value.split(" \\| ", -1))
                    .toList();
            if (row >= 0 && row < grid.size() && column >= 0 && column < grid.get(row).length) {
                int cellIndex = grid.stream().limit(row).mapToInt(cells -> cells.length).sum() + column;
                ClientProtocolState.toggleFocusedCell(cellIndex);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> moveFocus(0, -1);
            case GLFW.GLFW_KEY_RIGHT -> moveFocus(0, 1);
            case GLFW.GLFW_KEY_UP -> moveFocus(-1, 0);
            case GLFW.GLFW_KEY_DOWN -> moveFocus(1, 0);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> toggleKeyboardFocus();
            default -> super.keyPressed(keyCode, scanCode, modifiers);
        };
    }

    private boolean moveFocus(int rowOffset, int columnOffset) {
        int rowCount = displayedCard.rows().size();
        int current = ClientProtocolState.focusedCell();
        if (current < 0) {
            ClientProtocolState.focusCell(0);
            return true;
        }
        int row = Math.clamp(current / columns + rowOffset, 0, rowCount - 1);
        int column = Math.clamp(current % columns + columnOffset, 0, columns - 1);
        ClientProtocolState.focusCell(row * columns + column);
        return true;
    }

    private boolean toggleKeyboardFocus() {
        int current = ClientProtocolState.focusedCell();
        ClientProtocolState.toggleFocusedCell(current < 0 ? 0 : current);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
