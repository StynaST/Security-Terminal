package net.styna.ae2securityterminal.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.parts.CableBusContainer;

import net.styna.ae2securityterminal.api.SecurityConnectionException;
import net.styna.ae2securityterminal.security.SecurityChecks;
import net.styna.ae2securityterminal.security.NetworkInteractionProtection;

@Mixin(CableBusContainer.class)
public abstract class CableBusContainerMixin {
    @Unique
    private boolean securityterminal$connectionRefused;

    @WrapOperation(method = "addPart", at = @At(value = "INVOKE", target = "Lappeng/api/networking/GridHelper;createConnection(Lappeng/api/networking/IGridNode;Lappeng/api/networking/IGridNode;)Lappeng/api/networking/IGridConnection;"), require = 2)
    private IGridConnection securityterminal$refusePart(IGridNode a, IGridNode b,
            Operation<IGridConnection> original) {
        try {
            return original.call(a, b);
        } catch (SecurityConnectionException e) {
            this.securityterminal$connectionRefused = true;
            return null;
        }
    }

    @WrapMethod(method = "addPart")
    private IPart securityterminal$removeRefusedPart(IPartItem<?> partItem, Direction side, @Nullable Player player,
            Operation<IPart> original) {
        var blockEntity = ((CableBusContainer) (Object) this).getBlockEntity();
        if (player != null && blockEntity.getLevel() != null
                && !NetworkInteractionProtection.canPlaceAt(blockEntity.getLevel(), blockEntity.getBlockPos(), player)) {
            return null;
        }
        var previous = this.securityterminal$connectionRefused;
        this.securityterminal$connectionRefused = false;
        try {
            var part = original.call(partItem, side, player);
            if (this.securityterminal$connectionRefused && part != null) {
                ((CableBusContainer) (Object) this).removePart(part);
                return null;
            }
            return part;
        } finally {
            this.securityterminal$connectionRefused = previous;
        }
    }

    @WrapOperation(method = "addToWorld", at = @At(value = "INVOKE", target = "Lappeng/api/networking/GridHelper;createConnection(Lappeng/api/networking/IGridNode;Lappeng/api/networking/IGridNode;)Lappeng/api/networking/IGridConnection;"))
    private IGridConnection securityterminal$breakRefusedPart(IGridNode a, IGridNode b,
            Operation<IGridConnection> original) {
        try {
            return original.call(a, b);
        } catch (SecurityConnectionException e) {
            SecurityChecks.scheduleSecurityBreak(b);
            return null;
        }
    }
}
