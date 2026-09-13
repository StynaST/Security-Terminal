package net.styna.ae2securityterminal.security;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;

/**
 * Security state mixed into AE2 grid nodes.
 */
public interface SecurityNode {
    long securityterminal$getLastSecurityKey();

    void securityterminal$setLastSecurityKey(long key);

    void securityterminal$forceOwningPlayerId(int playerId);

    @Nullable
    IGrid securityterminal$getGridOrNull();

    static SecurityNode of(IGridNode node) {
        return (SecurityNode) node;
    }
}
