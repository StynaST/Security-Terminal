package net.styna.ae2securityterminal.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.blockentity.storage.MEChestBlockEntity;

import net.styna.ae2securityterminal.api.SecurityPermissions;
import net.styna.ae2securityterminal.security.SecurityChecks;
import net.styna.ae2securityterminal.security.SecurityService;

@Mixin(targets = "appeng.blockentity.storage.MEChestBlockEntity$ChestMonitorHandler")
public abstract class ChestMonitorHandlerMixin {
    @Shadow
    @Final
    MEChestBlockEntity this$0;

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void securityterminal$checkInject(AEKey what, long amount, Actionable mode, IActionSource source,
            CallbackInfoReturnable<Long> cir) {
        if (source.player().map(player -> !this.securityterminal$securityCheck(player, SecurityPermissions.INJECT))
                .orElse(false)) {
            cir.setReturnValue(0L);
        }
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void securityterminal$checkExtract(AEKey what, long amount, Actionable mode, IActionSource source,
            CallbackInfoReturnable<Long> cir) {
        if (source.player().map(player -> !this.securityterminal$securityCheck(player, SecurityPermissions.EXTRACT))
                .orElse(false)) {
            cir.setReturnValue(0L);
        }
    }

    @Unique
    private boolean securityterminal$securityCheck(Player player, SecurityPermissions requiredPermission) {
        var grid = SecurityChecks.getGrid(this.this$0.getActionableNode());
        if (grid != null) {
            return SecurityService.get(grid).hasPermission(player, requiredPermission);
        }
        return false;
    }
}
