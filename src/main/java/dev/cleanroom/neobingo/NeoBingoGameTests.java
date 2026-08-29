package dev.cleanroom.neobingo;

import dev.cleanroom.neobingo.application.ClaimBatchResult;
import dev.cleanroom.neobingo.application.ObjectiveClaimService;
import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.domain.rule.InventoryPresenceRule;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import dev.cleanroom.neobingo.world.RuntimeMatchWorldManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** 在真实服务端关卡中验证核心规则与世界存档适配器的协作。 */
@PrefixGameTestTemplate(false)
public final class NeoBingoGameTests {
    private static final PlayerId PLAYER =
            new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final PlayerId SECOND_PLAYER =
            new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final TeamId RED = new TeamId("red");
    private static final TeamId BLUE = new TeamId("blue");

    private NeoBingoGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        event.register(NeoBingoGameTests.class);
    }

    @GameTest(templateNamespace = NeoBingo.MOD_ID, template = "empty")
    public static void serverObservedObjectivesAreClaimed(GameTestHelper helper) {
        BingoSession session = runningSession();
        ObjectiveId objective = session.game().orElseThrow().card().objectiveAt(3);

        ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                session, PLAYER, Set.of(objective), InventoryPresenceRule.INSTANCE);

        helper.assertValueEqual(result.claimedTiles(), List.of(3), "服务端观察到的目标应认领对应格子");
        helper.assertValueEqual(session.game().orElseThrow().score(RED), 1, "队伍分数应随认领增加");
        helper.succeed();
    }

    @GameTest(templateNamespace = NeoBingo.MOD_ID, template = "empty")
    public static void completedLineFinishesGame(GameTestHelper helper) {
        BingoSession session = runningSession();
        Set<ObjectiveId> firstRow = Set.copyOf(session.game().orElseThrow().card().objectives().subList(0, 5));

        ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                session, PLAYER, firstRow, InventoryPresenceRule.INSTANCE);

        helper.assertValueEqual(result.state(), SessionState.FINISHED, "完成连线后游戏应结束");
        helper.assertValueEqual(result.winner().orElseThrow(), RED, "完成连线的队伍应获胜");
        helper.succeed();
    }

    @GameTest(templateNamespace = NeoBingo.MOD_ID, template = "empty", batch = "worldDataRecovery")
    public static void worldDataRestoresReconnectIdentity(GameTestHelper helper) {
        NeoBingoSavedData data = NeoBingoSavedData.get(helper.getLevel().getServer());
        data.clear();
        BingoSession session = runningSession();
        session.claim(PLAYER, 2);
        data.store(session);

        BingoSession restored = data.restoreSession().orElseThrow();

        helper.assertValueEqual(restored.roster().teamOf(PLAYER).orElseThrow(), RED, "重连玩家应恢复原队伍");
        helper.assertTrue(restored.game().orElseThrow().isClaimedBy(RED, 2), "世界存档应恢复已认领格子");
        data.clear();
        helper.succeed();
    }

    @GameTest(templateNamespace = NeoBingo.MOD_ID, template = "empty")
    public static void lockoutPreventsSecondTeamClaim(GameTestHelper helper) {
        BingoSession session = lobbyWithTwoTeams();
        session.start(5, objectives(), 42L, GameMode.LOCKOUT);
        ObjectiveId objective = session.game().orElseThrow().card().objectiveAt(0);

        ObjectiveClaimService.claimCompleted(
                session, PLAYER, Set.of(objective), InventoryPresenceRule.INSTANCE);
        ClaimBatchResult secondResult =
                ObjectiveClaimService.claimCompleted(
                        session, SECOND_PLAYER, Set.of(objective), InventoryPresenceRule.INSTANCE);

        helper.assertTrue(secondResult.claimedTiles().isEmpty(), "锁定格子不应被第二支队伍认领");
        helper.assertFalse(session.game().orElseThrow().isClaimedBy(BLUE, 0), "第二支队伍不应拥有锁定格子");
        helper.succeed();
    }

    @GameTest(templateNamespace = NeoBingo.MOD_ID, template = "empty")
    public static void lobbyRerollIsUsedWhenGameStarts(GameTestHelper helper) {
        BingoSession session = lobbyWithTwoTeams();
        session.reroll(5, objectives(), 99L);
        List<ObjectiveId> prepared = session.game().orElseThrow().card().objectives();
        session.start(5, objectives(), 42L, GameMode.LOCKOUT);

        helper.assertValueEqual(session.state(), SessionState.RUNNING, "预生成棋盘应能正常开局");
        helper.assertValueEqual(session.game().orElseThrow().mode(), GameMode.LOCKOUT, "开局时应应用所选游戏模式");
        helper.assertValueEqual(session.game().orElseThrow().card().objectives(), prepared, "开局应沿用大厅预生成棋盘");
        helper.assertValueEqual(session.seed().orElseThrow(), 99L, "开局应沿用预生成种子");
        helper.succeed();
    }

    @GameTest(templateNamespace = NeoBingo.MOD_ID, template = "empty", batch = "inventoryTicker")
    @SuppressWarnings("removal")
    public static void serverTickClaimsInventoryObjectives(GameTestHelper helper) {
        NeoBingoSavedData data = NeoBingoSavedData.get(helper.getLevel().getServer());
        data.clear();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().add(Items.STONE.getDefaultInstance());
        PlayerId playerId = new PlayerId(player.getUUID());
        List<ObjectiveId> pool = new ArrayList<>(objectives());
        pool.set(0, new ObjectiveId("minecraft:stone"));
        BingoSession session = new BingoSession();
        session.join(playerId, RED);
        session.start(5, pool, 42L, GameMode.STANDARD);
        data.store(session);

        InventoryClaimTicker.evaluatePlayers(helper.getLevel().getServer(), List.of(player));

        BingoSession restored = data.restoreSession().orElseThrow();
        helper.assertValueEqual(restored.game().orElseThrow().score(RED), 1, "服务端轮询应自动认领物品栏目标");
        data.clear();
        helper.succeed();
    }

    @GameTest(templateNamespace = NeoBingo.MOD_ID, template = "empty", batch = "rankedTicker")
    public static void rankedTickerFinishesAndPersistsSession(GameTestHelper helper) {
        NeoBingoSavedData data = NeoBingoSavedData.get(helper.getLevel().getServer());
        data.clear();
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.startRanked(5, objectives(), 42L, 1);
        session.claim(PLAYER, 0);
        data.store(session);

        helper.assertTrue(RankedCountdownTicker.tick(helper.getLevel().getServer()), "倒计时到期应结束排位游戏");

        BingoSession restored = data.restoreSession().orElseThrow();
        helper.assertValueEqual(restored.state(), SessionState.FINISHED, "排位结束状态应写回世界数据");
        helper.assertValueEqual(restored.remainingTicks().orElseThrow(), 0L, "到期后的剩余时间应为零");
        helper.assertValueEqual(restored.winner().orElseThrow(), RED, "唯一最高分队伍应成为胜者");
        data.clear();
        helper.succeed();
    }

    @GameTest(templateNamespace = NeoBingo.MOD_ID, template = "empty", batch = "commandLifecycle")
    @SuppressWarnings("removal")
    public static void brigadierCommandsCompleteBasicLifecycle(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        NeoBingoSavedData data = NeoBingoSavedData.get(server);
        data.clear();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ServerPlayer secondPlayer = helper.makeMockServerPlayerInLevel();
        PlayerId playerId = new PlayerId(player.getUUID());
        PlayerId secondPlayerId = new PlayerId(secondPlayer.getUUID());

        server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "neobingo book");
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "neobingo book");
        long settingsBookCount = player.getInventory().items.stream()
                .filter(stack -> stack.is(Items.WRITTEN_BOOK))
                .count();
        helper.assertValueEqual(settingsBookCount, 1L, "重复领取不应产生多本设置书");

        server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "neobingo join red");
        helper.assertValueEqual(
                server.getScoreboard().getPlayersTeam(player.getScoreboardName()).getColor(),
                ChatFormatting.RED,
                "加入红队后 Tab 玩家名应显示红色");
        server.getCommands().performPrefixedCommand(secondPlayer.createCommandSourceStack(), "neobingo join blue");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo team assign " + secondPlayer.getUUID() + " green");
        helper.assertValueEqual(
                data.restoreSession().orElseThrow().roster().teamOf(secondPlayerId).orElseThrow(),
                new TeamId("green"),
                "管理员分配命令应更改目标玩家队伍");
        helper.assertValueEqual(
                server.getScoreboard().getPlayersTeam(secondPlayer.getScoreboardName()).getColor(),
                ChatFormatting.GREEN,
                "管理员分队后 Tab 玩家名应同步队伍颜色");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo team remove " + secondPlayer.getUUID());
        helper.assertTrue(
                data.restoreSession().orElseThrow().roster().teamOf(secondPlayerId).isEmpty(),
                "管理员移除命令应将目标玩家移出大厅");
        helper.assertTrue(
                server.getScoreboard().getPlayersTeam(secondPlayer.getScoreboardName()) == null,
                "移出大厅后应清除 Tab 玩家名颜色");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo team assign " + secondPlayer.getUUID() + " blue");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "neobingo manage");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "neobingo randomteams 2");
        BingoSession randomized = data.restoreSession().orElseThrow();
        TeamId randomizedPlayerTeam = randomized.roster().teamOf(playerId).orElseThrow();
        helper.assertFalse(
                randomizedPlayerTeam.equals(randomized.roster().teamOf(secondPlayerId).orElseThrow()),
                "两名玩家随机分入两队时应位于不同队伍");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo lobby settings adjust s 1");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo lobby settings adjust d -1");
        helper.assertValueEqual(data.lobbySettings().count(dev.cleanroom.neobingo.domain.DifficultyTier.S), 4,
                "设置书命令应能单独增加 S 难度数量");
        helper.assertValueEqual(data.lobbySettings().count(dev.cleanroom.neobingo.domain.DifficultyTier.D), 6,
                "设置书命令应能单独减少 D 难度数量");
        helper.assertValueEqual(data.lobbySettings().total(), 25, "调整后的六档数量合计应为 25");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo lobby settings time 60");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo lobby settings spawn_distance 1");
        helper.assertValueEqual(data.lobbySettings().timedSeconds(), 960, "设置书命令应能调整计时时长");
        helper.assertValueEqual(data.lobbySettings().teamSpawnDistanceChunks(), 9,
                "设置书命令应能按区块调整队伍复活点间距");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo lobby settings toggle night_vision");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo lobby settings toggle team_chest");
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "neobingo lobby settings kit minecraft:bread 4");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "neobingo lobby preview");
        helper.assertValueEqual(
                data.restoreSession().orElseThrow().state(), SessionState.LOBBY, "大厅预览棋盘后应保持大厅状态");
        long previewSeed = data.restoreSession().orElseThrow().seed().orElseThrow();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "neobingo lobby start");
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "neobingo status");

        BingoSession running = data.restoreSession().orElseThrow();
        helper.assertValueEqual(running.state(), SessionState.RUNNING, "命令开局后会话应处于运行状态");
        helper.assertValueEqual(
                running.roster().teamOf(playerId).orElseThrow(), randomizedPlayerTeam, "开局后应保留随机分队结果");
        helper.assertValueEqual(running.game().orElseThrow().mode(), GameMode.STANDARD, "开局命令应选择标准模式");
        helper.assertValueEqual(running.seed().orElseThrow(), previewSeed, "开局应沿用大厅中预览棋盘的种子");
        var matchWorlds = RuntimeMatchWorldManager.active().orElseThrow();
        helper.assertTrue(player.serverLevel() == matchWorlds.overworld(), "开局后参赛玩家应进入比赛主世界");
        helper.assertValueEqual(player.getRespawnDimension(), matchWorlds.overworldKey(),
                "比赛中死亡后应在本局主世界复活");
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),
                "开启全员夜视后参赛玩家应获得夜视效果");
        helper.assertTrue(player.getInventory().countItem(Items.BREAD) >= 4,
                "开局时应向每名参赛玩家发放书中配置的初始物资");
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "neobingo teamchest");
        helper.assertTrue(player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu,
                "开启队伍箱后玩家应能打开共享六行箱子");
        player.closeContainer();
        var firstSpawn = matchWorlds.teamSpawns().get(running.roster().teamOf(playerId).orElseThrow());
        var secondSpawn = matchWorlds.teamSpawns().get(running.roster().teamOf(secondPlayerId).orElseThrow());
        helper.assertValueEqual(Math.abs(firstSpawn.getX() - secondSpawn.getX())
                        + Math.abs(firstSpawn.getZ() - secondSpawn.getZ()),
                9 * 16, "两队复活点应按配置的区块距离分布在原点周围");
        helper.assertTrue(
                RuntimeMatchWorldManager.portalTarget(matchWorlds.overworld(), net.minecraft.world.level.Level.NETHER)
                        .orElseThrow() == matchWorlds.nether(),
                "比赛主世界的下界传送门应进入比赛下界");
        var portalTransition = ((net.minecraft.world.level.block.NetherPortalBlock)
                net.minecraft.world.level.block.Blocks.NETHER_PORTAL)
                .getPortalDestination(matchWorlds.overworld(), player, firstSpawn);
        helper.assertTrue(portalTransition != null && portalTransition.newLevel() == matchWorlds.nether(),
                "黑曜石下界传送门应能创建本局下界出口并进入本局下界");
        helper.assertTrue(
                RuntimeMatchWorldManager.portalTarget(matchWorlds.overworld(), net.minecraft.world.level.Level.END)
                        .orElseThrow() == matchWorlds.end(),
                "比赛主世界的末地传送门应进入比赛末地");
        helper.assertTrue(
                RuntimeMatchWorldManager.portalTarget(matchWorlds.nether(), net.minecraft.world.level.Level.NETHER)
                        .orElseThrow() == matchWorlds.overworld(),
                "即使原版误判目标键，比赛下界的返回门也应回到比赛主世界");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "neobingo reroll");
        helper.assertValueEqual(
                data.restoreSession().orElseThrow().seed().orElseThrow(), previewSeed, "游戏过程中不得刷新棋盘");

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "neobingo end");
        helper.assertValueEqual(
                data.restoreSession().orElseThrow().state(),
                SessionState.FINISHED,
                "结束命令应持久化已结束状态");
        helper.assertTrue(player.serverLevel() == server.overworld(), "结束游戏后应将玩家送回大厅主世界");
        helper.assertValueEqual(player.getRespawnDimension(), server.overworld().dimension(),
                "结束游戏后应恢复大厅重生点");
        data.clear();
        helper.succeed();
    }

    private static BingoSession runningSession() {
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.start(5, objectives(), 42L, GameMode.STANDARD);
        return session;
    }

    private static BingoSession lobbyWithTwoTeams() {
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.join(SECOND_PLAYER, BLUE);
        return session;
    }

    private static List<ObjectiveId> objectives() {
        return IntStream.range(0, 25)
                .mapToObj(index -> new ObjectiveId("minecraft:test_item_" + index))
                .toList();
    }
}
