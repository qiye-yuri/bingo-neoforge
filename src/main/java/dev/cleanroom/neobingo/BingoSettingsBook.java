package dev.cleanroom.neobingo;

import dev.cleanroom.neobingo.domain.DifficultyTier;
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
    private static final int BOOK_VERSION = 2;
    private static final int DEFAULT_RANKED_SECONDS = 900;

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
        pages.add(modePage("book.neo_bingo.mode.standard", "standard"));
        pages.add(modePage("book.neo_bingo.mode.lockout", "lockout"));
        pages.add(modePage("book.neo_bingo.mode.hidden", "hidden"));
        pages.add(rankedPage());
        pages.add(Component.translatable("book.neo_bingo.tools")
                .append("\n\n")
                .append(button("book.neo_bingo.action.card", "/neobingo card"))
                .append("\n")
                .append(button("book.neo_bingo.action.claim", "/neobingo claim"))
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
        return List.copyOf(pages);
    }

    private static Component modePage(String titleKey, String mode) {
        MutableComponent page = Component.translatable(titleKey).withStyle(ChatFormatting.BOLD);
        page.append("\n").append(Component.translatable("book.neo_bingo.choose_difficulty"));
        for (DifficultyTier tier : DifficultyTier.values()) {
            page.append("\n").append(button(
                    "book.neo_bingo.difficulty." + tier.name().toLowerCase(),
                    "/neobingo start " + mode + " difficulty " + tier.name().toLowerCase()));
        }
        return page;
    }

    private static Component rankedPage() {
        MutableComponent page = Component.translatable(
                "book.neo_bingo.mode.ranked", DEFAULT_RANKED_SECONDS / 60).withStyle(ChatFormatting.BOLD);
        page.append("\n").append(Component.translatable("book.neo_bingo.choose_difficulty"));
        for (DifficultyTier tier : DifficultyTier.values()) {
            page.append("\n").append(button(
                    "book.neo_bingo.difficulty." + tier.name().toLowerCase(),
                    "/neobingo start ranked " + DEFAULT_RANKED_SECONDS
                            + " difficulty " + tier.name().toLowerCase()));
        }
        return page;
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
}
