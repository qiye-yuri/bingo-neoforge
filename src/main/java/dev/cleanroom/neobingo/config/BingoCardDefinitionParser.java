package dev.cleanroom.neobingo.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.io.Reader;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 将严格的版本化 JSON 转换为宾果卡定义。 */
public final class BingoCardDefinitionParser {
    private static final Set<String> ALLOWED_FIELDS = Set.of("schema_version", "size", "objectives");

    private BingoCardDefinitionParser() {
    }

    public static BingoCardDefinition parse(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("宾果卡定义根节点必须是对象");
        }
        JsonObject object = BingoCardDefinitionMigrator.migrate(root.getAsJsonObject());
        if (!ALLOWED_FIELDS.containsAll(object.keySet())) {
            throw new IllegalArgumentException("宾果卡定义包含未知字段");
        }
        int schemaVersion = requiredInteger(object, "schema_version");
        int size = requiredInteger(object, "size");
        JsonElement objectiveElement = object.get("objectives");
        if (objectiveElement == null || !objectiveElement.isJsonArray()) {
            throw new IllegalArgumentException("objectives 必须是数组");
        }
        List<ObjectiveId> objectives = objectiveElement.getAsJsonArray().asList().stream()
                .map(BingoCardDefinitionParser::requiredString)
                .map(ObjectiveId::new)
                .toList();
        return new BingoCardDefinition(schemaVersion, size, objectives);
    }

    private static int requiredInteger(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(name + " 必须是整数");
        }
        try {
            return element.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " 必须是整数", exception);
        }
    }

    private static String requiredString(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("每个目标都必须是命名空间标识字符串");
        }
        return element.getAsString();
    }
}
