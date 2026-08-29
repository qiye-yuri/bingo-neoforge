package dev.cleanroom.neobingo.world;

import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.TeamId;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 执行单局夜视、死亡保留、初始物资与队伍共享箱规则。 */
public final class MatchGameplayRules {
    private static boolean nightVision;
    private static boolean keepInventory;
    private static boolean teamChest;
    private static Map<String, Integer> starterItems = Map.of();
    private static final Map<TeamId, SimpleContainer> TEAM_CHESTS = new HashMap<>();
    private static final Map<UUID, SavedInventory> DEATH_INVENTORIES = new HashMap<>();

    private MatchGameplayRules() {}

    public static void begin(MinecraftServer server) {
        var settings = NeoBingoSavedData.get(server).lobbySettings();
        nightVision = settings.nightVision();
        keepInventory = settings.keepInventory();
        teamChest = settings.teamChest();
        starterItems = settings.starterItems();
        TEAM_CHESTS.clear();
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
        if (teamChest) TEAM_CHESTS.computeIfAbsent(team, ignored -> new SimpleContainer(54));
    }

    public static void openTeamChest(ServerPlayer player, TeamId team) {
        if (!teamChest) throw new IllegalStateException("本局未开启队伍箱");
        SimpleContainer chest = TEAM_CHESTS.computeIfAbsent(team, ignored -> new SimpleContainer(54));
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> ChestMenu.sixRows(id, inventory, chest),
                Component.literal("Bingo 队伍箱 · " + team.value())));
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
        TEAM_CHESTS.clear();
        DEATH_INVENTORIES.clear();
    }

    private static TeamId participantTeam(ServerPlayer player) {
        return NeoBingoSavedData.get(player.getServer()).restoreSession()
                .flatMap(session -> session.roster().teamOf(new PlayerId(player.getUUID()))).orElse(null);
    }

    private record SavedInventory(NonNullList<ItemStack> items, int level, int total, float progress) {}
}
