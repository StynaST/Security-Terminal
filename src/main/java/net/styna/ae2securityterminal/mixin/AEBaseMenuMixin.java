package net.styna.ae2securityterminal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.menu.AEBaseMenu;

import net.styna.ae2securityterminal.security.SecurityChecks;

@Mixin(AEBaseMenu.class)
public abstract class AEBaseMenuMixin {
    @Unique
    private int securityterminal$ticksSinceCheck;

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void securityterminal$verifyPermissions(CallbackInfo ci) {
        var menu = (AEBaseMenu) (Object) this;
        if (menu.isClientSide()) {
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
