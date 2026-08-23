package dev.cleanroom.neobingo.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class BingoObjectiveTextTest {
    @Test
    void localizesKnownItemWhilePreservingClaimMarker() {
        assertEquals("[✓] " + Items.STONE.getDescription().getString(),
                BingoObjectiveText.displayCell("[✓] minecraft:stone"));
    }

    @Test
    void localizesEveryKnownItemInRow() {
        assertEquals(
                "[ ] " + Items.STONE.getDescription().getString()
                        + " | [ ] " + Items.DIRT.getDescription().getString(),
                BingoObjectiveText.displayRow("[ ] minecraft:stone | [ ] minecraft:dirt"));
    }

    @Test
    void preservesHiddenAndUnknownObjectives() {
        assertEquals("[ ] ???", BingoObjectiveText.displayCell("[ ] ???"));
        assertEquals("[ ] future:unknown", BingoObjectiveText.displayCell("[ ] future:unknown"));
    }

    @Test
    void resolvesOnlyKnownVisibleItemObjectivesForIcons() {
        assertEquals(Items.STONE, BingoObjectiveText.itemForCell("[ ] minecraft:stone").orElseThrow());
        assertEquals(java.util.Optional.empty(), BingoObjectiveText.itemForCell("[ ] ???"));
        assertEquals(java.util.Optional.empty(), BingoObjectiveText.itemForCell("[ ] future:unknown"));
    }
}
