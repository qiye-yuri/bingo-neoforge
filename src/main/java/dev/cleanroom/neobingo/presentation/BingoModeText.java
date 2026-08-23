package dev.cleanroom.neobingo.presentation;

import dev.cleanroom.neobingo.domain.GameMode;
import java.util.Objects;
import net.minecraft.network.chat.Component;

/** 将领域或协议中的模式标识转换为可本地化的展示文本。 */
public final class BingoModeText {
    private BingoModeText() {
    }

    public static Component displayName(GameMode mode) {
        return displayName(Objects.requireNonNull(mode, "mode").name());
    }

    public static Component displayName(String mode) {
        Objects.requireNonNull(mode, "mode");
        return switch (mode) {
            case "STANDARD" -> Component.translatable("commands.neo_bingo.mode.standard");
            case "LOCKOUT" -> Component.translatable("commands.neo_bingo.mode.lockout");
            case "HIDDEN" -> Component.translatable("commands.neo_bingo.mode.hidden");
            case "RANKED" -> Component.translatable("commands.neo_bingo.mode.ranked");
            default -> Component.literal(mode);
        };
    }
}
