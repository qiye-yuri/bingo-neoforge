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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
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
    public static void rerollKeepsModeAndClearsClaims(GameTestHelper helper) {
        BingoSession session = lobbyWithTwoTeams();
        session.start(5, objectives(), 42L, GameMode.LOCKOUT);
        session.claim(PLAYER, 0);

        session.reroll(5, objectives(), 99L);

        helper.assertValueEqual(session.game().orElseThrow().mode(), GameMode.LOCKOUT, "重新生成应保留游戏模式");
        helper.assertValueEqual(session.game().orElseThrow().score(RED), 0, "重新生成应清空已有认领");
        helper.assertValueEqual(session.seed().orElseThrow(), 99L, "重新生成应更新种子");
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
