package dev.cleanroom.neobingo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BingoCardDefinitionParserTest {
    @Test
    void parsesVersionedDefinition() {
        BingoCardDefinition definition = parse(1, 2, objectives(4));

        assertEquals(1, definition.schemaVersion());
        assertEquals(2, definition.size());
        assertEquals(4, definition.objectives().size());
    }

    @Test
    void rejectsUnknownSchemaVersionAndInsufficientPool() {
        assertThrows(IllegalArgumentException.class, () -> parse(2, 2, objectives(4)));
        assertThrows(IllegalArgumentException.class, () -> parse(1, 3, objectives(8)));
    }

    @Test
    void rejectsDuplicateObjectivesAndUnknownFields() {
        assertThrows(IllegalArgumentException.class,
                () -> parse(1, 1, "[\"minecraft:stone\",\"minecraft:stone\"]"));
        assertThrows(IllegalArgumentException.class, () -> BingoCardDefinitionParser.parse(new StringReader(
                "{\"schema_version\":1,\"size\":1,\"objectives\":[\"minecraft:stone\"],\"extra\":true}")));
    }

    @Test
    void rejectsFractionalAndNonNumericSizes() {
        assertThrows(IllegalArgumentException.class, () -> BingoCardDefinitionParser.parse(new StringReader(
                "{\"schema_version\":1,\"size\":2.5,\"objectives\":[]}")));
        assertThrows(IllegalArgumentException.class, () -> BingoCardDefinitionParser.parse(new StringReader(
                "{\"schema_version\":1,\"size\":\"5\",\"objectives\":[]}")));
    }

    private static BingoCardDefinition parse(int version, int size, String objectives) {
        return BingoCardDefinitionParser.parse(new StringReader(
                "{\"schema_version\":" + version + ",\"size\":" + size + ",\"objectives\":" + objectives + "}"));
    }

    private static String objectives(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> "\"minecraft:item_" + index + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
