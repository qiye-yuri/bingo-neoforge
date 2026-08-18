package dev.cleanroom.neobingo;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(NeoBingo.MOD_ID)
public final class NeoBingo {
    public static final String MOD_ID = "neo_bingo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NeoBingo(IEventBus modBus, ModContainer container) {
        LOGGER.info("Neo Bingo initialized");
    }
}
