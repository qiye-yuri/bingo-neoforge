package dev.cleanroom.neobingo.world;

import dev.cleanroom.neobingo.NeoBingo;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Comparator;
import net.minecraft.world.entity.RelativeMovement;

/** 在原版三维度中管理一局比赛使用的区域与队伍出生点。 */
public final class RuntimeMatchWorldManager {
    private static MatchWorldGroup active;
    private static final AtomicLong MATCH_SEQUENCE = new AtomicLong(System.currentTimeMillis());

    private RuntimeMatchWorldManager() {
    }

    public static synchronized Optional<MatchWorldGroup> active() {
        return Optional.ofNullable(active);
    }

    public static synchronized Optional<ServerLevel> portalTarget(
            ServerLevel source,
            ResourceKey<Level> vanillaTarget) {
        if (active == null || !active.levels().contains(source)) {
            return Optional.empty();
        }
        // 原版用固定维度键判断返回方向；运行时维度键不同，必须按实例识别。
        if (source == active.nether() || source == active.end()) {
            return Optional.of(active.overworld());
        }
        if (vanillaTarget == Level.NETHER) {
            return Optional.of(active.nether());
        }
        if (vanillaTarget == Level.END) {
            return Optional.of(active.end());
        }
        if (vanillaTarget == Level.OVERWORLD) {
            return Optional.of(active.overworld());
        }
        return Optional.empty();
    }

    /** 向原版传送门逻辑提供等价的三维度键，修正运行时维度的方向与搜索半径判断。 */
    public static synchronized ResourceKey<Level> vanillaPortalDimension(ServerLevel level) {
        if (active == null) {
            return level.dimension();
        }
        if (level == active.nether()) {
            return Level.NETHER;
        }
        if (level == active.end()) {
            return Level.END;
        }
        if (level == active.overworld()) {
            return Level.OVERWORLD;
        }
        return level.dimension();
    }

    public static synchronized MatchWorldGroup create(
            MinecraftServer server,
            long matchId,
            long seed,
            Collection<TeamId> teams,
            int spawnDistanceChunks) {
        if (active != null) {
            throw new IllegalStateException("已有运行中的比赛世界组");
        }
        ServerLevel overworld = server.overworld();
        ServerLevel nether = server.getLevel(Level.NETHER);
        ServerLevel end = server.getLevel(Level.END);
        if (nether == null || end == null) {
            throw new IllegalStateException("服务器未加载原版下界或末地");
        }
        BlockPos center = allocateMatchRegion(matchId, seed);
        int centerX = center.getX();
        int centerZ = center.getZ();
        Map<TeamId, net.minecraft.core.BlockPos> teamSpawns = createTeamSpawns(
                overworld, teams, spawnDistanceChunks, centerX, centerZ);
        BlockPos worldSpawn = findSafeSpawn(overworld, centerX, centerZ);
        active = new MatchWorldGroup(
                matchId, seed, Level.OVERWORLD, Level.NETHER, Level.END,
                worldSpawn, teamSpawns, overworld, nether, end);
        NeoBingo.LOGGER.info("已创建运行时比赛地图区域：中心 {}, {}，种子标识 {}", centerX, centerZ, seed);
        return active;
    }

    public static MatchWorldGroup create(
            MinecraftServer server,
            long seed,
            Collection<TeamId> teams,
            int spawnDistanceChunks) {
        return create(server, MATCH_SEQUENCE.incrementAndGet(), seed, teams, spawnDistanceChunks);
    }

