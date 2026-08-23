package dev.cleanroom.neobingo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BingoCardDefinitionWriterTest {
    @Test
    void generatedJsonSurvivesStrictParserRoundTrip() throws Exception {
        BingoCardDefinition expected = definition();
        StringWriter output = new StringWriter();

        BingoCardDefinitionWriter.write(expected, output);

        assertEquals(expected, BingoCardDefinitionParser.parse(new StringReader(output.toString())));
    }

    @Test
    void commandLineGeneratorCreatesParentDirectories(@TempDir java.nio.file.Path directory) throws Exception {
        var output = directory.resolve("nested/card.json");

        BingoCardDefinitionGenerator.main(new String[] {
                output.toString(), "2",
                "minecraft:stone", "minecraft:dirt", "minecraft:apple", "minecraft:bread"
        });

        try (var reader = Files.newBufferedReader(output)) {
            assertEquals(definition(), BingoCardDefinitionParser.parse(reader));
        }
    }

    @Test
    void commandLineGeneratorRejectsIncompleteArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> BingoCardDefinitionGenerator.main(new String[] {"card.json", "5"}));
    }

    private static BingoCardDefinition definition() {
        return new BingoCardDefinition(1, 2, List.of(
                new ObjectiveId("minecraft:stone"),
                new ObjectiveId("minecraft:dirt"),
                new ObjectiveId("minecraft:apple"),
                new ObjectiveId("minecraft:bread")));
    }
}
