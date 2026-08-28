package dev.cleanroom.neobingo.config;

import dev.cleanroom.neobingo.NeoBingo;
import dev.cleanroom.neobingo.domain.DifficultyCardGenerator;
import dev.cleanroom.neobingo.domain.DifficultyTier;
import dev.cleanroom.neobingo.domain.DifficultyDistribution;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    private static final ResourceLocation DIFFICULTY_TIERS =
            ResourceLocation.fromNamespaceAndPath(NeoBingo.MOD_ID, "bingo_cards/difficulty_tiers.json");
    private static volatile BingoCardDefinition current;
    private static volatile DifficultyTierList currentTiers;

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

    public static List<ObjectiveId> objectives(DifficultyTier tier, long seed) {
        DifficultyTierList tierList = currentTiers;
        if (tierList == null) {
            throw new IllegalStateException("Bingo 难度列表尚未加载");
        }
        return DifficultyCardGenerator.generate(tierList.tiers(), tierList.exclusionGroups(), tier, seed);
    }

    public static List<ObjectiveId> objectives(DifficultyDistribution distribution, long seed) {
        DifficultyTierList tierList = currentTiers;
        if (tierList == null) {
            throw new IllegalStateException("Bingo 难度列表尚未加载");
        }
        return DifficultyCardGenerator.generate(
                tierList.tiers(), tierList.exclusionGroups(), distribution, seed);
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
            try (var tierReader = resources.openAsReader(DIFFICULTY_TIERS)) {
                DifficultyTierList parsed = DifficultyTierListParser.parse(tierReader);
                var validTiers = new java.util.EnumMap<dev.cleanroom.neobingo.domain.DifficultyTier,
                        List<ObjectiveId>>(dev.cleanroom.neobingo.domain.DifficultyTier.class);
                parsed.tiers().forEach((tier, entries) -> validTiers.put(tier, entries.stream()
                        .filter(BingoCardDefinitions::isValidItem)
                        .toList()));
                List<List<ObjectiveId>> validGroups = parsed.exclusionGroups().stream()
                        .map(group -> group.stream().filter(BingoCardDefinitions::isValidItem).toList())
                        .filter(group -> group.size() > 1)
                        .toList();
                currentTiers = new DifficultyTierList(Map.copyOf(validTiers), validGroups);
            }
            NeoBingo.LOGGER.info("已加载 Bingo 卡定义：{}×{}，目标数 {}",
                    loaded.size(), loaded.size(), loaded.objectives().size());
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取默认 Bingo 卡定义", exception);
        }
    }

    private static boolean isValidItem(ObjectiveId objective) {
        ResourceLocation key = ResourceLocation.tryParse(objective.value());
        return key != null && BuiltInRegistries.ITEM.containsKey(key) && BuiltInRegistries.ITEM.get(key) != Items.AIR;
    }
}
