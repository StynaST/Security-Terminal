package net.styna.ae2securityterminal.api;

import net.minecraft.world.entity.player.Player;

import appeng.api.networking.IGridService;

public interface ISecurityService extends IGridService {

    /**
     * @return true if a security provider is in the network ( and only 1 )
     */
    boolean isAvailable();

    /**
     * Check if a player has permissions.
     */
    boolean hasPermission(Player player, SecurityPermissions perm);

    /**
     * Check if a player has permissions.
     */
    boolean hasPermission(int playerID, SecurityPermissions perm);

    /**
     * @return PlayerID of the admin, or owner, this is the person who placed the security block.
     */
    int getOwner();
}
