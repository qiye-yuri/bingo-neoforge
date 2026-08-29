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
                data.lobbySettings = new LobbyGameSettings(
                        GameMode.valueOf(settingsTag.getString("mode")), counts);
            } catch (IllegalArgumentException ignored) {
                data.lobbySettings = new LobbyGameSettings();
            }
        }
        return data;
    }
}
