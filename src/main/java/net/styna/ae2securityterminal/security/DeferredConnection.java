package net.styna.ae2securityterminal.security;

import net.minecraft.core.Direction;

import appeng.api.networking.IGridNode;

/**
 * An in-world connection to a secured node, which is only made after connecting to unsecured neighbors.
 */
public record DeferredConnection(IGridNode node, Direction direction) {
}
