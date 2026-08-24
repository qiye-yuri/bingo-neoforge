package dev.cleanroom.neobingo.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProtocolVersionPayloadTest {
    @Test
    void exposesStableProtocolIdentity() {
        assertEquals("2", NeoBingoNetwork.PROTOCOL_VERSION);
        assertEquals(2, ProtocolVersionPayload.CURRENT_VERSION);
        assertEquals("neo_bingo:protocol_version", ProtocolVersionPayload.TYPE.id().toString());
    }

    @Test
    void rejectsInvalidApplicationVersion() {
        assertThrows(IllegalArgumentException.class, () -> new ProtocolVersionPayload(0));
    }

    @Test
    void clientRecordsNegotiatedApplicationVersion() {
        ClientProtocolState.accept(new ProtocolVersionPayload(1));

        assertEquals(1, ClientProtocolState.negotiatedVersion());
    }

    @Test
    void clearingConnectionStateRemovesNegotiationAndCard() {
        ClientProtocolState.accept(new ProtocolVersionPayload(1));
        ClientProtocolState.accept(new BingoCardPayload("red", "STANDARD", java.util.List.of("row")));

        ClientProtocolState.clear();

        assertEquals(0, ClientProtocolState.negotiatedVersion());
        assertEquals(java.util.Optional.empty(), ClientProtocolState.latestCard());
        assertEquals(true, ClientProtocolState.hudVisible());
        assertEquals(-1, ClientProtocolState.focusedCell());
    }

    @Test
    void clientCanToggleHudWithoutDiscardingCard() {
        BingoCardPayload card = new BingoCardPayload("red", "STANDARD", java.util.List.of("row"));
        ClientProtocolState.clear();
        ClientProtocolState.accept(card);

        ClientProtocolState.toggleHud();

        assertEquals(false, ClientProtocolState.hudVisible());
        assertEquals(card, ClientProtocolState.latestCard().orElseThrow());
    }

    @Test
    void clientCanFocusCardCellAndPreserveItAcrossUpdates() {
        ClientProtocolState.clear();
        ClientProtocolState.accept(new BingoCardPayload(
                "red", "STANDARD", java.util.List.of("[ ] stone | [✓] dirt", "[ ] sand | [ ] gravel")));

        ClientProtocolState.toggleFocusedCell(1);
        assertEquals("dirt", ClientProtocolState.focusedObjective().orElseThrow());

        ClientProtocolState.accept(new BingoCardPayload(
                "red", "STANDARD", java.util.List.of("[ ] stone | [✓] dirt", "[✓] sand | [ ] gravel")));
        assertEquals(1, ClientProtocolState.focusedCell());
        assertEquals("dirt", ClientProtocolState.focusedObjective().orElseThrow());

        ClientProtocolState.toggleFocusedCell(1);
        assertEquals(java.util.Optional.empty(), ClientProtocolState.focusedObjective());

        ClientProtocolState.focusCell(3);
        assertEquals("gravel", ClientProtocolState.focusedObjective().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> ClientProtocolState.focusCell(4));
    }

    @Test
    void clientDropsFocusWhenReplacementCardIsSmaller() {
        ClientProtocolState.clear();
        ClientProtocolState.accept(new BingoCardPayload(
                "red", "STANDARD", java.util.List.of("[ ] stone | [ ] dirt")));
        ClientProtocolState.toggleFocusedCell(1);

        ClientProtocolState.accept(new BingoCardPayload("red", "STANDARD", java.util.List.of("[ ] stone")));

        assertEquals(-1, ClientProtocolState.focusedCell());
        assertEquals(java.util.Optional.empty(), ClientProtocolState.focusedObjective());
    }
}
