package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.network.BingoCardPayload;
import dev.cleanroom.neobingo.network.ClientProtocolState;
import dev.cleanroom.neobingo.presentation.BingoModeText;
import dev.cleanroom.neobingo.presentation.BingoObjectiveText;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 以紧凑的图标网格展示服务端同步的 Bingo 卡快照。 */
public final class BingoCardScreen extends Screen {
    private static final int HEADER_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 20;
    private final BingoCardPayload initialCard;
    private BingoCardPayload displayedCard;
    private int gridLeft;
    private int gridTop;
    private int cellSize;
    private int columns;

    public BingoCardScreen(BingoCardPayload card) {
        super(Component.translatable("screen.neo_bingo.card"));
        initialCard = card;
        displayedCard = card;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        displayedCard = ClientProtocolState.latestCard().orElse(initialCard);
        List<List<BingoCardVisuals.Cell>> grid = BingoCardVisuals.parse(displayedCard.rows());
        columns = grid.stream().mapToInt(List::size).max().orElse(1);
        int rows = grid.size();
        cellSize = Math.clamp(Math.min(
                (width - 32) / columns,
                (height - HEADER_HEIGHT - FOOTER_HEIGHT - 32) / rows), 22, 48);
        int gridWidth = columns * cellSize;
        int gridHeight = rows * cellSize;
        int panelWidth = gridWidth + 12;
        int panelHeight = HEADER_HEIGHT + gridHeight + FOOTER_HEIGHT + 8;
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        gridLeft = left + 6;
        gridTop = top + HEADER_HEIGHT;
        int accent = BingoCardVisuals.teamColor(displayedCard.team());

        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xC0000000);
        graphics.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, accent);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, BingoCardVisuals.PANEL);
        graphics.drawCenteredString(font, Component.translatable("screen.neo_bingo.card.title",
                displayedCard.team(), BingoModeText.displayName(displayedCard.mode())), width / 2, top + 9, accent);

        BingoCardVisuals.Cell hovered = null;
        int index = 0;
        for (int row = 0; row < grid.size(); row++) {
            for (int column = 0; column < grid.get(row).size(); column++) {
                BingoCardVisuals.Cell cell = grid.get(row).get(column);
                int x = gridLeft + column * cellSize;
                int y = gridTop + row * cellSize;
                drawCell(graphics, cell, x, y, index);
                if (mouseX >= x && mouseX < x + cellSize && mouseY >= y && mouseY < y + cellSize) {
                    hovered = cell;
                }
                index++;
            }
        }
        graphics.drawCenteredString(font, Component.translatable("screen.neo_bingo.card.hint"), width / 2,
                top + panelHeight - 14, BingoCardVisuals.MUTED_TEXT);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) {
            graphics.renderTooltip(font, Component.literal(hovered.label()), mouseX, mouseY);
        }
    }

    private void drawCell(GuiGraphics graphics, BingoCardVisuals.Cell cell, int x, int y, int index) {
        int background = cell.claimed() ? BingoCardVisuals.CLAIMED
                : cell.hidden() ? BingoCardVisuals.HIDDEN
                : ((index / columns + index % columns) & 1) == 0
                        ? BingoCardVisuals.CELL : BingoCardVisuals.CELL_DARK;
        graphics.fill(x + 1, y + 1, x + cellSize - 1, y + cellSize - 1, background);
        if (ClientProtocolState.focusedCell() == index) {
            graphics.renderOutline(x + 1, y + 1, cellSize - 2, cellSize - 2, BingoCardVisuals.FOCUSED);
        }
        if (cell.hidden()) {
            graphics.drawCenteredString(font, "?", x + cellSize / 2, y + (cellSize - font.lineHeight) / 2,
                    BingoCardVisuals.MUTED_TEXT);
            return;
        }
        BingoObjectiveText.itemForCell(cell.raw()).ifPresent(item -> graphics.renderItem(
                item.getDefaultInstance(), x + (cellSize - 16) / 2, y + (cellSize - 16) / 2));
        if (cell.claimed()) {
            graphics.fill(x + 3, y + 3, x + cellSize - 3, y + cellSize - 3, BingoCardVisuals.CLAIMED_OVERLAY);
            graphics.drawString(font, "✓", x + cellSize - 10, y + 3, BingoCardVisuals.TEXT, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= gridLeft && mouseY >= gridTop) {
            int row = (int) (mouseY - gridTop) / cellSize;
            int column = (int) (mouseX - gridLeft) / cellSize;
            List<List<BingoCardVisuals.Cell>> grid = BingoCardVisuals.parse(displayedCard.rows());
            if (row >= 0 && row < grid.size() && column >= 0 && column < grid.get(row).size()) {
                ClientProtocolState.toggleFocusedCell(row * columns + column);
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
        int current = ClientProtocolState.focusedCell();
        if (current < 0) {
            ClientProtocolState.focusCell(0);
            return true;
        }
        int row = Math.clamp(current / columns + rowOffset, 0, displayedCard.rows().size() - 1);
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
