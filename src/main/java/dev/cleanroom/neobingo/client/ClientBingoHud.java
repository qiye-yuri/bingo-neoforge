package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.NeoBingo;
import dev.cleanroom.neobingo.network.ClientProtocolState;
import dev.cleanroom.neobingo.presentation.BingoModeText;
import dev.cleanroom.neobingo.presentation.BingoObjectiveText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 在增强客户端右上角绘制最近同步的 Bingo 卡。 */
@EventBusSubscriber(modid = NeoBingo.MOD_ID, value = Dist.CLIENT)
public final class ClientBingoHud {
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND = 0xB010141A;
    private static final int TITLE_COLOR = 0xFFF2C94C;
    private static final int TEXT_COLOR = 0xFFF2F2F2;

    private ClientBingoHud() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui || !ClientProtocolState.hudVisible()) {
            return;
        }
        ClientProtocolState.latestCard().ifPresent(card -> {
            java.util.List<String> rows = card.rows().stream()
                    .map(BingoObjectiveText::displayRow)
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            ClientProtocolState.focusedObjective().ifPresent(objective -> rows.add(
                    Component.translatable(
                            "hud.neo_bingo.focused", BingoObjectiveText.displayObjective(objective)).getString()));
            draw(event.getGuiGraphics(), minecraft.font,
                    card.team() + " · " + BingoModeText.displayName(card.mode()).getString(), rows);
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

    private static void draw(GuiGraphics graphics, Font font, String title, java.util.List<String> rows) {
        int maximumTextWidth = Math.max(120, graphics.guiWidth() * 45 / 100);
        java.util.List<String> visibleRows = rows.stream()
                .map(row -> font.plainSubstrByWidth(row, maximumTextWidth))
                .toList();
        String visibleTitle = font.plainSubstrByWidth(title, maximumTextWidth);
        int width = Math.max(font.width(visibleTitle), visibleRows.stream().mapToInt(font::width).max().orElse(0));
        int panelWidth = width + PADDING * 2;
        int panelHeight = (visibleRows.size() + 1) * LINE_HEIGHT + PADDING * 2;
        int left = graphics.guiWidth() - panelWidth - 6;
        int top = 6;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, BACKGROUND);
        graphics.drawString(font, visibleTitle, left + PADDING, top + PADDING, TITLE_COLOR, false);
        for (int index = 0; index < visibleRows.size(); index++) {
            graphics.drawString(font, visibleRows.get(index), left + PADDING,
                    top + PADDING + (index + 1) * LINE_HEIGHT, TEXT_COLOR, false);
        }
    }
}
