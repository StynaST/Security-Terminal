package net.styna.ae2securityterminal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.player.Player;

import appeng.menu.AEBaseMenu;

import net.styna.ae2securityterminal.security.SecurityChecks;
import appeng.api.networking.security.IActionHost;
import net.styna.ae2securityterminal.api.SecurityPermissions;

@Mixin(AEBaseMenu.class)
public abstract class AEBaseMenuMixin {
    @Unique
    private int securityterminal$ticksSinceCheck;

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void securityterminal$checkInteractionPermission(Player player, CallbackInfoReturnable<Boolean> cir) {
        var menu = (AEBaseMenu) (Object) this;
        if (!menu.isClientSide() && menu.getTarget() instanceof IActionHost host
                && !SecurityChecks.hasPlayerPermission(host.getActionableNode(), player, SecurityPermissions.BUILD)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void securityterminal$verifyPermissions(CallbackInfo ci) {
        var menu = (AEBaseMenu) (Object) this;
        if (menu.isClientSide()) {
            return;
        }

        if (menu.getTarget() instanceof IActionHost host
                && !SecurityChecks.hasPlayerPermission(host.getActionableNode(), menu.getPlayer(),
                        SecurityPermissions.BUILD)) {
            menu.setValidMenu(false);
            return;
        }
        var permission = SecurityChecks.getVerifiedPermission(menu);
        if (permission == null) {
            return;
        }

        this.securityterminal$ticksSinceCheck++;
        if (this.securityterminal$ticksSinceCheck < 20) {
            return;
        }

        this.securityterminal$ticksSinceCheck = 0;
        menu.setValidMenu(menu.isValidMenu() && SecurityChecks.hasAccess(menu, permission));
    }
}
