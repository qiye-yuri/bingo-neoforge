package dev.cleanroom.neobingo;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.cleanroom.neobingo.domain.DifficultyTier;
import dev.cleanroom.neobingo.domain.LobbyGameSettings;
import net.minecraft.core.component.DataComponents;
import org.junit.jupiter.api.Test;

class BingoSettingsBookTest {
    @Test
    void changingSettingsRebuildsVisibleBookPages() {
        LobbyGameSettings settings = new LobbyGameSettings();
        String before = BingoSettingsBook.create(settings).get(DataComponents.WRITTEN_BOOK_CONTENT)
                .getPages(false).get(2).getString();
        settings.adjust(DifficultyTier.S, 1);
        String after = BingoSettingsBook.create(settings).get(DataComponents.WRITTEN_BOOK_CONTENT)
                .getPages(false).get(2).getString();
        assertNotEquals(before, after);
        assertTrue(after.contains("S 4"));
    }
}
