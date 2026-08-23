package dev.cleanroom.neobingo.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;

/** 将受支持的旧版宾果卡定义升级为当前格式。 */
public final class BingoCardDefinitionMigrator {
    private static final int LEGACY_SCHEMA_VERSION = 0;

    private BingoCardDefinitionMigrator() {
    }

    public static JsonObject migrate(JsonObject source) {
        Objects.requireNonNull(source, "source");
        JsonObject migrated = source.deepCopy();
        int version = requiredVersion(migrated);
        if (version == BingoCardDefinition.CURRENT_SCHEMA_VERSION) {
            return migrated;
        }
        if (version != LEGACY_SCHEMA_VERSION) {
            throw new IllegalArgumentException("无法迁移宾果卡定义版本：" + version);
        }
        if (!migrated.has("card_size") || migrated.has("size")) {
            throw new IllegalArgumentException("版本 0 定义必须仅使用 card_size 字段");
        }
        JsonElement size = migrated.remove("card_size");
        migrated.add("size", size);
        migrated.addProperty("schema_version", BingoCardDefinition.CURRENT_SCHEMA_VERSION);
        return migrated;
    }

    private static int requiredVersion(JsonObject object) {
        JsonElement element = object.get("schema_version");
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("schema_version 必须是整数");
        }
        try {
            return element.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("schema_version 必须是整数", exception);
        }
    }
}
