package net.styna.ae2securityterminal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.styna.ae2securityterminal.security.StationProtection;

/**
 * Prevents active security stations from being replaced or removed by anything but their authorized players.
 */
@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void securityterminal$preventReplacement(BlockPos pos, BlockState state, int flags, int recursionLeft,
            CallbackInfoReturnable<Boolean> cir) {
        if (StationProtection.preventReplacement((Level) (Object) this, pos, state)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"), cancellable = true)
    private void securityterminal$preventDestroy(BlockPos pos, boolean dropBlock, Entity entity, int recursionLeft,
            CallbackInfoReturnable<Boolean> cir) {
        if (StationProtection.preventRemoval((Level) (Object) this, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "removeBlockEntity", at = @At("HEAD"), cancellable = true)
    private void securityterminal$preventBlockEntityRemoval(BlockPos pos, CallbackInfo ci) {
        if (StationProtection.preventRemoval((Level) (Object) this, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"), cancellable = true)
    private void securityterminal$preventBlockEntityReplacement(BlockEntity blockEntity, CallbackInfo ci) {
        if (StationProtection.preventRemoval((Level) (Object) this, blockEntity.getBlockPos())) {
            ci.cancel();
        }
    }
}
