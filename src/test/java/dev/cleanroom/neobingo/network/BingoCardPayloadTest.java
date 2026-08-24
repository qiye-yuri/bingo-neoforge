package dev.cleanroom.neobingo.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BingoCardPayloadTest {
    @Test
    void clientStoresLatestStructuredCard() {
        BingoCardPayload payload = new BingoCardPayload("red", "RANKED", 4, 125, List.of("row one", "row two"));

        ClientProtocolState.accept(payload);

        assertEquals(payload, ClientProtocolState.latestCard().orElseThrow());
        assertEquals("neo_bingo:bingo_card", BingoCardPayload.TYPE.id().toString());
        assertEquals(4, payload.score());
        assertEquals(125, payload.remainingSeconds());
    }

    @Test
    void rejectsEmptyAndOversizedCards() {
        assertThrows(IllegalArgumentException.class,
                () -> new BingoCardPayload("red", "STANDARD", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new BingoCardPayload("red", "STANDARD", java.util.Collections.nCopies(10, "row")));
    }

    @Test
    void rejectsInvalidMetadataAndIrregularGrids() {
        assertThrows(IllegalArgumentException.class,
                () -> new BingoCardPayload("Invalid Team", "STANDARD", List.of("row")));
        assertThrows(IllegalArgumentException.class,
                () -> new BingoCardPayload("red", "standard", List.of("row")));
        assertThrows(IllegalArgumentException.class,
                () -> new BingoCardPayload("red", "STANDARD", List.of("a | b", "c")));
        assertThrows(IllegalArgumentException.class,
                () -> new BingoCardPayload("red", "STANDARD", List.of(String.join(
                        " | ", java.util.Collections.nCopies(10, "cell")))));
        assertThrows(IllegalArgumentException.class,
                () -> new BingoCardPayload("red", "STANDARD", List.of("x".repeat(2049))));
    }
}
