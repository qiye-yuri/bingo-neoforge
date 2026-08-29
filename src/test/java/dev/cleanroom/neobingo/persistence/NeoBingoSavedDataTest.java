package dev.cleanroom.neobingo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.DifficultyTier;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class NeoBingoSavedDataTest {
    @Test
    void saveAndLoadRestoresSessionAndDirtyState() {
        BingoSession session = new BingoSession();
        session.join(
                new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                new TeamId("red"));
        List<ObjectiveId> objectives = IntStream.range(0, 25)
                .mapToObj(index -> new ObjectiveId("test:objective_" + index))
                .toList();
        session.start(5, objectives, 42L, GameMode.STANDARD);

        NeoBingoSavedData data = new NeoBingoSavedData();
        data.store(session);
        data.lobbySettings().mode(GameMode.HIDDEN);
        data.lobbySettings().adjust(DifficultyTier.S, 1);
        data.lobbySettings().adjust(DifficultyTier.D, -1);
        data.lobbySettings().adjustTimedSeconds(300);
        data.lobbySettings().adjustTeamSpawnDistanceChunks(4);
        data.lobbySettingsChanged();
        assertTrue(data.isDirty());

        CompoundTag encoded = data.save(new CompoundTag(), null);
        NeoBingoSavedData loaded = NeoBingoSavedData.load(encoded, null);
        assertEquals(session.snapshot(), loaded.restoreSession().orElseThrow().snapshot());
        assertEquals(GameMode.HIDDEN, loaded.lobbySettings().mode());
        assertEquals(4, loaded.lobbySettings().count(DifficultyTier.S));
        assertEquals(6, loaded.lobbySettings().count(DifficultyTier.D));
        assertEquals(1200, loaded.lobbySettings().timedSeconds());
        assertEquals(12, loaded.lobbySettings().teamSpawnDistanceChunks());

        loaded.clear();
        assertTrue(loaded.isDirty());
        assertFalse(loaded.restoreSession().isPresent());
    }
}
