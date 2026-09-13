package net.styna.ae2securityterminal.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.RestrictedInputSlot;

import net.styna.ae2securityterminal.api.SecurityPermissions;
import net.styna.ae2securityterminal.security.SecurityChecks;

@Mixin(MEStorageMenu.class)
public abstract class MEStorageMenuMixin {
    @Shadow
    @Final
    private List<RestrictedInputSlot> viewCellSlots;

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void securityterminal$updateViewCellAccess(CallbackInfo ci) {
        var menu = (MEStorageMenu) (Object) this;
        if (menu.isClientSide() || this.viewCellSlots.isEmpty()) {
            return;
        }

        final boolean canAccessViewCells = SecurityChecks.hasAccess(menu, SecurityPermissions.BUILD);
        for (var slot : this.viewCellSlots) {
            slot.setAllowEdit(canAccessViewCells);
        }
    }
}
