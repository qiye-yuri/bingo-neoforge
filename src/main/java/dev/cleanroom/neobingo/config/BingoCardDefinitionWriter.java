package dev.cleanroom.neobingo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/** 将当前版本的宾果卡定义写为稳定且便于审阅的 JSON。 */
public final class BingoCardDefinitionWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BingoCardDefinitionWriter() {
    }

    public static void write(BingoCardDefinition definition, Writer writer) throws IOException {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(writer, "writer");
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", definition.schemaVersion());
        root.addProperty("size", definition.size());
        JsonArray objectives = new JsonArray();
        definition.objectives().forEach(objective -> objectives.add(objective.value()));
        root.add("objectives", objectives);
        GSON.toJson(root, writer);
        writer.write(System.lineSeparator());
    }
}
