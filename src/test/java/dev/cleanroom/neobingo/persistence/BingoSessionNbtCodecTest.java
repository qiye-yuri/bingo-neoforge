package dev.cleanroom.neobingo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.cleanroom.neobingo.domain.BingoSession;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class BingoSessionNbtCodecTest {
    private static final PlayerId PLAYER = new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TeamId RED = new TeamId("red");

    @Test
    void snapshotSurvivesNbtRoundTrip() {
        BingoSession session = runningSession();
        session.claim(PLAYER, 0);
        session.claim(PLAYER, 6);

        CompoundTag encoded = BingoSessionNbtCodec.encode(session.snapshot());
        BingoSession restored = BingoSession.restore(BingoSessionNbtCodec.decode(encoded));

        assertEquals(session.snapshot(), restored.snapshot());
    }

    @Test
    void hiddenModeSurvivesNbtRoundTrip() {
        BingoSession session = runningSession(GameMode.HIDDEN);

        CompoundTag encoded = BingoSessionNbtCodec.encode(session.snapshot());
        BingoSession restored = BingoSession.restore(BingoSessionNbtCodec.decode(encoded));

        assertEquals(GameMode.HIDDEN, restored.game().orElseThrow().mode());
    }

    @Test
    void rankedModeSurvivesNbtRoundTrip() {
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.startRanked(5, objectives(), 42L, 1200);
        session.tickRanked();

        CompoundTag encoded = BingoSessionNbtCodec.encode(session.snapshot());
        BingoSession restored = BingoSession.restore(BingoSessionNbtCodec.decode(encoded));

        assertEquals(GameMode.RANKED, restored.game().orElseThrow().mode());
        assertEquals(1199L, restored.remainingTicks().orElseThrow());
    }

    @Test
    void rejectsUnknownSchemaVersion() {
        CompoundTag encoded = BingoSessionNbtCodec.encode(runningSession().snapshot());
        encoded.putInt("schema_version", 99);

        assertThrows(IllegalArgumentException.class, () -> BingoSessionNbtCodec.decode(encoded));
    }

    @Test
    void rejectsMissingRequiredFields() {
        CompoundTag encoded = BingoSessionNbtCodec.encode(runningSession().snapshot());
        encoded.remove("state");

        assertThrows(IllegalArgumentException.class, () -> BingoSessionNbtCodec.decode(encoded));
    }

    private static BingoSession runningSession() {
        return runningSession(GameMode.LOCKOUT);
    }

    private static BingoSession runningSession(GameMode mode) {
        BingoSession session = new BingoSession();
        session.join(PLAYER, RED);
        session.start(5, objectives(), 42L, mode);
        return session;
    }

    private static List<ObjectiveId> objectives() {
        return IntStream.range(0, 25)
                .mapToObj(index -> new ObjectiveId("test:objective_" + index))
                .toList();
    }
}
