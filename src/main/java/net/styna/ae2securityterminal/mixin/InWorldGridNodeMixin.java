package net.styna.ae2securityterminal.mixin;

import java.util.ArrayList;
import java.util.List;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.Direction;

import appeng.api.networking.IGridNode;
import appeng.me.GridConnection;
import appeng.me.InWorldGridNode;

import net.styna.ae2securityterminal.api.SecurityConnectionException;
import net.styna.ae2securityterminal.security.DeferredConnection;
import net.styna.ae2securityterminal.security.SecurityChecks;
import net.styna.ae2securityterminal.security.SecurityNode;

@Mixin(InWorldGridNode.class)
public abstract class InWorldGridNodeMixin {
    @Unique
    private List<DeferredConnection> securityterminal$deferred;

    @WrapOperation(method = "findInWorldConnections", at = @At(value = "INVOKE", target = "Lappeng/me/GridConnection;create(Lappeng/api/networking/IGridNode;Lappeng/api/networking/IGridNode;Lnet/minecraft/core/Direction;)Lappeng/me/GridConnection;"))
    private GridConnection securityterminal$deferSecureConnections(IGridNode a, IGridNode b, Direction direction,
            Operation<GridConnection> original) {
        if (this.securityterminal$deferred != null
                && SecurityNode.of(b).securityterminal$getLastSecurityKey() != -1) {
            this.securityterminal$deferred.add(new DeferredConnection(b, direction));
            return null;
        }
        return original.call(a, b, direction);
    }

    @WrapMethod(method = "findInWorldConnections")
    private void securityterminal$connectSecureNodesLast(Operation<Void> original) {
        var self = (InWorldGridNode) (Object) this;
        var previous = this.securityterminal$deferred;
        this.securityterminal$deferred = new ArrayList<>();
        try {
            original.call();

            var deferred = this.securityterminal$deferred;
            this.securityterminal$deferred = null;
            for (var connection : deferred) {
                // Exposure was checked before deferring; isExposedOnSide would reject new nodes without a grid yet.
                boolean connected = false;
                for (var existing : self.getConnections()) {
                    if (existing.getOtherSide(self) == connection.node()) {
                        connected = true;
                        break;
                    }
                }
                if (!connected) {
                    GridConnection.create(self, connection.node(), connection.direction());
                }
            }
        } catch (SecurityConnectionException e) {
            SecurityChecks.scheduleSecurityBreak(self);
        } finally {
            this.securityterminal$deferred = previous;
        }
    }
}
