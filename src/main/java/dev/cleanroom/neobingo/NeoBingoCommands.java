package dev.cleanroom.neobingo;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.cleanroom.neobingo.application.ClaimBatchResult;
import dev.cleanroom.neobingo.application.ObjectiveClaimService;
import dev.cleanroom.neobingo.config.BingoCardDefinition;
import dev.cleanroom.neobingo.config.BingoCardDefinitions;
import dev.cleanroom.neobingo.domain.BingoGame;
import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import dev.cleanroom.neobingo.presentation.BingoCardTextRenderer;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** 注册并执行服务器权威的宾果命令。 */
public final class NeoBingoCommands {
    private NeoBingoCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("neobingo")
                .then(Commands.literal("join")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .executes(context -> run(context.getSource(), () -> join(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "team"))))))
                .then(Commands.literal("leave")
                        .executes(context -> run(context.getSource(), () -> leave(context.getSource()))))
                .then(Commands.literal("start")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource(), () -> start(
                                context.getSource(),
                                context.getSource().getLevel().getRandom().nextLong(),
                                GameMode.STANDARD)))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(context -> run(context.getSource(), () -> start(
                                        context.getSource(),
                                        LongArgumentType.getLong(context, "seed"),
                                        GameMode.STANDARD))))
                        .then(modeStartCommand("standard", GameMode.STANDARD))
                        .then(modeStartCommand("lockout", GameMode.LOCKOUT)))
                .then(Commands.literal("reroll")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource(), () -> reroll(
                                context.getSource(),
                                context.getSource().getLevel().getRandom().nextLong())))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(context -> run(context.getSource(), () -> reroll(
                                        context.getSource(),
                                        LongArgumentType.getLong(context, "seed"))))))
                .then(Commands.literal("card")
                        .executes(context -> run(context.getSource(), () -> showCard(context.getSource()))))
                .then(Commands.literal("claim")
                        .executes(context -> run(context.getSource(), () -> claim(context.getSource()))))
                .then(Commands.literal("status")
                        .executes(context -> run(context.getSource(), () -> showStatus(context.getSource()))))
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

    private static LiteralArgumentBuilder<CommandSourceStack> modeStartCommand(
            String name,
            GameMode mode) {
        return Commands.literal(name)
                .executes(context -> run(context.getSource(), () -> start(
                        context.getSource(),
                        context.getSource().getLevel().getRandom().nextLong(),
                        mode)))
                .then(Commands.argument("seed", LongArgumentType.longArg())
                        .executes(context -> run(context.getSource(), () -> start(
                                context.getSource(),
                                LongArgumentType.getLong(context, "seed"),
                                mode))));
    }

    private static void leave(CommandSourceStack source) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        PlayerId playerId = new PlayerId(player.getUUID());
        if (session.roster().teamOf(playerId).isEmpty()) {
            throw new IllegalStateException("你尚未加入队伍");
        }
        session.leave(playerId);
        data.store(session);
        source.sendSuccess(() -> Component.literal("已离开当前队伍"), false);
    }

    private static void start(CommandSourceStack source, long seed, GameMode mode) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.start(definition.size(), definition.objectives(), seed, mode);
        data.store(session);
        source.sendSuccess(() -> Component.literal(
                "宾果游戏已开始，模式：" + modeName(mode) + "，种子：" + seed), true);
    }

    private static void reroll(CommandSourceStack source, long seed) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.reroll(definition.size(), definition.objectives(), seed);
        data.store(session);
        source.sendSuccess(() -> Component.literal("已重新生成宾果卡，种子：" + seed), true);
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

    private static void showStatus(CommandSourceStack source) {
        BingoSession session = requiredSession(NeoBingoSavedData.get(source.getServer()));
        long teamCount = session.roster().assignments().values().stream().distinct().count();
        StringBuilder status = new StringBuilder("宾果状态：")
                .append(stateName(session.state()))
                .append("，玩家：").append(session.roster().playerCount())
                .append("，队伍：").append(teamCount);
        session.game().ifPresent(game -> status.append("，模式：").append(modeName(game.mode())));
        session.seed().ifPresent(seed -> status.append("，种子：").append(seed));
        session.winner().ifPresent(winner -> status.append("，胜者：").append(winner.value()));
        source.sendSuccess(() -> Component.literal(status.toString()), false);
    }

    private static String modeName(GameMode mode) {
        return switch (mode) {
            case STANDARD -> "标准";
            case LOCKOUT -> "锁定";
        };
    }

    private static String stateName(SessionState state) {
        return switch (state) {
            case LOBBY -> "大厅";
            case RUNNING -> "进行中";
            case FINISHED -> "已结束";
        };
    }

    private static BingoSession requiredSession(NeoBingoSavedData data) {
        return data.restoreSession().orElseThrow(() -> new IllegalStateException("尚未创建宾果大厅"));
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
