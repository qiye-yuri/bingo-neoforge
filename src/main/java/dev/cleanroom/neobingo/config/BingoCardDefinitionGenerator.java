package dev.cleanroom.neobingo.config;

import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** 根据命令行参数生成当前版本的宾果卡定义。 */
public final class BingoCardDefinitionGenerator {
    private BingoCardDefinitionGenerator() {
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length < 3) {
            throw new IllegalArgumentException("用法：<输出文件> <边长> <目标标识>...");
        }
        Path output = Path.of(arguments[0]).toAbsolutePath().normalize();
        int size;
        try {
            size = Integer.parseInt(arguments[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("边长必须是整数", exception);
        }
        var objectives = Arrays.stream(arguments, 2, arguments.length)
                .map(ObjectiveId::new)
                .toList();
        var definition = new BingoCardDefinition(
                BingoCardDefinition.CURRENT_SCHEMA_VERSION,
                size,
                objectives);
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (var writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            BingoCardDefinitionWriter.write(definition, writer);
        }
    }
}
