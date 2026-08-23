package dev.cleanroom.neobingo.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 保存客户端 Bingo HUD 的缩放比例与停靠角落。 */
public final class BingoClientConfig {
    public enum HudCorner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        HudCorner next() {
            HudCorner[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue HUD_SCALE = BUILDER
            .comment("Bingo HUD scale percentage, from 50 to 200.")
            .defineInRange("hudScale", 100, 50, 200);
    private static final ModConfigSpec.EnumValue<HudCorner> HUD_CORNER = BUILDER
            .comment("Screen corner used by the Bingo HUD.")
            .defineEnum("hudCorner", HudCorner.TOP_RIGHT);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private BingoClientConfig() {
    }

    public static float scale() {
        return HUD_SCALE.get() / 100.0F;
    }

    public static int scalePercent() {
        return HUD_SCALE.get();
    }

    public static HudCorner corner() {
        return HUD_CORNER.get();
    }

    public static int adjustScale(int change) {
        int value = Math.clamp(HUD_SCALE.get() + change, 50, 200);
        HUD_SCALE.set(value);
        HUD_SCALE.save();
        return value;
    }

    public static HudCorner cycleCorner() {
        HudCorner corner = HUD_CORNER.get().next();
        HUD_CORNER.set(corner);
        HUD_CORNER.save();
        return corner;
    }
}
