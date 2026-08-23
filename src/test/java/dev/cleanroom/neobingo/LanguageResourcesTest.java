package dev.cleanroom.neobingo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LanguageResourcesTest {
    @Test
    void englishAndChineseContainMatchingKeysAndArguments() {
        JsonObject english = load("en_us");
        JsonObject chinese = load("zh_cn");

        assertEquals(english.keySet(), chinese.keySet());
        assertTrue(english.keySet().stream().allMatch(key ->
                key.startsWith("commands.neo_bingo.")
                        || key.startsWith("mod.neo_bingo.")
                        || key.startsWith("key.neo_bingo.")
                        || key.startsWith("key.categories.neo_bingo")
                        || key.startsWith("hud.neo_bingo.")
                        || key.startsWith("screen.neo_bingo.")));
        english.keySet().forEach(key -> assertEquals(
                placeholderCount(english.get(key).getAsString()),
                placeholderCount(chinese.get(key).getAsString()),
                key));
    }

    @Test
    void chineseKeepsBingoNameUntranslated() {
        String chinese = load("zh_cn").toString();

        assertTrue(chinese.contains("Bingo"));
        assertFalse(chinese.contains("宾果"));
    }

    private static JsonObject load(String language) {
        String path = "/assets/neo_bingo/lang/" + language + ".json";
        InputStream stream = LanguageResourcesTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("无法读取语言资源 " + path, exception);
        }
    }

    private static int placeholderCount(String value) {
        return (value.length() - value.replace("%s", "").length()) / 2;
    }
}
