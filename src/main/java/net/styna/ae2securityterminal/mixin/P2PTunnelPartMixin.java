package net.styna.ae2securityterminal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import appeng.parts.p2p.P2PTunnelPart;

import net.styna.ae2securityterminal.api.SecurityPermissions;
import net.styna.ae2securityterminal.security.SecurityChecks;
import net.styna.ae2securityterminal.security.SecurityService;

@Mixin(P2PTunnelPart.class)
public abstract class P2PTunnelPartMixin {
    @Inject(method = "onUseItemOn", at = @At("HEAD"), cancellable = true)
    private void securityterminal$checkBuildPermission(ItemStack heldItem, Player player, InteractionHand hand,
            Vec3 pos, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().isClientSide()) {
            return;
        }

        var grid = SecurityChecks.getGrid(((P2PTunnelPart<?>) (Object) this).getGridNode());
        if (grid == null || !SecurityService.get(grid).hasPermission(player, SecurityPermissions.BUILD)) {
            cir.setReturnValue(false);
        }
    }
}
