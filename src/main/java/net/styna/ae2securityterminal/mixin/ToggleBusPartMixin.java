package net.styna.ae2securityterminal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.parts.misc.ToggleBusPart;

import net.styna.ae2securityterminal.api.SecurityConnectionException;

@Mixin(ToggleBusPart.class)
public abstract class ToggleBusPartMixin {
    @WrapOperation(method = "updateInternalState", at = @At(value = "INVOKE", target = "Lappeng/api/networking/GridHelper;createConnection(Lappeng/api/networking/IGridNode;Lappeng/api/networking/IGridNode;)Lappeng/api/networking/IGridConnection;"))
    private IGridConnection securityterminal$skipRefusedConnection(IGridNode a, IGridNode b,
            Operation<IGridConnection> original) {
        try {
            return original.call(a, b);
        } catch (SecurityConnectionException e) {
            return null;
        }
    }
}
