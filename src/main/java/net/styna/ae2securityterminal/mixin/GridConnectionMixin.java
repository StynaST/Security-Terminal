package net.styna.ae2securityterminal.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Direction;

import appeng.api.networking.IGridNode;
import appeng.me.GridConnection;

import net.styna.ae2securityterminal.security.SecurityChecks;

@Mixin(GridConnection.class)
public abstract class GridConnectionMixin {
    @Inject(method = "create", at = @At("HEAD"))
    private static void securityterminal$checkSecurity(IGridNode aNode, IGridNode bNode, @Nullable Direction fromAtoB,
            CallbackInfoReturnable<GridConnection> cir) {
        SecurityChecks.checkConnection(aNode, bNode);
    }
}
