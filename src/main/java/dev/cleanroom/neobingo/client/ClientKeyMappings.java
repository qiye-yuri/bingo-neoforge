package dev.cleanroom.neobingo.client;

import dev.cleanroom.neobingo.NeoBingo;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** 注册增强客户端可重新绑定的按键。 */
@EventBusSubscriber(modid = NeoBingo.MOD_ID, value = Dist.CLIENT)
public final class ClientKeyMappings {
    private static final int DEFAULT_CARD_KEY = 66;
    private static final int DEFAULT_HUD_KEY = 72;
    public static final KeyMapping OPEN_CARD = new KeyMapping(
            "key.neo_bingo.open_card",
            DEFAULT_CARD_KEY,
            "key.categories.neo_bingo");
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.neo_bingo.toggle_hud",
            DEFAULT_HUD_KEY,
            "key.categories.neo_bingo");

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CARD);
        event.register(TOGGLE_HUD);
    }
}
