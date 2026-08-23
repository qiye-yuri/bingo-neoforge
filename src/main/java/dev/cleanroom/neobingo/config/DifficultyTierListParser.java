package dev.cleanroom.neobingo.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cleanroom.neobingo.domain.DifficultyTier;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.io.Reader;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 读取兼容上游 S/A/B/C/D 格式的物品难度列表。 */
public final class DifficultyTierListParser {
    private static final Map<DifficultyTier, String> LABELS = Map.of(
            DifficultyTier.MAX, "MAX",
            DifficultyTier.S, "S",
            DifficultyTier.A, "A",
            DifficultyTier.B, "B",
            DifficultyTier.C, "C",
            DifficultyTier.D, "D");

    private DifficultyTierListParser() {
    }

    public static DifficultyTierList parse(Reader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("难度列表根节点必须是对象");
        }
        JsonObject object = root.getAsJsonObject();
        Map<DifficultyTier, List<ObjectiveId>> tiers = new EnumMap<>(DifficultyTier.class);
        LABELS.forEach((tier, label) -> tiers.put(tier, objectives(object.getAsJsonArray(label))));
        JsonArray groups = object.getAsJsonArray("groups");
        List<List<ObjectiveId>> exclusions = groups == null ? List.of() : groups.asList().stream()
                .map(JsonElement::getAsJsonArray)
                .map(DifficultyTierListParser::exclusionObjectives)
                .filter(group -> group.size() > 1)
                .toList();
        return new DifficultyTierList(Map.copyOf(tiers), exclusions);
    }

    private static List<ObjectiveId> objectives(JsonArray array) {
        if (array == null) {
            throw new IllegalArgumentException("难度列表缺少必要等级");
        }
        return array.asList().stream().map(JsonElement::getAsString).map(ObjectiveId::new).toList();
    }

    private static List<ObjectiveId> exclusionObjectives(JsonArray array) {
        return array.asList().stream()
                .map(JsonElement::getAsString)
                .filter(value -> !value.startsWith("#"))
                .map(ObjectiveId::new)
                .toList();
    }
}
