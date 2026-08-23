package dev.cleanroom.neobingo.config;

import dev.cleanroom.neobingo.NeoBingo;
import java.io.IOException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** 保存最近一次服务端资源重载所验证的默认 Bingo 卡定义。 */
public final class BingoCardDefinitions {
    private static final ResourceLocation DEFAULT_DEFINITION =
            ResourceLocation.fromNamespaceAndPath(NeoBingo.MOD_ID, "bingo_cards/default.json");
    private static volatile BingoCardDefinition current;

    private BingoCardDefinitions() {
    }

    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) BingoCardDefinitions::reload);
    }

    public static BingoCardDefinition current() {
        BingoCardDefinition definition = current;
        if (definition == null) {
            throw new IllegalStateException("Bingo 卡定义尚未加载");
        }
        return definition;
    }

    private static void reload(ResourceManager resources) {
        try (var reader = resources.openAsReader(DEFAULT_DEFINITION)) {
            BingoCardDefinition loaded = BingoCardDefinitionParser.parse(reader);
            loaded.objectives().forEach(objective -> {
                ResourceLocation key = ResourceLocation.parse(objective.value());
                if (!BuiltInRegistries.ITEM.containsKey(key) || BuiltInRegistries.ITEM.get(key) == Items.AIR) {
                    throw new IllegalArgumentException("Bingo 卡定义包含无效物品：" + objective.value());
                }
            });
            current = loaded;
            NeoBingo.LOGGER.info("已加载 Bingo 卡定义：{}×{}，目标数 {}",
                    loaded.size(), loaded.size(), loaded.objectives().size());
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取默认 Bingo 卡定义", exception);
        }
    }
}
