package dev.cleanroom.neobingo.config;

import dev.cleanroom.neobingo.NeoBingo;
import dev.cleanroom.neobingo.domain.DifficultyCardGenerator;
import dev.cleanroom.neobingo.domain.DifficultyPreset;
import dev.cleanroom.neobingo.domain.DifficultyTier;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
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

    public static List<ObjectiveId> objectives(DifficultyPreset preset, long seed) {
        List<ObjectiveId> objectives = current().objectives();
        if (objectives.size() < 50) {
            throw new IllegalStateException("启用难度分级至少需要 50 个按难度排序的目标");
        }
        var tiers = new EnumMap<DifficultyTier, List<ObjectiveId>>(DifficultyTier.class);
        tiers.put(DifficultyTier.EASY, objectives.subList(0, 16));
        tiers.put(DifficultyTier.MEDIUM, objectives.subList(16, 26));
        tiers.put(DifficultyTier.HARD, objectives.subList(26, 34));
        tiers.put(DifficultyTier.EXTREME, objectives.subList(34, 42));
        tiers.put(DifficultyTier.IMPOSSIBLE, objectives.subList(42, 50));
        return DifficultyCardGenerator.generate(tiers, preset, seed);
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
