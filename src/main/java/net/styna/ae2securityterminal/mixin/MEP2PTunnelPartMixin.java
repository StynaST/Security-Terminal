package net.styna.ae2securityterminal.mixin;

import java.util.Map;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.parts.p2p.MEP2PTunnelPart;

import net.styna.ae2securityterminal.api.SecurityConnectionException;

@Mixin(MEP2PTunnelPart.class)
public abstract class MEP2PTunnelPartMixin {
    @WrapOperation(method = "updateConnections", at = @At(value = "INVOKE", target = "Lappeng/api/networking/GridHelper;createConnection(Lappeng/api/networking/IGridNode;Lappeng/api/networking/IGridNode;)Lappeng/api/networking/IGridConnection;"))
    private IGridConnection securityterminal$skipRefusedConnection(IGridNode a, IGridNode b,
            Operation<IGridConnection> original) {
        try {
            return original.call(a, b);
        } catch (SecurityConnectionException e) {
            return null;
        }
    }

    @WrapOperation(method = "updateConnections", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object securityterminal$skipMissingConnection(Map<Object, Object> map, Object key, Object value,
            Operation<Object> original) {
        return value == null ? null : original.call(map, key, value);
    }
}
