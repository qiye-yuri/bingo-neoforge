package dev.cleanroom.neobingo;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import dev.cleanroom.neobingo.network.NeoBingoNetwork;
import dev.cleanroom.neobingo.presentation.BingoCardTextRenderer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** 注册并执行服务器权威的 Bingo 命令。 */
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
                        .then(modeStartCommand("hidden", GameMode.HIDDEN))
                        .then(rankedStartCommand()))
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

    private static LiteralArgumentBuilder<CommandSourceStack> rankedStartCommand() {
        return Commands.literal("ranked")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 86400))
                        .executes(context -> run(context.getSource(), () -> startRanked(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "seconds"),
                                context.getSource().getLevel().getRandom().nextLong())))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(context -> run(context.getSource(), () -> startRanked(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds"),
                                        LongArgumentType.getLong(context, "seed"))))));
    }

    private static void leave(CommandSourceStack source) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        PlayerId playerId = new PlayerId(player.getUUID());
        if (session.roster().teamOf(playerId).isEmpty()) {
            throw failure("commands.neo_bingo.error.not_joined");
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
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        source.sendSuccess(() -> Component.translatable(
                "commands.neo_bingo.start.success", modeName(mode), seed), true);
    }

    private static void reroll(CommandSourceStack source, long seed) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.reroll(definition.size(), definition.objectives(), seed);
        data.store(session);
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.reroll.success", seed), true);
    }

    private static void startRanked(CommandSourceStack source, int seconds, long seed) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.startRanked(definition.size(), definition.objectives(), seed, Math.multiplyExact(seconds, 20L));
        data.store(session);
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        source.sendSuccess(() -> Component.translatable(
                "commands.neo_bingo.start.ranked.success", seconds, seed), true);
    }

    private static void showCard(CommandSourceStack source) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        BingoSession session = requiredSession(NeoBingoSavedData.get(source.getServer()));
        BingoGame game = session.game().orElseThrow(() -> failure("commands.neo_bingo.error.not_started"));
        TeamId team = session.roster().teamOf(new PlayerId(player.getUUID()))
                .orElseThrow(() -> failure("commands.neo_bingo.error.not_joined"));

        NeoBingoNetwork.sendCardIfSupported(player, game, team);

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
        PlayerId playerId = new PlayerId(player.getUUID());
        if (session.game().isEmpty()) {
            throw failure("commands.neo_bingo.error.not_started");
        }
        if (session.roster().teamOf(playerId).isEmpty()) {
            throw failure("commands.neo_bingo.error.not_joined");
        }
        ClaimBatchResult result = ObjectiveClaimService.claimCompleted(
                session,
                playerId,
                ServerInventoryObjectiveReader.read(player),
                InventoryPresenceRule.INSTANCE);
        if (result.claimedTiles().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.neo_bingo.claim.empty"), false);
            return;
        }

        data.store(session);
        TeamId team = session.roster().teamOf(new PlayerId(player.getUUID())).orElseThrow();
        NeoBingoNetwork.syncTeamCard(
                session, team, source.getServer().getPlayerList().getPlayers());
        source.sendSuccess(
                () -> Component.translatable("commands.neo_bingo.claim.success", result.claimedTiles().size()),
                true);
        result.winner().ifPresent(winner -> source.sendSuccess(
                () -> Component.translatable("commands.neo_bingo.win", winner.value()),
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
        session.roster().teamSizes().forEach((team, memberCount) -> source.sendSuccess(
                () -> Component.translatable(
                        "commands.neo_bingo.status.team", team.value(), memberCount), false));
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
        session.remainingTicks().ifPresent(ticks -> source.sendSuccess(() -> Component.translatable(
                "commands.neo_bingo.status.remaining", (ticks + 19) / 20), false));
    }

    private static Component modeName(GameMode mode) {
        return switch (mode) {
            case STANDARD -> Component.translatable("commands.neo_bingo.mode.standard");
            case LOCKOUT -> Component.translatable("commands.neo_bingo.mode.lockout");
            case HIDDEN -> Component.translatable("commands.neo_bingo.mode.hidden");
            case RANKED -> Component.translatable("commands.neo_bingo.mode.ranked");
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
        return data.restoreSession().orElseThrow(() -> failure("commands.neo_bingo.error.no_lobby"));
    }

    private static int run(CommandSourceStack source, CommandAction action) {
        try {
            action.execute();
            return 1;
        } catch (CommandFeedbackException exception) {
            source.sendFailure(exception.feedback());
            return 0;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            NeoBingo.LOGGER.debug("Bingo 命令参数或会话状态无效", exception);
            source.sendFailure(Component.translatable("commands.neo_bingo.error.invalid_request"));
            return 0;
        } catch (CommandSyntaxException exception) {
            source.sendFailure(Component.translatable("commands.neo_bingo.error.player_only"));
            return 0;
        } catch (Exception exception) {
            NeoBingo.LOGGER.error("执行 Bingo 命令时发生未预期错误", exception);
            source.sendFailure(Component.translatable("commands.neo_bingo.error.unexpected"));
            return 0;
        }
    }

    private static CommandFeedbackException failure(String translationKey) {
        return new CommandFeedbackException(Component.translatable(translationKey));
    }

    private static final class CommandFeedbackException extends RuntimeException {
        private final Component feedback;

        private CommandFeedbackException(Component feedback) {
            super(null, null, false, false);
            this.feedback = feedback;
        }

        private Component feedback() {
            return feedback;
        }
    }

    @FunctionalInterface
    private interface CommandAction {
        void execute() throws Exception;
    }
}
