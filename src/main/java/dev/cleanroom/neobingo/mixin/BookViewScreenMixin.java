package dev.cleanroom.neobingo.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 执行大厅设置命令时保留当前书页，不触发原版关闭书本的行为。 */
@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin {
    private static final String SETTINGS_PREFIX = "/neobingo lobby settings ";

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void neoBingo$keepSettingsPageOpen(Style style, CallbackInfoReturnable<Boolean> callback) {
        ClickEvent click = style.getClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.RUN_COMMAND
                || !click.getValue().startsWith(SETTINGS_PREFIX)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand(click.getValue().substring(1));
            callback.setReturnValue(true);
        }
    }
}
