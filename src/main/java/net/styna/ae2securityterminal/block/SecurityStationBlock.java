package net.styna.ae2securityterminal.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;

import net.styna.ae2securityterminal.menu.SecurityStationMenu;
import net.styna.ae2securityterminal.security.StationProtection;

public class SecurityStationBlock extends AEBaseEntityBlock<SecurityStationBlockEntity> {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public SecurityStationBlock(Properties props) {
        super(metalProps(props));
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, SecurityStationBlockEntity be) {
        return currentState.setValue(POWERED, be.isActive());
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack tool,
            boolean willHarvest, FluidState fluid) {
        final var be = this.getBlockEntity(level, pos);
        if (!level.isClientSide() && be != null && !be.canPlayerAccess(player)) {
            return false;
        }
        return StationProtection.removeByPlayer(level, pos,
                () -> super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        final var be = this.getBlockEntity(level, pos);
        if (!level.isClientSide() && be != null && !be.canPlayerAccess(player)) {
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        final var be = this.getBlockEntity(level, pos);
        if (be != null) {
            if (!level.isClientSide() && be.canPlayerAccess(player)) {
                MenuOpener.open(SecurityStationMenu.TYPE, player, MenuLocators.forBlockEntity(be));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        final var be = this.getBlockEntity(level, pos);
        return (be == null || !be.isSecurityEnabled()) && super.canEntityDestroy(state, level, pos, entity);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos,
            @Nullable Explosion explosion) {
        return level instanceof Level l && StationProtection.isProtected(l, pos) ? Float.MAX_VALUE
                : super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public void onBlockExploded(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion) {
        if (!StationProtection.isProtected(level, pos)) {
            super.onBlockExploded(state, level, pos, explosion);
        }
    }
}
