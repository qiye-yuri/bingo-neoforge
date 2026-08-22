package dev.cleanroom.neobingo;

import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

/** 从服务端玩家物品栏提取可用于目标判定的物品标识。 */
public final class ServerInventoryObjectiveReader {
    private ServerInventoryObjectiveReader() {
    }

    public static Set<ObjectiveId> read(ServerPlayer player) {
        return IntStream.range(0, player.getInventory().getContainerSize())
                .mapToObj(player.getInventory()::getItem)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                .map(key -> new ObjectiveId(key.toString()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
