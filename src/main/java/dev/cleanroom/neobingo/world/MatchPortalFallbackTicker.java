package dev.cleanroom.neobingo.world;

import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 原版传送计时未触发时，为运行时比赛维度提供黑曜石传送门兜底。 */
public final class MatchPortalFallbackTicker {
    private static final int FALLBACK_TICKS = 100;
    private static final Map<UUID, PortalStay> STAYS = new HashMap<>();

    private MatchPortalFallbackTicker() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var group = RuntimeMatchWorldManager.active().orElse(null);
        boolean participant = NeoBingoSavedData.get(player.getServer()).restoreSession()
                .flatMap(session -> session.roster().teamOf(new PlayerId(player.getUUID()))).isPresent();
        if (group == null || !participant || !group.levels().contains(player.serverLevel()) || player.isOnPortalCooldown()) {
            STAYS.remove(player.getUUID());
            return;
        }
        BlockPos portalPos = portalPosition(player);
        if (portalPos == null) {
            STAYS.remove(player.getUUID());
            return;
        }
        ResourceKey<Level> dimension = player.serverLevel().dimension();
        PortalStay previous = STAYS.get(player.getUUID());
        int ticks = previous != null && previous.dimension().equals(dimension) ? previous.ticks() + 1 : 1;
        if (ticks < FALLBACK_TICKS) {
            STAYS.put(player.getUUID(), new PortalStay(dimension, ticks));
            return;
        }
        STAYS.remove(player.getUUID());
        var transition = ((NetherPortalBlock) Blocks.NETHER_PORTAL)
                .getPortalDestination(player.serverLevel(), player, portalPos);
        if (transition != null) {
            player.changeDimension(transition);
            player.setPortalCooldown();
        }
    }

    private static BlockPos portalPosition(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        if (player.serverLevel().getBlockState(feet).is(Blocks.NETHER_PORTAL)) return feet;
        BlockPos head = feet.above();
        return player.serverLevel().getBlockState(head).is(Blocks.NETHER_PORTAL) ? head : null;
    }

    private record PortalStay(ResourceKey<Level> dimension, int ticks) {}
}
