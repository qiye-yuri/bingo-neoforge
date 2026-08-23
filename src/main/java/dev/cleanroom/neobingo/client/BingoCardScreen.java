package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.network.BingoCardPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 以居中面板展示服务端同步的宾果卡快照。 */
public final class BingoCardScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int LINE_HEIGHT = 14;
    private static final int BACKGROUND = 0xE0182029;
    private static final int BORDER = 0xFFF2C94C;
    private static final int TEXT = 0xFFF2F2F2;
    private final BingoCardPayload card;

    public BingoCardScreen(BingoCardPayload card) {
        super(Component.translatable("screen.neo_bingo.card"));
        this.card = card;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int width = Math.min(PANEL_WIDTH, this.width - 24);
        int height = 42 + card.rows().size() * LINE_HEIGHT;
        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, BORDER);
        graphics.fill(left, top, left + width, top + height, BACKGROUND);
        graphics.drawCenteredString(font,
                Component.translatable("screen.neo_bingo.card.title", card.team(), card.mode()),
                this.width / 2, top + 10, BORDER);
        for (int index = 0; index < card.rows().size(); index++) {
            graphics.drawString(font, card.rows().get(index), left + 10,
                    top + 28 + index * LINE_HEIGHT, TEXT, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
