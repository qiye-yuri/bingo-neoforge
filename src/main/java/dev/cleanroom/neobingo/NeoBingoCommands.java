package dev.cleanroom.neobingo;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.cleanroom.neobingo.application.ClaimBatchResult;
import dev.cleanroom.neobingo.application.ObjectiveClaimService;
import dev.cleanroom.neobingo.config.BingoCardDefinition;
import dev.cleanroom.neobingo.config.BingoCardDefinitions;
import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.DifficultyTier;
import dev.cleanroom.neobingo.domain.DifficultyDistribution;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.domain.rule.InventoryPresenceRule;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import dev.cleanroom.neobingo.network.NeoBingoNetwork;
import dev.cleanroom.neobingo.presentation.BingoModeText;
import dev.cleanroom.neobingo.world.RuntimeMatchWorldManager;
import dev.cleanroom.neobingo.world.MatchGameplayRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.EnumMap;

/** 注册并执行服务器权威的 Bingo 命令。 */
public final class NeoBingoCommands {
    private NeoBingoCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("neobingo")
                .then(Commands.literal("join")
                        .requires(NeoBingoPermissions::canPlay)
                        .then(Commands.argument("team", StringArgumentType.word())
                                .executes(context -> run(context.getSource(), () -> join(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "team"))))))
                .then(Commands.literal("leave")
                        .requires(NeoBingoPermissions::canPlay)
                        .executes(context -> run(context.getSource(), () -> leave(context.getSource()))))
                .then(Commands.literal("book")
                        .requires(NeoBingoPermissions::canPlay)
                        .executes(context -> run(context.getSource(), () -> giveBook(context.getSource()))))
                .then(Commands.literal("teamchest")
                        .requires(NeoBingoPermissions::canPlay)
                        .executes(context -> run(context.getSource(), () -> openTeamChest(context.getSource()))))
                .then(Commands.literal("randomteams")
                        .requires(NeoBingoPermissions::canAdmin)
                        .executes(context -> run(context.getSource(), () -> randomizeTeams(context.getSource(), 2)))
                        .then(Commands.argument("count", IntegerArgumentType.integer(2, 8))
                                .executes(context -> run(context.getSource(), () -> randomizeTeams(
                                        context.getSource(), IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("team")
                        .requires(NeoBingoPermissions::canAdmin)
                        .then(Commands.literal("assign")
                                .then(Commands.argument("player", EntityArgument.entity())
                                        .then(Commands.argument("team", StringArgumentType.word())
                                                .executes(context -> run(context.getSource(), () -> assignPlayer(
                                                        context.getSource(),
                                                        requiredTargetPlayer(EntityArgument.getEntity(context, "player")),
                                                        StringArgumentType.getString(context, "team")))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.entity())
                                        .executes(context -> run(context.getSource(), () -> removePlayer(
                                                context.getSource(),
                                                requiredTargetPlayer(EntityArgument.getEntity(context, "player"))))))))
                .then(Commands.literal("manage")
                        .requires(NeoBingoPermissions::canAdmin)
                        .executes(context -> run(context.getSource(), () -> showTeamManager(context.getSource()))))
                .then(Commands.literal("lobby")
                        .requires(NeoBingoPermissions::canAdmin)
                        .then(lobbySettingsCommands())
                        .then(Commands.literal("preview")
                                .executes(context -> run(context.getSource(), () -> previewLobbyCard(context.getSource()))))
                        .then(Commands.literal("refresh")
                                .executes(context -> run(context.getSource(), () -> previewLobbyCard(context.getSource()))))
                        .then(Commands.literal("start")
                                .executes(context -> run(context.getSource(), () -> startConfigured(context.getSource())))))
                .then(Commands.literal("start")
                        .requires(NeoBingoPermissions::canPlay)
                        .executes(context -> run(context.getSource(), () -> start(
                                context.getSource(),
                                context.getSource().getLevel().getRandom().nextLong(),
                                GameMode.STANDARD,
                                DifficultyTier.C)))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(context -> run(context.getSource(), () -> start(
                                        context.getSource(),
                                        LongArgumentType.getLong(context, "seed"),
                                        GameMode.STANDARD,
                                        DifficultyTier.C))))
                        .then(modeStartCommand("standard", GameMode.STANDARD))
                        .then(modeStartCommand("lockout", GameMode.LOCKOUT))
                        .then(modeStartCommand("hidden", GameMode.HIDDEN))
                        .then(rankedStartCommand()))
                .then(Commands.literal("reroll")
                        .requires(NeoBingoPermissions::canAdmin)
                        .executes(context -> run(context.getSource(), () -> reroll(
                                context.getSource(),
                                context.getSource().getLevel().getRandom().nextLong(),
                                DifficultyTier.C)))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(context -> run(context.getSource(), () -> reroll(
                                        context.getSource(),
                                        LongArgumentType.getLong(context, "seed"),
                                        DifficultyTier.C))))
                        .then(rerollDifficultyCommands())
                        .then(Commands.literal("mix").then(distributionArguments(context -> run(
                                context.getSource(), () -> reroll(
                                        context.getSource(),
                                        context.getSource().getLevel().getRandom().nextLong(),
                                        distribution(context)))))))
                .then(Commands.literal("claim")
                        .requires(NeoBingoPermissions::canPlay)
                        .executes(context -> run(context.getSource(), () -> claim(context.getSource()))))
                .then(Commands.literal("status")
                        .requires(NeoBingoPermissions::canPlay)
                        .executes(context -> run(context.getSource(), () -> showStatus(context.getSource()))))
                .then(Commands.literal("end")
                        .requires(NeoBingoPermissions::canAdmin)
                        .executes(context -> run(context.getSource(), () -> end(context.getSource())))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> lobbySettingsCommands() {
        return Commands.literal("settings")
                .executes(context -> run(context.getSource(), () -> showLobbySettings(context.getSource())))
                .then(Commands.literal("mode")
                        .then(lobbyMode("standard", GameMode.STANDARD))
                        .then(lobbyMode("lockout", GameMode.LOCKOUT))
                        .then(lobbyMode("hidden", GameMode.HIDDEN))
                        .then(lobbyMode("ranked", GameMode.RANKED)))
                .then(Commands.literal("adjust")
                        .then(Commands.argument("tier", StringArgumentType.word())
                                .then(Commands.argument("delta", IntegerArgumentType.integer(-25, 25))
                                        .executes(context -> run(context.getSource(), () -> adjustLobbyDifficulty(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "tier"),
                                                IntegerArgumentType.getInteger(context, "delta")))))))
                .then(Commands.literal("time")
                        .then(Commands.argument("delta_seconds", IntegerArgumentType.integer(-86_400, 86_400))
                                .executes(context -> run(context.getSource(), () -> adjustTimedSeconds(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "delta_seconds"))))))
                .then(Commands.literal("spawn_distance")
                        .then(Commands.argument("delta_chunks", IntegerArgumentType.integer(-128, 128))
                                .executes(context -> run(context.getSource(), () -> adjustSpawnDistance(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "delta_chunks"))))))
                .then(Commands.literal("team_chest_rows")
                        .then(Commands.argument("delta", IntegerArgumentType.integer(-6, 6))
                                .executes(context -> run(context.getSource(), () -> adjustTeamChestRows(
                                        context.getSource(), IntegerArgumentType.getInteger(context, "delta"))))))
                .then(Commands.literal("toggle")
                        .then(Commands.argument("rule", StringArgumentType.word())
                                .executes(context -> run(context.getSource(), () -> toggleLobbyRule(
                                        context.getSource(), StringArgumentType.getString(context, "rule"))))))
                .then(Commands.literal("kit")
                        .then(Commands.literal("clear")
                                .executes(context -> run(context.getSource(), () -> clearStarterItems(
                                        context.getSource()))))
                        .then(Commands.literal("held")
                                .then(Commands.argument("delta", IntegerArgumentType.integer(-64, 64))
                                        .executes(context -> run(context.getSource(), () -> adjustHeldStarterItem(
                                                context.getSource(), IntegerArgumentType.getInteger(context, "delta"))))))
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .then(Commands.argument("delta", IntegerArgumentType.integer(-64, 64))
                                        .executes(context -> run(context.getSource(), () -> adjustStarterItem(
                                                context.getSource(), ResourceLocationArgument.getId(context, "item"),
                                                IntegerArgumentType.getInteger(context, "delta")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> lobbyMode(String name, GameMode mode) {
        return Commands.literal(name)
                .executes(context -> run(context.getSource(), () -> setLobbyMode(context.getSource(), mode)));
    }

    private static void setLobbyMode(CommandSourceStack source, GameMode mode) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        requireLobby(data);
        data.lobbySettings().mode(mode);
        data.lobbySettingsChanged();
        showLobbySettings(source);
    }

    private static void adjustLobbyDifficulty(CommandSourceStack source, String tierName, int delta) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        requireLobby(data);
        DifficultyTier tier = DifficultyTier.valueOf(tierName.toUpperCase(java.util.Locale.ROOT));
        data.lobbySettings().adjust(tier, delta);
        data.lobbySettingsChanged();
        showLobbySettings(source);
    }

    private static void showLobbySettings(CommandSourceStack source) {
        var settings = NeoBingoSavedData.get(source.getServer()).lobbySettings();
        source.sendSuccess(() -> Component.translatable(
                "commands.neo_bingo.lobby.settings",
                BingoModeText.displayName(settings.mode()),
                settings.count(DifficultyTier.MAX), settings.count(DifficultyTier.S),
                settings.count(DifficultyTier.A), settings.count(DifficultyTier.B),
                settings.count(DifficultyTier.C), settings.count(DifficultyTier.D), settings.total(),
                settings.timedSeconds() / 60, settings.teamSpawnDistanceChunks(),
                settingState(settings.nightVision()), settingState(settings.keepInventory()),
                settingState(settings.teamChest()), settings.teamChestRows()), false);
    }

    private static Component settingState(boolean enabled) {
        return Component.translatable(enabled ? "book.neo_bingo.enabled" : "book.neo_bingo.disabled");
    }

    private static void adjustTimedSeconds(CommandSourceStack source, int deltaSeconds) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        requireLobby(data);
        data.lobbySettings().adjustTimedSeconds(deltaSeconds);
        data.lobbySettingsChanged();
        showLobbySettings(source);
    }

    private static void adjustSpawnDistance(CommandSourceStack source, int deltaChunks) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        requireLobby(data);
        data.lobbySettings().adjustTeamSpawnDistanceChunks(deltaChunks);
        data.lobbySettingsChanged();
        showLobbySettings(source);
    }

    private static void adjustTeamChestRows(CommandSourceStack source, int delta) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        requireLobby(data);
        data.lobbySettings().adjustTeamChestRows(delta);
        data.lobbySettingsChanged();
        showLobbySettings(source);
    }

    private static void toggleLobbyRule(CommandSourceStack source, String rule) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        requireLobby(data);
        switch (rule) {
            case "night_vision" -> data.lobbySettings().toggleNightVision();
            case "keep_inventory" -> data.lobbySettings().toggleKeepInventory();
            case "team_chest" -> data.lobbySettings().toggleTeamChest();
            default -> throw failure("commands.neo_bingo.error.invalid_request");
        }
        data.lobbySettingsChanged();
        showLobbySettings(source);
    }

    private static void adjustStarterItem(CommandSourceStack source, ResourceLocation id, int delta) {
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id))
            throw failure("commands.neo_bingo.error.invalid_request");
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        requireLobby(data);
        int count = data.lobbySettings().adjustStarterItem(id.toString(), delta);
        data.lobbySettingsChanged();
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.lobby.kit", id.toString(), count), false);
    }

    private static void adjustHeldStarterItem(CommandSourceStack source, int delta) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        if (player.getMainHandItem().isEmpty()) throw failure("commands.neo_bingo.error.invalid_request");
        adjustStarterItem(source,
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()), delta);
    }

    private static void clearStarterItems(CommandSourceStack source) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        requireLobby(data);
        data.lobbySettings().clearStarterItems();
        data.lobbySettingsChanged();
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.lobby.kit.cleared"), false);
    }

    private static void openTeamChest(CommandSourceStack source) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        BingoSession session = requiredSession(NeoBingoSavedData.get(source.getServer()));
        if (session.state() != SessionState.RUNNING) throw failure("commands.neo_bingo.error.not_started");
        TeamId team = session.roster().teamOf(new PlayerId(player.getUUID()))
                .orElseThrow(() -> failure("commands.neo_bingo.error.not_joined"));
        MatchGameplayRules.openTeamChest(player, team);
    }

    private static void previewLobbyCard(CommandSourceStack source) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        long seed = source.getLevel().getRandom().nextLong();
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.reroll(definition.size(), BingoCardDefinitions.objectives(data.lobbySettings().distribution(), seed), seed);
        data.store(session);
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        announce(source, Component.translatable("commands.neo_bingo.lobby.preview.success", seed));
    }

    private static void startConfigured(CommandSourceStack source) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        var settings = data.lobbySettings();
        long seed = data.restoreSession().flatMap(BingoSession::seed)
                .orElseGet(() -> source.getLevel().getRandom().nextLong());
        if (settings.mode() == GameMode.RANKED) {
            startRanked(source, settings.timedSeconds(), seed, settings.distribution());
        } else {
            start(source, seed, settings.mode(), settings.distribution());
        }
    }

    private static void requireLobby(NeoBingoSavedData data) {
        data.restoreSession().ifPresent(session -> {
            if (session.state() != SessionState.LOBBY) {
                throw failure("commands.neo_bingo.error.invalid_request");
            }
        });
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
        BingoScoreboardTeams.assign(player, team);
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.join.success", team.value()), false);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> modeStartCommand(
            String name,
            GameMode mode) {
        return Commands.literal(name)
                .executes(context -> run(context.getSource(), () -> start(
                        context.getSource(),
                        context.getSource().getLevel().getRandom().nextLong(),
                        mode,
                        DifficultyTier.C)))
                .then(Commands.argument("seed", LongArgumentType.longArg())
                        .executes(context -> run(context.getSource(), () -> start(
                                context.getSource(),
                                LongArgumentType.getLong(context, "seed"),
                                mode,
                                DifficultyTier.C))))
                .then(difficultyCommands(mode))
                .then(Commands.literal("mix").then(distributionArguments(context -> run(
                        context.getSource(), () -> start(
                                context.getSource(),
                                context.getSource().getLevel().getRandom().nextLong(),
                                mode,
                                distribution(context))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> difficultyCommands(GameMode mode) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("difficulty");
        for (DifficultyTier tier : DifficultyTier.values()) {
            root.then(Commands.literal(tier.name().toLowerCase())
                    .executes(context -> run(context.getSource(), () -> start(
                            context.getSource(),
                            context.getSource().getLevel().getRandom().nextLong(),
                            mode,
                            tier)))
                    .then(Commands.argument("seed", LongArgumentType.longArg())
                            .executes(context -> run(context.getSource(), () -> start(
                                    context.getSource(),
                                    LongArgumentType.getLong(context, "seed"),
                                    mode,
                                    tier)))));
        }
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rankedStartCommand() {
        return Commands.literal("ranked")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 86400))
                        .executes(context -> run(context.getSource(), () -> startRanked(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "seconds"),
                                context.getSource().getLevel().getRandom().nextLong(),
                                DifficultyTier.C)))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(context -> run(context.getSource(), () -> startRanked(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds"),
                                        LongArgumentType.getLong(context, "seed"),
                                        DifficultyTier.C))))
                        .then(rankedDifficultyCommands())
                        .then(Commands.literal("mix").then(distributionArguments(context -> run(
                                context.getSource(), () -> startRanked(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds"),
                                        context.getSource().getLevel().getRandom().nextLong(),
                                        distribution(context)))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rankedDifficultyCommands() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("difficulty");
        for (DifficultyTier tier : DifficultyTier.values()) {
            root.then(Commands.literal(tier.name().toLowerCase())
                    .executes(context -> run(context.getSource(), () -> startRanked(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "seconds"),
                            context.getSource().getLevel().getRandom().nextLong(),
                            tier)))
                    .then(Commands.argument("seed", LongArgumentType.longArg())
                            .executes(context -> run(context.getSource(), () -> startRanked(
                                    context.getSource(),
                                    IntegerArgumentType.getInteger(context, "seconds"),
                                    LongArgumentType.getLong(context, "seed"),
                                    tier)))));
        }
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rerollDifficultyCommands() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("difficulty");
        for (DifficultyTier tier : DifficultyTier.values()) {
            root.then(Commands.literal(tier.name().toLowerCase())
                    .executes(context -> run(context.getSource(), () -> reroll(
                            context.getSource(), context.getSource().getLevel().getRandom().nextLong(), tier)))
                    .then(Commands.argument("seed", LongArgumentType.longArg())
                            .executes(context -> run(context.getSource(), () -> reroll(
                                    context.getSource(), LongArgumentType.getLong(context, "seed"), tier)))));
        }
        return root;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> distributionArguments(
            Command<CommandSourceStack> action) {
        return Commands.argument("max_count", IntegerArgumentType.integer(0, 25))
                .then(Commands.argument("s_count", IntegerArgumentType.integer(0, 25))
                        .then(Commands.argument("a_count", IntegerArgumentType.integer(0, 25))
                                .then(Commands.argument("b_count", IntegerArgumentType.integer(0, 25))
                                        .then(Commands.argument("c_count", IntegerArgumentType.integer(0, 25))
                                                .then(Commands.argument("d_count", IntegerArgumentType.integer(0, 25))
                                                        .executes(action))))));
    }

    private static DifficultyDistribution distribution(CommandContext<CommandSourceStack> context) {
        EnumMap<DifficultyTier, Integer> counts = new EnumMap<>(DifficultyTier.class);
        counts.put(DifficultyTier.MAX, IntegerArgumentType.getInteger(context, "max_count"));
        counts.put(DifficultyTier.S, IntegerArgumentType.getInteger(context, "s_count"));
        counts.put(DifficultyTier.A, IntegerArgumentType.getInteger(context, "a_count"));
        counts.put(DifficultyTier.B, IntegerArgumentType.getInteger(context, "b_count"));
        counts.put(DifficultyTier.C, IntegerArgumentType.getInteger(context, "c_count"));
        counts.put(DifficultyTier.D, IntegerArgumentType.getInteger(context, "d_count"));
        return new DifficultyDistribution(counts);
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
        BingoScoreboardTeams.remove(player);
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.leave.success"), false);
    }

    private static void giveBook(CommandSourceStack source) throws Exception {
        ServerPlayer player = source.getPlayerOrException();
        boolean given = BingoSettingsBook.giveIfMissing(player);
        source.sendSuccess(() -> Component.translatable(given
                ? "commands.neo_bingo.book.success"
                : "commands.neo_bingo.book.already_has"), false);
    }

    private static void randomizeTeams(CommandSourceStack source, int teamCount) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        session.randomizeTeams(teamCount, new java.util.Random(source.getLevel().getRandom().nextLong()));
        data.store(session);
        BingoScoreboardTeams.synchronize(session, source.getServer().getPlayerList().getPlayers());
        announce(source, Component.translatable(
                "commands.neo_bingo.randomteams.success", session.roster().playerCount(), teamCount));
    }

    private static void assignPlayer(CommandSourceStack source, ServerPlayer player, String teamName) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        TeamId team = new TeamId(teamName);
        session.join(new PlayerId(player.getUUID()), team);
        data.store(session);
        BingoScoreboardTeams.assign(player, team);
        announce(source, Component.translatable(
                "commands.neo_bingo.team.assign.success", player.getDisplayName(), team.value()));
    }

    private static ServerPlayer requiredTargetPlayer(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        throw failure("commands.neo_bingo.error.target_player_only");
    }

    private static void removePlayer(CommandSourceStack source, ServerPlayer player) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        PlayerId playerId = new PlayerId(player.getUUID());
        if (session.roster().teamOf(playerId).isEmpty()) {
            throw failure("commands.neo_bingo.error.target_not_joined");
        }
        session.leave(playerId);
        data.store(session);
        BingoScoreboardTeams.remove(player);
        announce(source, Component.translatable(
                "commands.neo_bingo.team.remove.success", player.getDisplayName()));
    }

    private static void showTeamManager(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.manage.title")
                .withStyle(ChatFormatting.BOLD), false);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            MutableComponent line = Component.literal("• ").append(player.getDisplayName()).append(" ");
            for (String team : List.of("red", "blue", "green", "yellow")) {
                line.append(commandButton(team, "/neobingo team assign " + player.getUUID() + " " + team));
            }
            line.append(commandButton("×", "/neobingo team remove " + player.getUUID()));
            source.sendSuccess(() -> line, false);
        }
        source.sendSuccess(() -> Component.translatable("commands.neo_bingo.manage.actions")
                .append(" ")
                .append(commandButton("↻", "/neobingo reroll"))
                .append(commandButton("■", "/neobingo end")), false);
    }

    private static Component commandButton(String label, String command) {
        return Component.literal("[" + label + "]")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    private static void start(CommandSourceStack source, long seed, GameMode mode, DifficultyTier difficulty) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        requireStartPermission(source, session);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.start(definition.size(), BingoCardDefinitions.objectives(difficulty, seed), seed, mode);
        enterMatchWorlds(source, session);
        data.store(session);
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        announce(source, Component.translatable(
                "commands.neo_bingo.start.success", BingoModeText.displayName(mode), session.seed().orElseThrow()));
    }

    private static void start(
            CommandSourceStack source,
            long seed,
            GameMode mode,
            DifficultyDistribution distribution) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        requireStartPermission(source, session);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.start(definition.size(), BingoCardDefinitions.objectives(distribution, seed), seed, mode);
        enterMatchWorlds(source, session);
        data.store(session);
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        announce(source, Component.translatable(
                "commands.neo_bingo.start.success", BingoModeText.displayName(mode), session.seed().orElseThrow()));
    }

    private static void reroll(CommandSourceStack source, long seed, DifficultyTier difficulty) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.reroll(definition.size(), BingoCardDefinitions.objectives(difficulty, seed), seed);
        data.store(session);
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        announce(source, Component.translatable("commands.neo_bingo.reroll.success", seed));
    }

    private static void reroll(CommandSourceStack source, long seed, DifficultyDistribution distribution) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.reroll(definition.size(), BingoCardDefinitions.objectives(distribution, seed), seed);
        data.store(session);
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        announce(source, Component.translatable("commands.neo_bingo.reroll.success", seed));
    }

    private static void startRanked(CommandSourceStack source, int seconds, long seed, DifficultyTier difficulty) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        requireStartPermission(source, session);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.startRanked(
                definition.size(),
                BingoCardDefinitions.objectives(difficulty, seed),
                seed,
                Math.multiplyExact(seconds, 20L));
        enterMatchWorlds(source, session);
        data.store(session);
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        announce(source, Component.translatable(
                "commands.neo_bingo.start.ranked.success", seconds, session.seed().orElseThrow()));
    }

    private static void startRanked(
            CommandSourceStack source,
            int seconds,
            long seed,
            DifficultyDistribution distribution) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        requireStartPermission(source, session);
        BingoCardDefinition definition = BingoCardDefinitions.current();
        session.startRanked(definition.size(), BingoCardDefinitions.objectives(distribution, seed), seed, seconds * 20L);
        enterMatchWorlds(source, session);
        data.store(session);
        NeoBingoNetwork.syncAllCards(session, source.getServer().getPlayerList().getPlayers());
        announce(source, Component.translatable(
                "commands.neo_bingo.start.ranked.success", seconds, session.seed().orElseThrow()));
    }

    private static void requireStartPermission(CommandSourceStack source, BingoSession session) {
        if (NeoBingoPermissions.canAdmin(source)) {
            return;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            throw failure("commands.neo_bingo.error.start_permission");
        }
        if (session.roster().teamOf(new PlayerId(player.getUUID())).isEmpty()) {
            throw failure("commands.neo_bingo.error.start_permission");
        }
    }

    private static void end(CommandSourceStack source) {
        NeoBingoSavedData data = NeoBingoSavedData.get(source.getServer());
        BingoSession session = requiredSession(data);
        session.end();
        data.store(session);
        leaveMatchWorlds(source);
        announce(source, Component.translatable("commands.neo_bingo.end.success"));
    }

    private static void enterMatchWorlds(CommandSourceStack source, BingoSession session) {
        int spawnDistance = NeoBingoSavedData.get(source.getServer()).lobbySettings().teamSpawnDistanceChunks();
        RuntimeMatchWorldManager.create(
                source.getServer(), session.seed().orElseThrow(), session.roster().teamSizes().keySet(), spawnDistance);
        MatchGameplayRules.begin(source.getServer());
        try {
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                session.roster().teamOf(new PlayerId(player.getUUID()))
                        .ifPresent(team -> {
                            RuntimeMatchWorldManager.sendToMatch(player, team);
                            MatchGameplayRules.preparePlayer(player, team);
                        });
            }
        } catch (RuntimeException exception) {
            RuntimeMatchWorldManager.finish(source.getServer());
            throw exception;
        }
    }

    private static void leaveMatchWorlds(CommandSourceStack source) {
        RuntimeMatchWorldManager.finish(source.getServer());
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
        result.winner().ifPresent(winner -> announce(
                source, Component.translatable("commands.neo_bingo.win", winner.value())));
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
                BingoModeText.displayName(game.mode()),
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

    private static void announce(CommandSourceStack source, Component message) {
        source.getServer().getPlayerList().broadcastSystemMessage(message, false);
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendSuccess(() -> message, false);
        }
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
