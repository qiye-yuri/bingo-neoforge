package dev.cleanroom.neobingo;

import com.mojang.logging.LogUtils;
import dev.cleanroom.neobingo.config.BingoCardDefinitions;
import dev.cleanroom.neobingo.network.NeoBingoNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(NeoBingo.MOD_ID)
public final class NeoBingo {
    public static final String MOD_ID = "neo_bingo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NeoBingo(IEventBus modBus, ModContainer container) {
        modBus.addListener(NeoBingoGameTests::register);
        modBus.addListener(NeoBingoNetwork::register);
        NeoForge.EVENT_BUS.addListener(BingoCardDefinitions::registerReloadListener);
        NeoForge.EVENT_BUS.addListener(NeoBingoPermissions::register);
        NeoForge.EVENT_BUS.addListener(NeoBingoCommands::register);
        NeoForge.EVENT_BUS.addListener(InventoryClaimTicker::onServerTick);
        NeoForge.EVENT_BUS.addListener(RankedCountdownTicker::onServerTick);
        NeoForge.EVENT_BUS.addListener(NeoBingoNetwork::onPlayerLogin);
        LOGGER.info("Neo Bingo initialized");
    }
}
