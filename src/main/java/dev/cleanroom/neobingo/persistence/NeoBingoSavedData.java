package dev.cleanroom.neobingo.persistence;

import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.BingoSessionSnapshot;
import dev.cleanroom.neobingo.domain.DifficultyTier;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.LobbyGameSettings;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** 将全局 Bingo 会话保存到主世界 data 目录。 */
public final class NeoBingoSavedData extends SavedData {
    private static final String DATA_NAME = "neo_bingo_session";
    private static final String SESSION_KEY = "session";
    private static final String LOBBY_SETTINGS_KEY = "lobby_settings";
    private static final Factory<NeoBingoSavedData> FACTORY =
            new Factory<>(NeoBingoSavedData::new, NeoBingoSavedData::load);

    private BingoSessionSnapshot snapshot;
    private LobbyGameSettings lobbySettings = new LobbyGameSettings();

    public static NeoBingoSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void store(BingoSession session) {
        Objects.requireNonNull(session, "session");
        snapshot = session.snapshot();
        setDirty();
    }

    public Optional<BingoSession> restoreSession() {
        return Optional.ofNullable(snapshot).map(BingoSession::restore);
    }

    public LobbyGameSettings lobbySettings() {
        return lobbySettings;
    }

    public void lobbySettingsChanged() {
        setDirty();
    }

    public void clear() {
        snapshot = null;
        lobbySettings = new LobbyGameSettings();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (snapshot != null) {
            tag.put(SESSION_KEY, BingoSessionNbtCodec.encode(snapshot));
        }
        CompoundTag settingsTag = new CompoundTag();
        settingsTag.putString("mode", lobbySettings.mode().name());
        settingsTag.putInt("timed_seconds", lobbySettings.timedSeconds());
        settingsTag.putInt("team_spawn_distance_chunks", lobbySettings.teamSpawnDistanceChunks());
        settingsTag.putBoolean("night_vision", lobbySettings.nightVision());
        settingsTag.putBoolean("keep_inventory", lobbySettings.keepInventory());
        settingsTag.putBoolean("team_chest", lobbySettings.teamChest());
        settingsTag.putInt("team_chest_rows", lobbySettings.teamChestRows());
        CompoundTag kitTag = new CompoundTag();
        lobbySettings.starterItems().forEach(kitTag::putInt);
        settingsTag.put("starter_items", kitTag);
        for (DifficultyTier tier : DifficultyTier.values()) {
            settingsTag.putInt(tier.name().toLowerCase(), lobbySettings.count(tier));
        }
        tag.put(LOBBY_SETTINGS_KEY, settingsTag);
        return tag;
    }

    static NeoBingoSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        NeoBingoSavedData data = new NeoBingoSavedData();
        if (tag.contains(SESSION_KEY, Tag.TAG_COMPOUND)) {
            data.snapshot = BingoSessionNbtCodec.decode(tag.getCompound(SESSION_KEY));
        }
        if (tag.contains(LOBBY_SETTINGS_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag settingsTag = tag.getCompound(LOBBY_SETTINGS_KEY);
            EnumMap<DifficultyTier, Integer> counts = new EnumMap<>(DifficultyTier.class);
            for (DifficultyTier tier : DifficultyTier.values()) {
                counts.put(tier, settingsTag.getInt(tier.name().toLowerCase()));
            }
            try {
                java.util.Map<String, Integer> starterItems = new java.util.LinkedHashMap<>();
                CompoundTag kitTag = settingsTag.getCompound("starter_items");
                for (String key : kitTag.getAllKeys()) starterItems.put(key, kitTag.getInt(key));
                data.lobbySettings = new LobbyGameSettings(
                        GameMode.valueOf(settingsTag.getString("mode")),
                        counts,
                        settingsTag.contains("timed_seconds") ? settingsTag.getInt("timed_seconds") : 900,
                        settingsTag.contains("team_spawn_distance_chunks")
                                ? settingsTag.getInt("team_spawn_distance_chunks") : 8,
                        settingsTag.getBoolean("night_vision"),
                        settingsTag.getBoolean("keep_inventory"),
                        settingsTag.getBoolean("team_chest"),
                        settingsTag.contains("team_chest_rows") ? settingsTag.getInt("team_chest_rows") : 3,
                        starterItems);
            } catch (IllegalArgumentException ignored) {
                data.lobbySettings = new LobbyGameSettings();
            }
        }
        return data;
    }
}
