package net.styna.ae2securityterminal.security;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import appeng.api.networking.GridHelper;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.PartHelper;
import appeng.hooks.ticking.TickHandler;
import net.styna.ae2securityterminal.api.SecurityPermissions;

public final class NetworkInteractionProtection {
    public static boolean canBuildAt(LevelAccessor accessor, BlockPos pos, Player player) {
        if (!(accessor instanceof Level level) || level.isClientSide()) {
            return true;
        }
        if (level.getBlockEntity(pos) instanceof IActionHost host
                && !SecurityChecks.hasPlayerPermission(host.getActionableNode(), player, SecurityPermissions.BUILD)) {
            return false;
        }
        var parts = PartHelper.getPartHost(level, pos);
        if (parts != null) {
            var cable = parts.getPart(null);
            if (cable != null && !SecurityChecks.hasPlayerPermission(cable.getGridNode(), player,
                    SecurityPermissions.BUILD)) {
                return false;
            }
            for (var side : Direction.values()) {
                var part = parts.getPart(side);
                if (part != null && !SecurityChecks.hasPlayerPermission(part.getGridNode(), player,
                        SecurityPermissions.BUILD)) {
                    return false;
                }
            }
        }
        var host = GridHelper.getNodeHost(level, pos);
        if (host != null) {
            for (var side : Direction.values()) {
                if (!SecurityChecks.hasPlayerPermission(host.getGridNode(side), player, SecurityPermissions.BUILD)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void resyncDeniedInteraction(LevelAccessor accessor, BlockPos pos, Player player) {
        if (!(accessor instanceof Level level) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        var origin = pos.immutable();
        // Run after vanilla acknowledges the client's predicted interaction.
        TickHandler.instance().addCallable(level, () -> {
            if (serverPlayer.level() != level || serverPlayer.hasDisconnected()) {
                return;
            }
            sendBlockState(level, origin, serverPlayer);
            for (var side : Direction.values()) {
                sendBlockState(level, origin.relative(side), serverPlayer);
            }
        });
    }

    private static void sendBlockState(Level level, BlockPos pos, ServerPlayer player) {
        if (!level.hasChunkAt(pos)) {
            return;
        }
        player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            var packet = blockEntity.getUpdatePacket();
            if (packet != null) {
                player.connection.send(packet);
            }
        }
    }

    public static boolean canPlaceAt(LevelAccessor level, BlockPos pos, Player player) {
        if (!canBuildAt(level, pos, player)) {
            return false;
        }
        for (var side : Direction.values()) {
            if (!canBuildAt(level, pos.relative(side), player)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!canBuildAt(event.getLevel(), event.getPos(), event.getEntity())) {
            event.setCanceled(true);
            resyncDeniedInteraction(event.getLevel(), event.getPos(), event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!canBuildAt(event.getLevel(), event.getPos(), event.getEntity())) {
            event.setCanceled(true);
            resyncDeniedInteraction(event.getLevel(), event.getPos(), event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBreak(BreakBlockEvent event) {
        if (!canBuildAt(event.getLevel(), event.getPos(), event.getPlayer())) {
            event.setCanceled(true);
            resyncDeniedInteraction(event.getLevel(), event.getPos(), event.getPlayer());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (var snapshot : multi.getReplacedBlockSnapshots()) {
                if (!canPlaceAt(event.getLevel(), snapshot.getPos(), player)) {
                    event.setCanceled(true);
                    for (var replaced : multi.getReplacedBlockSnapshots()) {
                        resyncDeniedInteraction(event.getLevel(), replaced.getPos(), player);
                    }
                    return;
                }
            }
        } else if (!canPlaceAt(event.getLevel(), event.getPos(), player)) {
            event.setCanceled(true);
            resyncDeniedInteraction(event.getLevel(), event.getPos(), player);
        }
    }
}
