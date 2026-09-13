package net.styna.ae2securityterminal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

import appeng.api.networking.security.IActionSource;

import net.styna.ae2securityterminal.security.SecurityChecks;

/**
 * Pattern providers only push into interfaces of other networks they are allowed to build on.
 */
@Mixin(targets = "appeng.helpers.patternprovider.PatternProviderTargetCache")
public abstract class PatternProviderTargetMixin {
    @Shadow
    @Final
    private IActionSource src;

    @WrapOperation(method = "find", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/capabilities/BlockCapabilityCache;getCapability()Ljava/lang/Object;"))
    private Object securityterminal$restrictInterfaceAccess(BlockCapabilityCache<?, ?> cache,
            Operation<Object> original) {
        var storage = original.call(cache);
        if (storage != null && cache.context() instanceof Direction side) {
            var host = SecurityChecks.findInterface(cache.level(), cache.pos(), side);
            if (host != null) {
                var targetGrid = SecurityChecks.getInterfaceGrid(host);
                var ownGrid = this.src.machine().map(m -> SecurityChecks.getGrid(m.getActionableNode())).orElse(null);
                if (targetGrid != ownGrid && !SecurityChecks.canAccess(targetGrid, this.src)) {
                    return null;
                }
            }
        }
        return storage;
    }
}
