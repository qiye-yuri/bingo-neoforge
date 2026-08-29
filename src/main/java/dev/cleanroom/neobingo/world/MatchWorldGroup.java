package dev.cleanroom.neobingo.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import dev.cleanroom.neobingo.domain.TeamId;

import java.util.List;
import java.util.Map;

/** 保存一局比赛独占的主世界、下界与末地实例。 */
public record MatchWorldGroup(
        long matchId,
        long seed,
        ResourceKey<Level> overworldKey,
        ResourceKey<Level> netherKey,
        ResourceKey<Level> endKey,
        BlockPos overworldSpawn,
        Map<TeamId, BlockPos> teamSpawns,
        ServerLevel overworld,
        ServerLevel nether,
        ServerLevel end) {

    public MatchWorldGroup {
        teamSpawns = Map.copyOf(teamSpawns);
    }

    public List<ServerLevel> levels() {
        return List.of(overworld, nether, end);
    }
}
