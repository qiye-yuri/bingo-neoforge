package dev.cleanroom.neobingo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NeoBingoPermissionsTest {
    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void exposesStablePermissionNodeNames() {
        assertEquals("neo_bingo.command.play", NeoBingoPermissions.PLAY.getNodeName());
        assertEquals("neo_bingo.command.admin", NeoBingoPermissions.ADMIN.getNodeName());
    }

    @Test
    void defaultsToPublicPlayAndOnlineOperatorAdministration() {
        assertTrue(NeoBingoPermissions.PLAY.getDefaultResolver().resolve(null, PLAYER_ID));
        assertFalse(NeoBingoPermissions.ADMIN.getDefaultResolver().resolve(null, PLAYER_ID));
    }
}
