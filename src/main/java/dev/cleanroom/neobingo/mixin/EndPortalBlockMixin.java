package dev.cleanroom.neobingo.mixin;

import dev.cleanroom.neobingo.world.RuntimeMatchWorldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 将比赛世界中的末地传送门限制在当前比赛世界组内。 */
@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
    @Redirect(
            method = "getPortalDestination",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel neoBingo$redirectPortal(
            MinecraftServer server,
            ResourceKey<Level> target,
            ServerLevel source,
            Entity entity,
            BlockPos pos) {
        return RuntimeMatchWorldManager.portalTarget(source, target).orElseGet(() -> server.getLevel(target));
    }
}
