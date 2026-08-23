package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.NeoBingo;
import dev.cleanroom.neobingo.network.ClientProtocolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** 在增强客户端右上角绘制最近同步的宾果卡。 */
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
        if (minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        ClientProtocolState.latestCard().ifPresent(card -> draw(event.getGuiGraphics(), minecraft.font,
                card.team() + " · " + card.mode(), card.rows()));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientProtocolState.clear();
    }

    private static void draw(GuiGraphics graphics, Font font, String title, java.util.List<String> rows) {
        int width = Math.max(font.width(title), rows.stream().mapToInt(font::width).max().orElse(0));
        int panelWidth = width + PADDING * 2;
        int panelHeight = (rows.size() + 1) * LINE_HEIGHT + PADDING * 2;
        int left = graphics.guiWidth() - panelWidth - 6;
        int top = 6;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, BACKGROUND);
        graphics.drawString(font, title, left + PADDING, top + PADDING, TITLE_COLOR, false);
        for (int index = 0; index < rows.size(); index++) {
            graphics.drawString(font, rows.get(index), left + PADDING,
                    top + PADDING + (index + 1) * LINE_HEIGHT, TEXT_COLOR, false);
        }
    }
}
