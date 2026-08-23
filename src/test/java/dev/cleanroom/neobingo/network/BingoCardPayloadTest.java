package dev.cleanroom.neobingo.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BingoCardPayloadTest {
    @Test
    void clientStoresLatestStructuredCard() {
        BingoCardPayload payload = new BingoCardPayload("red", "STANDARD", List.of("row one", "row two"));

        ClientProtocolState.accept(payload);

        assertEquals(payload, ClientProtocolState.latestCard().orElseThrow());
        assertEquals("neo_bingo:bingo_card", BingoCardPayload.TYPE.id().toString());
    }

    @Test
    void rejectsEmptyAndOversizedCards() {
        assertThrows(IllegalArgumentException.class,
                () -> new BingoCardPayload("red", "STANDARD", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new BingoCardPayload("red", "STANDARD", java.util.Collections.nCopies(10, "row")));
    }
}
