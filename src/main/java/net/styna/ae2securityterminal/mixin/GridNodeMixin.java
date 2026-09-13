package net.styna.ae2securityterminal.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;

import appeng.api.networking.IGrid;
import appeng.me.Grid;
import appeng.me.GridNode;

import net.styna.ae2securityterminal.security.SecurityNode;
import net.styna.ae2securityterminal.security.SecurityService;

@Mixin(GridNode.class)
public abstract class GridNodeMixin implements SecurityNode {
    @Shadow
    private int owningPlayerId;

    @Shadow
    abstract Grid getMyGrid();

    @Unique
    private long securityterminal$lastSecurityKey = -1;

    @Override
    public long securityterminal$getLastSecurityKey() {
        return this.securityterminal$lastSecurityKey;
    }

    @Override
    public void securityterminal$setLastSecurityKey(long key) {
        this.securityterminal$lastSecurityKey = key;
    }

    @Override
    public void securityterminal$forceOwningPlayerId(int playerId) {
        this.owningPlayerId = playerId;
    }

    @Nullable
    @Override
    public IGrid securityterminal$getGridOrNull() {
        return getMyGrid();
    }

    @Inject(method = "loadFromNBT", at = @At("HEAD"))
    private void securityterminal$loadSecurityKey(String name, ValueInput input, CallbackInfo ci) {
        this.securityterminal$lastSecurityKey = input.read(name, CompoundTag.CODEC)
                .map(tag -> tag.getLongOr(SecurityService.KEY_TAG, -1L))
                .orElse(-1L);
    }
}
