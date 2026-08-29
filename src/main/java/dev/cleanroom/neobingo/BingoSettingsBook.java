package dev.cleanroom.neobingo;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

/** 创建并发放使用原版书本界面的 Bingo 游戏设置入口。 */
public final class BingoSettingsBook {
    private static final String MARKER = "neo_bingo_settings_book";
    private static final String VERSION_MARKER = "neo_bingo_settings_book_version";
    private static final int BOOK_VERSION = 9;

    private BingoSettingsBook() {
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            giveIfMissing(player);
        }
    }

    public static boolean giveIfMissing(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (!isSettingsBook(existing)) {
                continue;
            }
            if (settingsBookVersion(existing) >= BOOK_VERSION) {
                return false;
            }
            player.getInventory().setItem(slot, create());
            return true;
        }
        ItemStack book = create();
        if (!player.getInventory().add(book)) {
            player.drop(book, false, false);
        }
        return true;
    }

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(MARKER, true);
        marker.putInt(VERSION_MARKER, BOOK_VERSION);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        book.set(DataComponents.CUSTOM_NAME, Component.translatable("item.neo_bingo.settings_book"));
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Bingo Settings"),
                "Neo Bingo",
                0,
                pages().stream().map(Filterable::passThrough).toList(),
                true));
        return book;
    }

    private static boolean isSettingsBook(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(MARKER);
    }

    private static int settingsBookVersion(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(VERSION_MARKER);
    }

    private static List<Component> pages() {
        List<Component> pages = new ArrayList<>();
        pages.add(Component.translatable("book.neo_bingo.intro")
                .append("\n\n")
                .append(button("book.neo_bingo.team.red", "/neobingo join red"))
                .append("\n")
                .append(button("book.neo_bingo.team.blue", "/neobingo join blue"))
                .append("\n")
                .append(button("book.neo_bingo.team.green", "/neobingo join green"))
                .append("\n")
                .append(button("book.neo_bingo.team.yellow", "/neobingo join yellow")));
        pages.add(Component.translatable("book.neo_bingo.mode.title").withStyle(ChatFormatting.BOLD)
                .append("\n\n").append(button("book.neo_bingo.mode.standard", "/neobingo lobby settings mode standard"))
                .append("\n").append(button("book.neo_bingo.mode.lockout", "/neobingo lobby settings mode lockout"))
                .append("\n").append(button("book.neo_bingo.mode.hidden", "/neobingo lobby settings mode hidden"))
                .append("\n").append(button("book.neo_bingo.mode.ranked.short", "/neobingo lobby settings mode ranked")));
        pages.add(difficultyPage());
        pages.add(matchOptionsPage());
        pages.add(ruleOptionsPage());
        pages.add(starterKitPage());
        pages.add(Component.translatable("book.neo_bingo.lobby_card").withStyle(ChatFormatting.BOLD)
                .append("\n\n").append(button("book.neo_bingo.action.settings", "/neobingo lobby settings"))
                .append("\n").append(button("book.neo_bingo.action.preview", "/neobingo lobby preview"))
                .append("\n").append(button("book.neo_bingo.action.refresh", "/neobingo lobby refresh"))
                .append("\n\n").append(button("book.neo_bingo.action.start", "/neobingo lobby start")));
        pages.add(Component.translatable("book.neo_bingo.tools")
                .append("\n\n")
                .append(button("book.neo_bingo.action.claim", "/neobingo claim"))
                .append("\n")
                .append(button("book.neo_bingo.action.team_chest", "/neobingo teamchest"))
                .append("\n")
                .append(button("book.neo_bingo.action.status", "/neobingo status"))
                .append("\n")
                .append(button("book.neo_bingo.action.leave", "/neobingo leave")));
        pages.add(Component.translatable("book.neo_bingo.randomteams")
                .append("\n\n")
                .append(button("book.neo_bingo.randomteams.two", "/neobingo randomteams 2"))
                .append("\n")
                .append(button("book.neo_bingo.randomteams.three", "/neobingo randomteams 3"))
                .append("\n")
                .append(button("book.neo_bingo.randomteams.four", "/neobingo randomteams 4"))
                .append("\n\n")
                .append(Component.translatable("book.neo_bingo.admin_only").withStyle(ChatFormatting.DARK_GRAY)));
        pages.add(Component.translatable("book.neo_bingo.admin.title")
                .withStyle(ChatFormatting.BOLD)
                .append("\n")
                .append(Component.translatable("book.neo_bingo.admin.description")
                        .withStyle(ChatFormatting.DARK_GRAY))
                .append("\n\n")
                .append(button("book.neo_bingo.admin.manage", "/neobingo manage"))
                .append("\n\n")
                .append(Component.translatable("book.neo_bingo.admin.confirm")
                        .withStyle(ChatFormatting.DARK_GRAY)));
        return List.copyOf(pages);
    }

    /** 六档数量集中在同一页，每次点击精确增减一个格子。 */
    private static Component difficultyPage() {
        MutableComponent page = Component.translatable("book.neo_bingo.difficulty_editor_all")
                .withStyle(ChatFormatting.BOLD);
        for (String tier : List.of("max", "s", "a", "b", "c", "d")) {
            String prefix = "/neobingo lobby settings adjust " + tier + " ";
            page.append("\n").append(Component.literal(tier.toUpperCase() + " "))
                    .append(literalButton("−", prefix + "-1"))
                    .append(" ")
                    .append(literalButton("+", prefix + "1"));
        }
        page.append("\n\n").append(Component.translatable("book.neo_bingo.adjust.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        return page;
    }

    private static Component matchOptionsPage() {
        MutableComponent page = Component.translatable("book.neo_bingo.match_options")
                .withStyle(ChatFormatting.BOLD);
        page.append("\n\n").append(Component.translatable("book.neo_bingo.timed_minutes"));
        page.append("\n").append(literalButton("−5", "/neobingo lobby settings time -300"))
                .append(" ").append(literalButton("−1", "/neobingo lobby settings time -60"))
                .append(" ").append(literalButton("+1", "/neobingo lobby settings time 60"))
                .append(" ").append(literalButton("+5", "/neobingo lobby settings time 300"));
        page.append("\n\n").append(Component.translatable("book.neo_bingo.spawn_distance"));
        page.append("\n").append(literalButton("−4", "/neobingo lobby settings spawn_distance -4"))
                .append(" ").append(literalButton("−1", "/neobingo lobby settings spawn_distance -1"))
                .append(" ").append(literalButton("+1", "/neobingo lobby settings spawn_distance 1"))
                .append(" ").append(literalButton("+4", "/neobingo lobby settings spawn_distance 4"));
        page.append("\n\n").append(Component.translatable("book.neo_bingo.options_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        return page;
    }

    private static Component ruleOptionsPage() {
        return Component.translatable("book.neo_bingo.rule_options").withStyle(ChatFormatting.BOLD)
                .append("\n\n").append(button("book.neo_bingo.rule.night_vision", "/neobingo lobby settings toggle night_vision"))
                .append("\n\n").append(button("book.neo_bingo.rule.keep_inventory", "/neobingo lobby settings toggle keep_inventory"))
                .append("\n\n").append(button("book.neo_bingo.rule.team_chest", "/neobingo lobby settings toggle team_chest"))
                .append("\n\n").append(Component.translatable("book.neo_bingo.rule_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component starterKitPage() {
        MutableComponent page = Component.translatable("book.neo_bingo.starter_kit").withStyle(ChatFormatting.BOLD);
        appendKitRow(page, "item.minecraft.bread", "minecraft:bread", 4);
        appendKitRow(page, "block.minecraft.oak_log", "minecraft:oak_log", 8);
        appendKitRow(page, "block.minecraft.cobblestone", "minecraft:cobblestone", 16);
        appendKitRow(page, "block.minecraft.torch", "minecraft:torch", 8);
        appendKitRow(page, "item.minecraft.iron_ingot", "minecraft:iron_ingot", 4);
        appendKitRow(page, "item.minecraft.cooked_beef", "minecraft:cooked_beef", 4);
        return page;
    }

    private static void appendKitRow(MutableComponent page, String nameKey, String item, int step) {
        String prefix = "/neobingo lobby settings kit " + item + " ";
        page.append("\n").append(Component.translatable(nameKey).append(" "))
                .append(literalButton("−", prefix + -step)).append(" ")
                .append(literalButton("+", prefix + step));
    }

    private static Component button(String translationKey, String command) {
        return Component.literal("[ ")
                .append(Component.translatable(translationKey))
                .append(" ]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.DARK_GREEN)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    private static Component literalButton(String label, String command) {
        return Component.literal("[" + label + "]")
                .withStyle(style -> style.withColor(ChatFormatting.DARK_GREEN)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

}
