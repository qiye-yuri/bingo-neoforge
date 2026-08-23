package dev.cleanroom.neobingo.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProtocolVersionPayloadTest {
    @Test
    void exposesStableProtocolIdentity() {
        assertEquals("1", NeoBingoNetwork.PROTOCOL_VERSION);
        assertEquals(1, ProtocolVersionPayload.CURRENT_VERSION);
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
}
