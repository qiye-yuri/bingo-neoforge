package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.NeoBingo;
import dev.cleanroom.neobingo.network.ClientProtocolState;
import dev.cleanroom.neobingo.presentation.BingoModeText;
import dev.cleanroom.neobingo.presentation.BingoObjectiveText;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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
    private static final int HEADER_HEIGHT = 29;

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
            float scale = BingoClientConfig.scale();
            int scaledWidth = Math.round(panelWidth * scale);
            int scaledHeight = Math.round(panelHeight * scale);
            int left = left(BingoClientConfig.corner(), event.getGuiGraphics().guiWidth(), scaledWidth);
            int top = top(BingoClientConfig.corner(), event.getGuiGraphics().guiHeight(), scaledHeight);
            int accent = BingoCardVisuals.teamColor(card.team());
            GuiGraphics graphics = event.getGuiGraphics();
            graphics.pose().pushPose();
            graphics.pose().translate(left, top, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.fill(-1, -1, panelWidth + 1, panelHeight + 1, 0x90000000);
            graphics.fill(0, 0, panelWidth, panelHeight, BingoCardVisuals.PANEL);
            graphics.fill(0, 0, 3, panelHeight, accent);
            String title = card.team() + " · " + BingoModeText.displayName(card.mode()).getString();
            graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(title, panelWidth - 10),
                    6, 5, accent, false);
            graphics.drawString(minecraft.font, status(card.score(), card.remainingSeconds()),
                    6, 16, BingoCardVisuals.MUTED_TEXT, false);
            int index = 0;
            for (int row = 0; row < grid.size(); row++) {
                for (int column = 0; column < grid.get(row).size(); column++) {
                    BingoCardVisuals.Cell cell = grid.get(row).get(column);
                    int x = 5 + column * CELL_SIZE;
                    int y = HEADER_HEIGHT + row * CELL_SIZE;
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
            graphics.pose().popPose();
        });
    }

    private static Component status(int score, long remainingSeconds) {
        return remainingSeconds >= 0
                ? Component.translatable("hud.neo_bingo.status.timed", score, formatTime(remainingSeconds))
                : Component.translatable("hud.neo_bingo.status.score", score);
    }

    private static String formatTime(long seconds) {
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }

    private static int left(BingoClientConfig.HudCorner corner, int screenWidth, int panelWidth) {
        return switch (corner) {
            case TOP_LEFT, BOTTOM_LEFT -> 6;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - panelWidth - 6;
        };
    }

    private static int top(BingoClientConfig.HudCorner corner, int screenHeight, int panelHeight) {
        return switch (corner) {
            case TOP_LEFT, TOP_RIGHT -> 6;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - panelHeight - 6;
        };
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientProtocolState.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientKeyMappings.TOGGLE_HUD.consumeClick()) {
            ClientProtocolState.toggleHud();
        }
        while (ClientKeyMappings.CYCLE_HUD_POSITION.consumeClick()) {
            BingoClientConfig.HudCorner corner = BingoClientConfig.cycleCorner();
            showSetting(minecraft, Component.translatable("hud.neo_bingo.position.changed",
                    Component.translatable("hud.neo_bingo.position." + corner.name().toLowerCase())));
        }
        while (ClientKeyMappings.DECREASE_HUD_SCALE.consumeClick()) {
            showSetting(minecraft, Component.translatable(
                    "hud.neo_bingo.scale.changed", BingoClientConfig.adjustScale(-10)));
        }
        while (ClientKeyMappings.INCREASE_HUD_SCALE.consumeClick()) {
            showSetting(minecraft, Component.translatable(
                    "hud.neo_bingo.scale.changed", BingoClientConfig.adjustScale(10)));
        }
    }

    private static void showSetting(Minecraft minecraft, Component message) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }
}
