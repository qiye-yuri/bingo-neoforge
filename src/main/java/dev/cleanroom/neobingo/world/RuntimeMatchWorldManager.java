package dev.cleanroom.neobingo.world;

import com.google.common.collect.ImmutableList;
import dev.cleanroom.neobingo.NeoBingo;
import dev.cleanroom.neobingo.domain.TeamId;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Comparator;
import net.minecraft.world.entity.RelativeMovement;

/** 在服务器运行期间创建并卸载单局独占的三维度世界组。 */
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
        String root = "matches/" + Long.toUnsignedString(matchId, 36);
        ResourceKey<Level> overworldKey = levelKey(root + "/overworld");
        ResourceKey<Level> netherKey = levelKey(root + "/the_nether");
        ResourceKey<Level> endKey = levelKey(root + "/the_end");

        ServerLevel overworld = createLevel(server, overworldKey, LevelStem.OVERWORLD, true);
        ServerLevel nether = createLevel(server, netherKey, LevelStem.NETHER, false);
        ServerLevel end = createLevel(server, endKey, LevelStem.END, false);
        end.setDragonFight(new EndDragonFight(end, seed, EndDragonFight.Data.DEFAULT));
        Map<TeamId, net.minecraft.core.BlockPos> teamSpawns = createTeamSpawns(
                overworld, teams, spawnDistanceChunks);
        int spawnY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        active = new MatchWorldGroup(
                matchId, seed, overworldKey, netherKey, endKey,
                new net.minecraft.core.BlockPos(0, spawnY, 0), teamSpawns, overworld, nether, end);
        NeoBingo.LOGGER.info("已创建运行时比赛世界组：{}", root);
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
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (group.levels().contains(player.serverLevel())) {
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
        if (group.levels().stream().anyMatch(level -> !level.players().isEmpty())) {
            throw new IllegalStateException("比赛世界中仍有玩家，不能卸载");
        }
        for (ServerLevel level : group.levels().reversed()) {
            server.forgeGetWorldMap().remove(level.dimension());
            server.markWorldsDirty();
            level.save(null, true, false);
            NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
            try {
                level.close();
            } catch (IOException exception) {
                throw new IllegalStateException("无法关闭比赛世界 " + level.dimension().location(), exception);
            }
        }
        active = null;
        NeoBingo.LOGGER.info("已卸载运行时比赛世界组：{}", group.matchId());
    }

    private static ServerLevel createLevel(
            MinecraftServer server,
            ResourceKey<Level> levelKey,
            ResourceKey<LevelStem> stemKey,
            boolean tickTime) {
        LevelStem stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).getOrThrow(stemKey);
        var data = new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData());
        var listener = LoggerChunkProgressListener.createCompleted();
        ServerLevel level = new ServerLevel(
                server,
                server.executor,
                server.storageSource,
                data,
                levelKey,
                stem,
                listener,
                server.getWorldData().isDebugWorld(),
                BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed()),
                ImmutableList.of(),
                tickTime,
                server.overworld().getRandomSequences());
        server.overworld().getWorldBorder().addListener(
                new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        server.forgeGetWorldMap().put(levelKey, level);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
    }

    private static ResourceKey<Level> levelKey(String path) {
        return ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(NeoBingo.MOD_ID, path));
    }

    /** 以 (0,0) 为中心在网格上分布队伍，横纵相邻间距为配置的区块数。 */
    private static Map<TeamId, net.minecraft.core.BlockPos> createTeamSpawns(
            ServerLevel level,
            Collection<TeamId> teams,
            int distanceChunks) {
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
            int x = (int) Math.round((column - (columns - 1) / 2.0) * spacing);
            int z = (int) Math.round((row - (rows - 1) / 2.0) * spacing);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            result.put(sorted.get(index), new net.minecraft.core.BlockPos(x, y, z));
        }
        return result;
    }
}
