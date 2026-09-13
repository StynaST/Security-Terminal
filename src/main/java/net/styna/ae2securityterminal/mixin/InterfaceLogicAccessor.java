package net.styna.ae2securityterminal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.api.storage.MEStorage;
import appeng.helpers.InterfaceLogic;

@Mixin(InterfaceLogic.class)
public interface InterfaceLogicAccessor {
    @Invoker("getLocalInventory")
    MEStorage securityterminal$getLocalInventory();
}
