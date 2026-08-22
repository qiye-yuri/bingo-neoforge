package dev.cleanroom.neobingo;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.cleanroom.neobingo.application.ClaimBatchResult;
import dev.cleanroom.neobingo.application.ObjectiveClaimService;
import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.presentation.BingoCardTextRenderer;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** 注册并执行服务器权威的宾果命令。 */
public final class NeoBingoCommands {
    private static final int CARD_SIZE = 5;

    private NeoBingoCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("neobingo")
                .then(Commands.literal("join")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .executes(context -> run(context.getSource(), () -> join(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "team"))))))
                .then(Commands.literal("start")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource(), () -> start(
                                context.getSource(),
                                context.getSource().getLevel().getRandom().nextLong())))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(context -> run(context.getSource(), () -> start(
                                        context.getSource(),
                                        LongArgumentType.getLong(context, "seed"))))))
                .then(Commands.literal("card")
                        .executes(context -> run(context.getSource(), () -> showCard(context.getSource()))))
                .then(Commands.literal("claim")
                        .executes(context -> run(context.getSource(), () -> claim(context.getSource()))))
                .then(Commands.literal("end")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource(), () -> end(context.getSource())))));
    }

    private static void join(CommandSourceStack source, String teamName) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = data.restoreSession()
                .filter(existing -> existing.state() != SessionState.FINISHED)
                .orElseGet(BingoSession::new);
        TeamId team = new TeamId(teamName);
        session.join(new PlayerId(player.getUUID()), team);
        data.store(session);
        source.sendSuccess(() -> Component.literal("已加入队伍 " + team.value()), false);
    }

    private static void start(CommandSourceStack source, long seed) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        session.start(CARD_SIZE, objectivePool(), seed, GameMode.STANDARD);
        data.store(session);
        source.sendSuccess(() -> Component.literal("宾果游戏已开始，种子：" + seed), true);
    }

    private static void showCard(CommandSourceStack source) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        BingoSession session = requiredSession(NeoBingoSavedData.get(source.getServer()));
        BingoGame game = session.game().orElseThrow(() -> new IllegalStateException("游戏尚未开始"));
        TeamId team = session.roster().teamOf(new PlayerId(player.getUUID()))
                .orElseThrow(() -> new IllegalStateException("你尚未加入队伍"));

        source.sendSuccess(() -> Component.literal("宾果卡（队伍 " + team.value() + "）"), false);
        for (String line : BingoCardTextRenderer.render(game, team)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
    }

    private static void end(CommandSourceStack source) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        session.end();
        data.store(session);
        source.sendSuccess(() -> Component.literal("宾果游戏已结束"), true);
    }

    private static void claim(CommandSourceStack source) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        Set<ObjectiveId> inventoryObjectives = IntStream.range(0, player.getInventory().getContainerSize())
                .mapToObj(player.getInventory()::getItem)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                .map(key -> new ObjectiveId(key.toString()))
                .collect(Collectors.toUnmodifiableSet());
        ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                session,
                new PlayerId(player.getUUID()),
                inventoryObjectives);
        if (result.claimedTiles().isEmpty()) {
            source.sendSuccess(() -> Component.literal("物品栏中没有可新认领的目标"), false);
            return;
        }

        data.store(session);
        source.sendSuccess(
                () -> Component.literal("已为队伍认领 " + result.claimedTiles().size() + " 个格子"),
                true);
        result.winner().ifPresent(team -> source.sendSuccess(
                () -> Component.literal("队伍 " + team.value() + " 完成连线并获胜"),
                true));
    }

    private static BingoSession requiredSession(NeoBingoSavedData data) {
        return data.restoreSession().orElseThrow(() -> new IllegalStateException("尚未创建宾果大厅"));
    }

    private static List<ObjectiveId> objectivePool() {
        return BuiltInRegistries.ITEM.keySet().stream()
                .filter(key -> BuiltInRegistries.ITEM.get(key) != Items.AIR)
                .map(key -> new ObjectiveId(key.toString()))
                .toList();
    }

    private static int run(CommandSourceStack source, CommandAction action) {
        try {
            action.execute();
            return 1;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        } catch (CommandSyntaxException exception) {
            source.sendFailure(Component.literal("命令只能由游戏内玩家执行"));
            return 0;
        } catch (Exception exception) {
            NeoBingo.LOGGER.error("执行宾果命令时发生未预期错误", exception);
            source.sendFailure(Component.literal("命令执行失败，请查看服务器日志"));
            return 0;
        }
    }

    @FunctionalInterface
    private interface CommandAction {
        void execute() throws Exception;
    }
}
