package net.styna.ae2securityterminal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.storage.NetworkStorage;

import net.styna.ae2securityterminal.api.SecurityPermissions;
import net.styna.ae2securityterminal.security.NetworkStorageSecurity;
import net.styna.ae2securityterminal.security.SecurityChecks;
import net.styna.ae2securityterminal.security.SecurityService;

@Mixin(NetworkStorage.class)
public abstract class NetworkStorageMixin implements NetworkStorageSecurity {
    @Unique
    private SecurityService securityterminal$security;

    @Override
    public void securityterminal$setSecurity(SecurityService security) {
        this.securityterminal$security = security;
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void securityterminal$checkInject(AEKey what, long amount, Actionable type, IActionSource src,
            CallbackInfoReturnable<Long> cir) {
        if (this.securityterminal$security != null
                && SecurityChecks.testPermission(this.securityterminal$security, src, SecurityPermissions.INJECT)) {
            cir.setReturnValue(0L);
        }
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void securityterminal$checkExtract(AEKey what, long amount, Actionable mode, IActionSource source,
            CallbackInfoReturnable<Long> cir) {
        if (this.securityterminal$security != null
                && SecurityChecks.testPermission(this.securityterminal$security, source,
                        SecurityPermissions.EXTRACT)) {
            cir.setReturnValue(0L);
        }
    }
}
