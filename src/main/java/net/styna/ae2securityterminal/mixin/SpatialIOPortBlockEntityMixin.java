package net.styna.ae2securityterminal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import appeng.api.networking.IGridNode;
import appeng.blockentity.spatial.SpatialIOPortBlockEntity;

import net.styna.ae2securityterminal.security.SecurityChecks;
import net.styna.ae2securityterminal.security.SecurityService;

@Mixin(SpatialIOPortBlockEntity.class)
public abstract class SpatialIOPortBlockEntityMixin {
    @WrapOperation(method = "lambda$transition$0", at = @At(value = "INVOKE", target = "Lappeng/api/networking/IGridNode;getOwningPlayerId()I"))
    private int securityterminal$useSecurityOwner(IGridNode node, Operation<Integer> original) {
        var security = SecurityService.get(SecurityChecks.getGrid(node));
        if (security != null && security.isAvailable()) {
            return security.getOwner();
        }
        return -1;
    }
}
