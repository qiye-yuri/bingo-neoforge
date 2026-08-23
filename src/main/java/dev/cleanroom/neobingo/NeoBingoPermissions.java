package dev.cleanroom.neobingo;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/** 集中定义并查询 NeoForge Bingo 命令权限。 */
public final class NeoBingoPermissions {
    private static final int ADMIN_PERMISSION_LEVEL = 2;
    public static final PermissionNode<Boolean> PLAY = new PermissionNode<>(
            NeoBingo.MOD_ID,
            "command.play",
            PermissionTypes.BOOLEAN,
            (player, playerId, context) -> true);
    public static final PermissionNode<Boolean> ADMIN = new PermissionNode<>(
            NeoBingo.MOD_ID,
            "command.admin",
            PermissionTypes.BOOLEAN,
            (player, playerId, context) -> player != null
                    && player.createCommandSourceStack().hasPermission(ADMIN_PERMISSION_LEVEL));

    private NeoBingoPermissions() {
    }

    public static void register(PermissionGatherEvent.Nodes event) {
        event.addNodes(PLAY, ADMIN);
    }

    public static boolean canPlay(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player
                ? PermissionAPI.getPermission(player, PLAY)
                : true;
    }

    public static boolean canAdmin(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player
                ? PermissionAPI.getPermission(player, ADMIN)
                : source.hasPermission(ADMIN_PERMISSION_LEVEL);
    }
}