    public static void sendToMatch(ServerPlayer player, TeamId team) {
        MatchWorldGroup group = active().orElseThrow(() -> new IllegalStateException("比赛世界尚未创建"));
        ServerLevel level = group.overworld();
        var base = group.teamSpawns().getOrDefault(team, group.overworldSpawn());
        level.getChunk(base.getX() >> 4, base.getZ() >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
        // 强制将个人重生点绑定到本局主世界，死亡后不会回到大厅。
        player.setRespawnPosition(group.overworldKey(), new net.minecraft.core.BlockPos(base.getX(), y, base.getZ()),
                player.getYRot(), true, false);
        player.teleportTo(level, base.getX() + 0.5, y, base.getZ() + 0.5,
                Set.<RelativeMovement>of(), player.getYRot(), player.getXRot());
    }

    public static void returnToLobby(ServerPlayer player) {
        ServerLevel lobby = player.getServer().overworld();
        var base = lobby.getSharedSpawnPos();
        lobby.getChunk(base.getX() >> 4, base.getZ() >> 4);
        int y = lobby.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
        // 对局结束后恢复大厅重生点，避免下一次死亡引用已卸载的比赛维度。
        player.setRespawnPosition(lobby.dimension(), new net.minecraft.core.BlockPos(base.getX(), y, base.getZ()),
                player.getYRot(), true, false);
        player.teleportTo(lobby, base.getX() + 0.5, y, base.getZ() + 0.5,
                Set.<RelativeMovement>of(), player.getYRot(), player.getXRot());
    }

    public static void finish(MinecraftServer server) {
        active().ifPresent(group -> {
            MatchGameplayRules.end(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean participant = NeoBingoSavedData.get(server).restoreSession()
                        .flatMap(session -> session.roster().teamOf(new PlayerId(player.getUUID()))).isPresent();
                if (participant && group.levels().contains(player.serverLevel())) {
                    returnToLobby(player);
                }
            }
            unload(server);
        });
    }

    public static synchronized void unload(MinecraftServer server) {
        MatchWorldGroup group = active;
        if (group == null) {
            return;
        }
        // 原版三维度由服务器持续管理，此处只结束比赛区域，不关闭关卡。
        active = null;
        NeoBingo.LOGGER.info("已结束原版比赛世界区域：{}", group.matchId());
    }

    /** 在本局独立区域的中心附近分布队伍，横纵相邻间距为配置的区块数。 */
    private static Map<TeamId, net.minecraft.core.BlockPos> createTeamSpawns(
            ServerLevel level,
            Collection<TeamId> teams,
            int distanceChunks,
            int centerX,
            int centerZ) {
        var sorted = teams.stream().sorted(Comparator.comparing(TeamId::value)).toList();
        Map<TeamId, net.minecraft.core.BlockPos> result = new LinkedHashMap<>();
        if (sorted.isEmpty()) {
            return result;
        }
        int columns = (int) Math.ceil(Math.sqrt(sorted.size()));
        int rows = (int) Math.ceil((double) sorted.size() / columns);
        int spacing = Math.clamp(distanceChunks, 1, 128) * 16;
        for (int index = 0; index < sorted.size(); index++) {
            int column = index % columns;
            int row = index / columns;
            int x = centerX + (int) Math.round((column - (columns - 1) / 2.0) * spacing);
            int z = centerZ + (int) Math.round((row - (rows - 1) / 2.0) * spacing);
            BlockPos spawn = findSafeSpawn(level, x, z);
            level.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
            result.put(sorted.get(index), spawn);
        }
        return result;
    }

    /**
     * 原项目通过重启后删除世界文件获得新地图；这里在不重启服务器的前提下，
     * 在原版要塞覆盖范围内轮换 48 个地图槽位，同时保留地狱门和末地门可达性。
     */
    static BlockPos allocateMatchRegion(long matchId, long seed) {
        int slot = (int) Math.floorMod(matchId, 48);
        // 7×7 网格去掉中央大厅槽位；连续 48 局不会在本次服务进程中重复。
        int raw = slot >= 24 ? slot + 1 : slot;
        int gridX = raw % 7 - 3;
        int gridZ = raw / 7 - 3;
        return new BlockPos(gridX * 6144, 0, gridZ * 6144);
    }

    /** 在目标点附近寻找脚下可站立且头顶无阻挡的地表。 */
    private static BlockPos findSafeSpawn(ServerLevel level, int targetX, int targetZ) {
        for (int radius = 0; radius <= 8; radius++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    if (radius > 0 && Math.abs(offsetX) != radius && Math.abs(offsetZ) != radius) continue;
                    int x = targetX + offsetX * 16;
                    int z = targetZ + offsetZ * 16;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos position = new BlockPos(x, y, z);
                    if (level.getBlockState(position.below()).isFaceSturdy(level, position.below(),
                            net.minecraft.core.Direction.UP)
                            && level.getBlockState(position).getCollisionShape(level, position).isEmpty()
                            && level.getBlockState(position.above()).getCollisionShape(level, position.above()).isEmpty()
                            && level.getFluidState(position).isEmpty()) {
                        return position;
                    }
                }
            }
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
        return new BlockPos(targetX, y, targetZ);
    }
}
