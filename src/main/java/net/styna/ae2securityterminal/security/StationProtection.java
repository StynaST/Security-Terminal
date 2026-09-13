package net.styna.ae2securityterminal.security;

import java.util.function.BooleanSupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.util.TriState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import net.styna.ae2securityterminal.block.SecurityStationBlock;
import net.styna.ae2securityterminal.block.SecurityStationBlockEntity;

public final class StationProtection {

    private static final ThreadLocal<Removal> REMOVAL = new ThreadLocal<>();

    public static boolean isProtected(LevelAccessor accessor, BlockPos pos) {
        // Level.getBlockEntity can create a block entity and re-enter the setBlockEntity guard during placement.
        return accessor instanceof Level level && !level.isClientSide()
                && level.getBlockState(pos).getBlock() instanceof SecurityStationBlock
                && level.getChunkAt(pos).getBlockEntity(pos,
                        LevelChunk.EntityCreationType.CHECK) instanceof SecurityStationBlockEntity station
                && station.isSecurityEnabled();
    }

    public static boolean preventRemoval(Level level, BlockPos pos) {
        final Removal removal = REMOVAL.get();
        return (removal == null || removal.level != level || !removal.pos.equals(pos)) && isProtected(level, pos);
    }

    public static boolean preventReplacement(Level level, BlockPos pos, BlockState state) {
        return !level.isClientSide() && level.getBlockState(pos).getBlock() != state.getBlock()
                && preventRemoval(level, pos);
    }

    public static boolean removeByPlayer(Level level, BlockPos pos, BooleanSupplier action) {
        final Removal previous = REMOVAL.get();
        REMOVAL.set(new Removal(level, pos.immutable()));
        try {
            return action.getAsBoolean();
        } finally {
            if (previous == null) {
                REMOVAL.remove();
            } else {
                REMOVAL.set(previous);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof SecurityStationBlockEntity station)) {
            return;
        }
        if (!station.canPlayerAccess(event.getEntity())) {
            event.setCanceled(true);
        } else if (isProtected(event.getLevel(), event.getPos())) {
            // Item use runs before block activation, allowing packing tools to bypass block checks.
            event.setUseItem(TriState.FALSE);
            event.setUseBlock(TriState.TRUE);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (isProtected(event.getLevel(), event.getPos())
                && !((SecurityStationBlockEntity) event.getLevel().getBlockEntity(event.getPos()))
                        .canPlayerAccess(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBreak(BreakBlockEvent event) {
        if (isProtected(event.getLevel(), event.getPos())
                && !((SecurityStationBlockEntity) event.getLevel().getBlockEntity(event.getPos()))
                        .canPlayerAccess(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> isProtected(event.getLevel(), pos));
    }

    private record Removal(Level level, BlockPos pos) {
    }
}
