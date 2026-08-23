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
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.domain.rule.InventoryPresenceRule;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import dev.cleanroom.neobingo.presentation.BingoCardTextRenderer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                        .then(modeStartCommand("lockout", GameMode.LOCKOUT))
                        .then(modeStartCommand("hidden", GameMode.HIDDEN)))
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
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.join.success", team.value()), false);
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
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.leave.success"), false);
    }

    private static void start(CommandSourceStack source, long seed, GameMode mode) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.start(definition.size(), definition.objectives(), seed, mode);
        data.store(session);
        source.sendSuccess(() -> Component.translatable(
                "commands.neo_bingo.start.success", modeName(mode), seed), true);
    }

    private static void reroll(CommandSourceStack source, long seed) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.reroll(definition.size(), definition.objectives(), seed);
        data.store(session);
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.reroll.success", seed), true);
    }

    private static void showCard(CommandSourceStack source) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        BingoSession session = requiredSession(NeoBingoSavedData.get(source.getServer()));
        BingoGame game = session.game().orElseThrow(() -> new IllegalStateException("游戏尚未开始"));
        TeamId team = session.roster().teamOf(new PlayerId(player.getUUID()))
                .orElseThrow(() -> new IllegalStateException("你尚未加入队伍"));

        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.card.title", team.value()), false);
        for (String line : BingoCardTextRenderer.render(game, team)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
    }

    private static void end(CommandSourceStack source) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        session.end();
        data.store(session);
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.end.success"), true);
    }

    private static void claim(CommandSourceStack source) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                session,
                new PlayerId(player.getUUID()),
                ServerInventoryObjectiveReader.read(player),
                InventoryPresenceRule.INSTANCE);
        if (result.claimedTiles().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.neo_bingo.claim.empty"), false);
            return;
        }

        data.store(session);
        source.sendSuccess(
                () -> Component.translatable("commands.neo_bingo.claim.success", result.claimedTiles().size()),
                true);
        result.winner().ifPresent(team -> source.sendSuccess(
                () -> Component.translatable("commands.neo_bingo.win", team.value()),
                true));
    }

    private static void showStatus(CommandSourceStack source) {
        BingoSession session = requiredSession(NeoBingoSavedData.get(source.getServer()));
        long teamCount = session.roster().assignments().values().stream().distinct().count();
        source.sendSuccess(() -> Component.translatable(
                "commands.neo_bingo.status.summary",
                stateName(session.state()),
                session.roster().playerCount(),
                teamCount), false);
        session.game().ifPresent(game -> source.sendSuccess(() -> Component.translatable(
                "commands.neo_bingo.status.game",
                modeName(game.mode()),
                session.seed().orElseThrow()), false));
        session.game().ifPresent(game -> game.standings(session.roster().assignments().values()).forEach(standing ->
                source.sendSuccess(() -> Component.translatable(
                        "commands.neo_bingo.status.standing",
                        standing.rank(),
                        standing.team().value(),
                        standing.score()), false)));
        session.winner().ifPresent(winner -> source.sendSuccess(() -> Component.translatable(
                "commands.neo_bingo.status.winner", winner.value()), false));
    }

    private static Component modeName(GameMode mode) {
        return switch (mode) {
            case STANDARD -> Component.translatable("commands.neo_bingo.mode.standard");
            case LOCKOUT -> Component.translatable("commands.neo_bingo.mode.lockout");
            case HIDDEN -> Component.translatable("commands.neo_bingo.mode.hidden");
        };
    }

    private static Component stateName(SessionState state) {
        return switch (state) {
            case LOBBY -> Component.translatable("commands.neo_bingo.state.lobby");
            case RUNNING -> Component.translatable("commands.neo_bingo.state.running");
            case FINISHED -> Component.translatable("commands.neo_bingo.state.finished");
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
            source.sendFailure(Component.translatable("commands.neo_bingo.error.player_only"));
            return 0;
        } catch (Exception exception) {
            NeoBingo.LOGGER.error("执行宾果命令时发生未预期错误", exception);
            source.sendFailure(Component.translatable("commands.neo_bingo.error.unexpected"));
            return 0;
        }
    }

    @FunctionalInterface
    private interface CommandAction {
        void execute() throws Exception;
    }
}
