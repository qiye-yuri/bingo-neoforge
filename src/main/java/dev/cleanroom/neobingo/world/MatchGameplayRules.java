package dev.cleanroom.neobingo.world;

import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import dev.cleanroom.neobingo.persistence.TeamChestSavedData;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.UUID;

/** 执行单局夜视、死亡保留、初始物资与队伍共享箱规则。 */
public final class MatchGameplayRules {
    private static boolean nightVision;
    private static boolean keepInventory;
    private static boolean teamChest;
    private static int teamChestRows = 3;
    private static Map<String, Integer> starterItems = Map.of();
    private static final Map<UUID, SavedInventory> DEATH_INVENTORIES = new java.util.HashMap<>();

    private MatchGameplayRules() {}

    public static void begin(MinecraftServer server) {
        var settings = NeoBingoSavedData.get(server).lobbySettings();
        nightVision = settings.nightVision();
        keepInventory = settings.keepInventory();
        teamChest = settings.teamChest();
        teamChestRows = settings.teamChestRows();
        starterItems = settings.starterItems();
        TeamChestSavedData.get(server).clearAll();
        DEATH_INVENTORIES.clear();
        if (nightVision) for (ServerPlayer player : server.getPlayerList().getPlayers())
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, true));
    }

    public static void preparePlayer(ServerPlayer player, TeamId team) {
        if (nightVision) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, true));
        }
        starterItems.forEach((id, count) -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id))
                .ifPresent(item -> player.getInventory().add(new ItemStack(item, count))));
        if (teamChest) TeamChestSavedData.get(player.getServer()).inventory(team, teamChestRows);
    }

    public static void openTeamChest(ServerPlayer player, TeamId team) {
        if (!teamChest) throw new IllegalStateException("本局未开启队伍箱");
        SimpleContainer chest = TeamChestSavedData.get(player.getServer()).inventory(team, teamChestRows);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new ChestMenu(menuType(teamChestRows), id, inventory, chest, teamChestRows),
                Component.literal("Bingo 队伍箱 · " + team.value())));
    }

    /** 返回队伍箱中的物品，使其与队员背包一起参与 Bingo 自动判定。 */
    public static Set<dev.cleanroom.neobingo.domain.ObjectiveId> teamChestObjectives(MinecraftServer server, TeamId team) {
        if (!teamChest) return Set.of();
        SimpleContainer chest = TeamChestSavedData.get(server).inventory(team, teamChestRows);
        return IntStream.range(0, chest.getContainerSize())
                .mapToObj(chest::getItem)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                .map(key -> new dev.cleanroom.neobingo.domain.ObjectiveId(key.toString()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!keepInventory || !(event.getEntity() instanceof ServerPlayer player) || participantTeam(player) == null) return;
        NonNullList<ItemStack> items = NonNullList.withSize(player.getInventory().getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) items.set(slot, player.getInventory().getItem(slot).copy());
        DEATH_INVENTORIES.put(player.getUUID(), new SavedInventory(
                items, player.experienceLevel, player.totalExperience, player.experienceProgress));
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SavedInventory saved = DEATH_INVENTORIES.remove(player.getUUID());
        if (saved != null) {
            for (int slot = 0; slot < saved.items().size(); slot++)
                player.getInventory().setItem(slot, saved.items().get(slot).copy());
            player.experienceLevel = saved.level();
            player.totalExperience = saved.total();
            player.experienceProgress = saved.progress();
        }
        if (nightVision && participantTeam(player) != null)
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, true));
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        if (keepInventory && event.getEntity() instanceof ServerPlayer player && participantTeam(player) != null)
            event.setCanceled(true);
    }

    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (keepInventory && event.getEntity() instanceof ServerPlayer player && participantTeam(player) != null)
            event.setCanceled(true);
    }

    public static void end(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) player.removeEffect(MobEffects.NIGHT_VISION);
        nightVision = keepInventory = teamChest = false;
        starterItems = Map.of();
        DEATH_INVENTORIES.clear();
    }

    private static MenuType<ChestMenu> menuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };
    }

    private static TeamId participantTeam(ServerPlayer player) {
        return NeoBingoSavedData.get(player.getServer()).restoreSession()
                .flatMap(session -> session.roster().teamOf(new PlayerId(player.getUUID()))).orElse(null);
    }

    private record SavedInventory(NonNullList<ItemStack> items, int level, int total, float progress) {}
}
