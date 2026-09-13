package net.styna.ae2securityterminal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.locator.MenuHostLocator;

import net.styna.ae2securityterminal.security.SecurityChecks;

@Mixin(MenuTypeBuilder.class)
public abstract class MenuTypeBuilderMixin {
    @Shadow
    private MenuType<?> menuType;

    @WrapOperation(method = "open", at = @At(value = "INVOKE", target = "Lappeng/menu/locator/MenuHostLocator;locate(Lnet/minecraft/world/entity/player/Player;Ljava/lang/Class;)Ljava/lang/Object;"))
    private Object securityterminal$checkPermissions(MenuHostLocator locator, Player player, Class<?> hostInterface,
            Operation<Object> original) {
        var host = original.call(locator, player, hostInterface);
        if (host != null && !SecurityChecks.canOpen(this.menuType, host, player)) {
            return null;
        }
        return host;
    }
}
