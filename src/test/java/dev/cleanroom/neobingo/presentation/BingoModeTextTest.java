package dev.cleanroom.neobingo.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.cleanroom.neobingo.domain.GameMode;
import org.junit.jupiter.api.Test;

class BingoModeTextTest {
    @Test
    void mapsEveryKnownModeToStableTranslationKey() {
        assertEquals("commands.neo_bingo.mode.standard", BingoModeText.displayName(GameMode.STANDARD).getString());
        assertEquals("commands.neo_bingo.mode.lockout", BingoModeText.displayName(GameMode.LOCKOUT).getString());
        assertEquals("commands.neo_bingo.mode.hidden", BingoModeText.displayName(GameMode.HIDDEN).getString());
        assertEquals("commands.neo_bingo.mode.ranked", BingoModeText.displayName(GameMode.RANKED).getString());
    }

    @Test
    void preservesUnknownProtocolModeForForwardCompatibility() {
        assertEquals("FUTURE_MODE", BingoModeText.displayName("FUTURE_MODE").getString());
    }
}
