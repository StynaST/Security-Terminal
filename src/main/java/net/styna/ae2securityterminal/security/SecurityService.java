package net.styna.ae2securityterminal.security;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.storage.IStorageService;

import net.styna.ae2securityterminal.api.ISecurityProvider;
import net.styna.ae2securityterminal.api.ISecurityService;
import net.styna.ae2securityterminal.api.SecurityPermissions;

public class SecurityService implements ISecurityService, IGridServiceProvider {

    public static final String KEY_TAG = "securityterminal:k";

    private final IGrid myGrid;
    private final List<ISecurityProvider> securityProvider = new ArrayList<>();
    private final HashMap<Integer, EnumSet<SecurityPermissions>> playerPerms = new HashMap<>();
    private long securityKey = -1;

    public SecurityService(IGrid grid, IStorageService storage) {
        this.myGrid = grid;
        ((NetworkStorageSecurity) storage.getInventory()).securityterminal$setSecurity(this);
    }

    @Nullable
    public static SecurityService get(@Nullable IGrid grid) {
        return grid == null ? null : (SecurityService) grid.getService(ISecurityService.class);
    }

    public void updatePermissions() {
        this.playerPerms.clear();
        if (this.securityProvider.isEmpty()) {
            return;
        }

        this.securityProvider.get(0).readPermissions(this.playerPerms);
    }

    public long getSecurityKey() {
        return this.securityKey;
    }

    @Override
    public void removeNode(IGridNode gridNode) {
        if (gridNode.getOwner() instanceof ISecurityProvider provider) {
            this.securityProvider.remove(provider);
            this.updateSecurityKey();
        }
    }

    private void updateSecurityKey() {
        final long lastCode = this.securityKey;

        int newOwner = -1;
        if (this.securityProvider.size() == 1) {
            this.securityKey = this.securityProvider.get(0).getSecurityKey();
            newOwner = this.securityProvider.get(0).getOwner();
        } else {
            this.securityKey = -1;
        }

        if (lastCode != this.securityKey) {
            this.updatePermissions();
            for (var n : this.myGrid.getNodes()) {
                var securityNode = SecurityNode.of(n);
                securityNode.securityterminal$setLastSecurityKey(this.securityKey);
                // Providers keep their placer; otherwise moving a station between grids would erase its owner.
                if (!(n.getOwner() instanceof ISecurityProvider) && n.getOwningPlayerId() != newOwner) {
                    securityNode.securityterminal$forceOwningPlayerId(newOwner);
                }
            }
        }
    }

    @Override
    public void addNode(IGridNode gridNode, @Nullable CompoundTag savedData) {
        if (gridNode.getOwner() instanceof ISecurityProvider provider) {
            this.securityProvider.add(provider);
            this.updateSecurityKey();
        } else {
            SecurityNode.of(gridNode).securityterminal$setLastSecurityKey(this.securityKey);
        }
    }

    @Override
    public void saveNodeData(IGridNode gridNode, CompoundTag savedData) {
        savedData.putLong(KEY_TAG, SecurityNode.of(gridNode).securityterminal$getLastSecurityKey());
    }

    @Override
    public boolean isAvailable() {
        return this.securityProvider.size() == 1 && this.securityProvider.get(0).isSecurityEnabled();
    }

    @Override
    public boolean hasPermission(Player player, SecurityPermissions perm) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(perm);

        if (!this.isAvailable()) {
            return true;
        }

        var registry = IPlayerRegistry.getMapping(player.level());
        if (registry == null) {
            return false;
        }
        final int playerID = registry.getPlayerId(player.getGameProfile());

        EnumSet<SecurityPermissions> perms = this.playerPerms.get(playerID);
        if (perms == null) {
            perms = this.playerPerms.get(-1);
        }

        return perms != null && perms.contains(perm);
    }

    @Override
    public boolean hasPermission(int playerID, SecurityPermissions perm) {
        Objects.requireNonNull(perm);
        if (!this.isAvailable()) {
            return true;
        }
        var perms = this.playerPerms.get(playerID);
        if (perms == null) {
            perms = this.playerPerms.get(-1);
        }
        return perms != null && perms.contains(perm);
    }

    @Override
    public int getOwner() {
        if (this.isAvailable()) {
            return this.securityProvider.get(0).getOwner();
        }
        return -1;
    }

    public IGrid getGrid() {
        return this.myGrid;
    }
}
