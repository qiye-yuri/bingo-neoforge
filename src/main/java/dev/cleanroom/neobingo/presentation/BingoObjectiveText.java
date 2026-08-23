package dev.cleanroom.neobingo.presentation;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** 将物品目标标识转换为客户端当前语言中的展示文本。 */
public final class BingoObjectiveText {
    private static final String CELL_SEPARATOR = " | ";

    private BingoObjectiveText() {
    }

    public static String displayRow(String row) {
        return Arrays.stream(row.split(" \\| ", -1))
                .map(BingoObjectiveText::displayCell)
                .collect(Collectors.joining(CELL_SEPARATOR));
    }

    public static String displayCell(String cell) {
        String marker = cell.startsWith("[✓] ") || cell.startsWith("[ ] ")
                ? cell.substring(0, 4)
                : "";
        String objective = marker.isEmpty() ? cell : cell.substring(4);
        return marker + displayObjective(objective);
    }

    public static String displayObjective(String objective) {
        return itemForObjective(objective)
                .map(item -> item.getDescription().getString())
                .orElse(objective);
    }

    public static Optional<Item> itemForCell(String cell) {
        String objective = cell.startsWith("[✓] ") || cell.startsWith("[ ] ")
                ? cell.substring(4)
                : cell;
        return itemForObjective(objective);
    }

    public static Optional<Item> itemForObjective(String objective) {
        ResourceLocation key = ResourceLocation.tryParse(objective);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return Optional.empty();
        }
        var item = BuiltInRegistries.ITEM.get(key);
        return item == Items.AIR ? Optional.empty() : Optional.of(item);
    }
}
