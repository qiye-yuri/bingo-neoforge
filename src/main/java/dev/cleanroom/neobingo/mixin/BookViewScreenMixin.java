package dev.cleanroom.neobingo.mixin;

import dev.cleanroom.neobingo.BingoSettingsBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 执行大厅设置命令时保留当前书页，不触发原版关闭书本的行为。 */
@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin {
    private static final String SETTINGS_PREFIX = "/neobingo lobby settings ";
    @Unique private boolean neoBingo$editingSettings;
    @Unique private ItemStack neoBingo$lastBook = ItemStack.EMPTY;

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void neoBingo$keepSettingsPageOpen(Style style, CallbackInfoReturnable<Boolean> callback) {
        ClickEvent click = style.getClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.RUN_COMMAND
                || !click.getValue().startsWith(SETTINGS_PREFIX)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            neoBingo$editingSettings = true;
            minecraft.getConnection().sendCommand(click.getValue().substring(1));
            callback.setReturnValue(true);
        }
    }

    /** 物品栏同步到达后原位更新书页，BookViewScreen 会保留当前页码。 */
    @Inject(method = "render", at = @At("HEAD"))
    private void neoBingo$refreshVisibleSettings(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!neoBingo$editingSettings || minecraft.player == null) return;
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack current = minecraft.player.getInventory().getItem(slot);
            if (!BingoSettingsBook.isSettingsBook(current)) continue;
            if (!ItemStack.matches(neoBingo$lastBook, current)) {
                neoBingo$lastBook = current.copy();
                ((BookViewScreen) (Object) this).setBookAccess(BookViewScreen.BookAccess.fromItem(current));
            }
            return;
        }
    }
}
