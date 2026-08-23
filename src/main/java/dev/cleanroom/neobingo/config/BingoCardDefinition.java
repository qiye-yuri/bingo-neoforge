package dev.cleanroom.neobingo.config;

import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.util.List;
import java.util.Objects;

/** 描述一个带版本号且可由数据包替换的 Bingo 卡目标池。 */
public record BingoCardDefinition(int schemaVersion, int size, List<ObjectiveId> objectives) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public BingoCardDefinition {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("不支持的 Bingo 卡定义版本：" + schemaVersion);
        }
        if (size < 1 || size > 9) {
            throw new IllegalArgumentException("Bingo 卡边长必须介于 1 到 9");
        }
        objectives = List.copyOf(Objects.requireNonNull(objectives, "objectives"));
        if (objectives.stream().distinct().count() != objectives.size()) {
            throw new IllegalArgumentException("Bingo 卡目标不能重复");
        }
        if (objectives.size() < Math.multiplyExact(size, size)) {
            throw new IllegalArgumentException("目标数量不足以生成指定大小的 Bingo 卡");
        }
    }
}
