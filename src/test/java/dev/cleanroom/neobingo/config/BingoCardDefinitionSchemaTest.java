package dev.cleanroom.neobingo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BingoCardDefinitionSchemaTest {
    @Test
    void schemaDocumentsCurrentStrictContract() throws Exception {
        JsonObject schema = resource("/data/neo_bingo/bingo_cards/schema.json");
        JsonObject properties = schema.getAsJsonObject("properties");

        assertEquals(BingoCardDefinition.CURRENT_SCHEMA_VERSION,
                properties.getAsJsonObject("schema_version").get("const").getAsInt());
        assertEquals(1, properties.getAsJsonObject("size").get("minimum").getAsInt());
        assertEquals(9, properties.getAsJsonObject("size").get("maximum").getAsInt());
        assertFalse(schema.get("additionalProperties").getAsBoolean());
        assertEquals(3, schema.getAsJsonArray("required").size());
    }

    @Test
    void bundledDefinitionConformsToParserAndSchemaLimits() throws Exception {
        JsonObject schema = resource("/data/neo_bingo/bingo_cards/schema.json");
        JsonObject bundled = resource("/data/neo_bingo/bingo_cards/default.json");
        BingoCardDefinition parsed = BingoCardDefinitionParser.parse(
                new java.io.StringReader(bundled.toString()));

        JsonObject sizeSchema = schema.getAsJsonObject("properties").getAsJsonObject("size");
        assertTrue(parsed.size() >= sizeSchema.get("minimum").getAsInt());
        assertTrue(parsed.size() <= sizeSchema.get("maximum").getAsInt());
        assertFalse(parsed.objectives().isEmpty());
    }

    private static JsonObject resource(String path) throws Exception {
        var stream = BingoCardDefinitionSchemaTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
