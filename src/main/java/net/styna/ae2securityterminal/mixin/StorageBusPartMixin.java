package net.styna.ae2securityterminal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import appeng.api.networking.security.IActionSource;
import appeng.parts.PartAdjacentApi;
import appeng.parts.storagebus.StorageBusPart;

import net.styna.ae2securityterminal.security.SecurityChecks;

@Mixin(StorageBusPart.class)
public abstract class StorageBusPartMixin {
    @Shadow
    @Final
    protected IActionSource source;

    /**
     * Storage buses only see the local inventory of interfaces on networks they are not allowed to build on.
     */
    @WrapOperation(method = "updateTarget", at = @At(value = "INVOKE", target = "Lappeng/parts/PartAdjacentApi;find()Ljava/lang/Object;"))
    private Object securityterminal$restrictInterfaceAccess(PartAdjacentApi<?> api, Operation<Object> original) {
        var result = original.call(api);
        if (result != null) {
            var part = (StorageBusPart) (Object) this;
            var blockEntity = part.getBlockEntity();
            var side = part.getSide();
            var host = SecurityChecks.findInterface(blockEntity.getLevel(), blockEntity.getBlockPos().relative(side),
                    side.getOpposite());
            if (host != null && !SecurityChecks.canAccess(SecurityChecks.getInterfaceGrid(host), this.source)) {
                return ((InterfaceLogicAccessor) host.getInterfaceLogic()).securityterminal$getLocalInventory();
            }
        }
        return result;
    }
}
