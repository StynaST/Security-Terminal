package net.styna.ae2securityterminal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;

import appeng.api.util.AEColor;
import appeng.parts.networking.CablePart;

import net.styna.ae2securityterminal.api.SecurityPermissions;
import net.styna.ae2securityterminal.security.SecurityChecks;

@Mixin(CablePart.class)
public abstract class CablePartMixin {
    @Inject(method = "changeColor", at = @At("HEAD"), cancellable = true)
    private void securityterminal$checkBuildPermission(AEColor newColor, Player who,
            CallbackInfoReturnable<Boolean> cir) {
        var self = (CablePart) (Object) this;
        if (who != null && self.getCableColor() != newColor
                && !SecurityChecks.hasPlayerPermission(self.getGridNode(), who, SecurityPermissions.BUILD)) {
            cir.setReturnValue(false);
        }
    }
}
