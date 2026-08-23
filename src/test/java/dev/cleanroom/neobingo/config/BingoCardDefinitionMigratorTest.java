package dev.cleanroom.neobingo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class BingoCardDefinitionMigratorTest {
    @Test
    void migratesVersionZeroWithoutMutatingSource() {
        JsonObject source = JsonParser.parseString(
                "{\"schema_version\":0,\"card_size\":1,\"objectives\":[\"minecraft:stone\"]}")
                .getAsJsonObject();

        JsonObject migrated = BingoCardDefinitionMigrator.migrate(source);

        assertEquals(0, source.get("schema_version").getAsInt());
        assertEquals(1, migrated.get("schema_version").getAsInt());
        assertEquals(1, migrated.get("size").getAsInt());
        assertEquals(1, BingoCardDefinitionParser.parse(new StringReader(source.toString())).size());
    }

    @Test
    void rejectsUnknownAndAmbiguousLegacyVersions() {
        assertThrows(IllegalArgumentException.class, () -> BingoCardDefinitionMigrator.migrate(
                JsonParser.parseString("{\"schema_version\":9}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> BingoCardDefinitionMigrator.migrate(
                JsonParser.parseString("{\"schema_version\":0,\"card_size\":1,\"size\":1}")
                        .getAsJsonObject()));
    }
}
