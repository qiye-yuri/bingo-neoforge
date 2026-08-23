package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.NeoBingo;
import dev.cleanroom.neobingo.network.ClientProtocolState;
import dev.cleanroom.neobingo.presentation.BingoModeText;
import dev.cleanroom.neobingo.presentation.BingoObjectiveText;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** 在增强客户端右上角绘制紧凑的 Bingo 图标卡。 */
@EventBusSubscriber(modid = NeoBingo.MOD_ID, value = Dist.CLIENT)
public final class ClientBingoHud {
    private static final int CELL_SIZE = 20;
    private static final int HEADER_HEIGHT = 18;

    private ClientBingoHud() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui || !ClientProtocolState.hudVisible()) {
            return;
        }
        ClientProtocolState.latestCard().ifPresent(card -> {
            List<List<BingoCardVisuals.Cell>> grid = BingoCardVisuals.parse(card.rows());
            int columns = grid.stream().mapToInt(List::size).max().orElse(1);
            int panelWidth = columns * CELL_SIZE + 8;
            int panelHeight = grid.size() * CELL_SIZE + HEADER_HEIGHT + 6;
            int left = event.getGuiGraphics().guiWidth() - panelWidth - 6;
            int top = 6;
            int accent = BingoCardVisuals.teamColor(card.team());
            GuiGraphics graphics = event.getGuiGraphics();
            graphics.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, 0x90000000);
            graphics.fill(left, top, left + panelWidth, top + panelHeight, BingoCardVisuals.PANEL);
            graphics.fill(left, top, left + 3, top + panelHeight, accent);
            String title = card.team() + " · " + BingoModeText.displayName(card.mode()).getString();
            graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(title, panelWidth - 10),
                    left + 6, top + 5, accent, false);
            int index = 0;
            for (int row = 0; row < grid.size(); row++) {
                for (int column = 0; column < grid.get(row).size(); column++) {
                    BingoCardVisuals.Cell cell = grid.get(row).get(column);
                    int x = left + 5 + column * CELL_SIZE;
                    int y = top + HEADER_HEIGHT + row * CELL_SIZE;
                    int color = cell.claimed() ? BingoCardVisuals.CLAIMED
                            : cell.hidden() ? BingoCardVisuals.HIDDEN : BingoCardVisuals.CELL;
                    graphics.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, color);
                    if (cell.hidden()) {
                        graphics.drawCenteredString(minecraft.font, "?", x + CELL_SIZE / 2, y + 6,
                                BingoCardVisuals.MUTED_TEXT);
                    } else {
                        BingoObjectiveText.itemForCell(cell.raw()).ifPresent(item ->
                                graphics.renderItem(item.getDefaultInstance(), x + 2, y + 2));
                    }
                    if (cell.claimed()) {
                        graphics.fill(x + 2, y + 2, x + CELL_SIZE - 2, y + CELL_SIZE - 2,
                                BingoCardVisuals.CLAIMED_OVERLAY);
                    }
                    if (ClientProtocolState.focusedCell() == index) {
                        graphics.renderOutline(x, y, CELL_SIZE, CELL_SIZE, BingoCardVisuals.FOCUSED);
                    }
                    index++;
                }
            }
        });
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientProtocolState.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientKeyMappings.OPEN_CARD.consumeClick()) {
            ClientProtocolState.latestCard().ifPresent(card -> minecraft.setScreen(new BingoCardScreen(card)));
        }
        while (ClientKeyMappings.TOGGLE_HUD.consumeClick()) {
            ClientProtocolState.toggleHud();
        }
    }
}
