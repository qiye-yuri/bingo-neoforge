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
    private static final int DEFAULT_HUD_KEY = 72;
    private static final int DEFAULT_POSITION_KEY = 80;
    private static final int DEFAULT_SCALE_DOWN_KEY = 91;
    private static final int DEFAULT_SCALE_UP_KEY = 93;
    private static final int DEFAULT_TEAM_CHEST_KEY = 71;
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.neo_bingo.toggle_hud",
            DEFAULT_HUD_KEY,
            "key.categories.neo_bingo");
    public static final KeyMapping CYCLE_HUD_POSITION = new KeyMapping(
            "key.neo_bingo.hud_position", DEFAULT_POSITION_KEY, "key.categories.neo_bingo");
    public static final KeyMapping DECREASE_HUD_SCALE = new KeyMapping(
            "key.neo_bingo.hud_scale_down", DEFAULT_SCALE_DOWN_KEY, "key.categories.neo_bingo");
    public static final KeyMapping INCREASE_HUD_SCALE = new KeyMapping(
            "key.neo_bingo.hud_scale_up", DEFAULT_SCALE_UP_KEY, "key.categories.neo_bingo");
    public static final KeyMapping OPEN_TEAM_CHEST = new KeyMapping(
            "key.neo_bingo.open_team_chest", DEFAULT_TEAM_CHEST_KEY, "key.categories.neo_bingo");

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HUD);
        event.register(CYCLE_HUD_POSITION);
        event.register(DECREASE_HUD_SCALE);
        event.register(INCREASE_HUD_SCALE);
        event.register(OPEN_TEAM_CHEST);
    }
}
