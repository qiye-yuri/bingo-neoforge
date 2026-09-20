package dev.cleanroom.neobingo;

import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/** 向全服公布实际完成的物品目标，而不是只显示格子数量。 */
public final class BingoClaimAnnouncements {
    private BingoClaimAnnouncements() {}

    public static void broadcast(MinecraftServer server, BingoSession session, TeamId team, List<Integer> tiles) {
        var card = session.game().orElseThrow().card();
        for (int tile : tiles) {
            String id = card.objectiveAt(tile).value();
            Component objective = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id))
                    .map(item -> (Component) item.getDescription())
                    .orElseGet(() -> Component.literal(id));
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("commands.neo_bingo.claim.objective", team.value(), objective), false);
        }
    }
}
