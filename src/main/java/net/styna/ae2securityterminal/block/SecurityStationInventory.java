package net.styna.ae2securityterminal.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.network.chat.Component;

import appeng.api.config.Actionable;
import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;

import net.styna.ae2securityterminal.item.BiometricCardItem;

public class SecurityStationInventory implements MEStorage {

    private final List<GenericStack> storedItems = new ArrayList<>();
    private final SecurityStationBlockEntity securityTile;

    public SecurityStationInventory(SecurityStationBlockEntity ts) {
        this.securityTile = ts;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (this.hasPermission(source)) {
            if (what instanceof AEItemKey itemKey && itemKey.getItem() instanceof BiometricCardItem) {
                if (this.canAccept(itemKey)) {
                    if (mode == Actionable.SIMULATE) {
                        return amount;
                    }

                    this.storedItems.add(new GenericStack(itemKey, amount));
                    this.securityTile.inventoryChanged();
                    return amount;
                }
            }
        }
        return 0;
    }

    private boolean hasPermission(IActionSource src) {
        return src.player().map(this.securityTile::canPlayerAccess).orElse(false);
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (this.hasPermission(source)) {
            for (int i = 0; i < this.storedItems.size(); i++) {
                var target = this.storedItems.get(i);
                if (target.what().equals(what)) {
                    final long output = Math.min(amount, target.amount());

                    if (mode == Actionable.SIMULATE) {
                        return output;
                    }

                    if (output >= target.amount()) {
                        this.storedItems.remove(i);
                    } else {
                        this.storedItems.set(i, new GenericStack(target.what(), target.amount() - output));
                    }
                    this.securityTile.inventoryChanged();
                    return output;
                }
            }
        }
        return 0;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (var stack : this.storedItems) {
            out.add(stack.what(), stack.amount());
        }
    }

    @Override
    public Component getDescription() {
        return this.securityTile.getName();
    }

    private boolean canAccept(AEItemKey input) {
        final var newUser = BiometricCardItem.getProfile(input.toStack());

        var registry = IPlayerRegistry.getMapping(this.securityTile.getLevel());
        final int playerID = registry == null || newUser == null ? -1 : registry.getPlayerId(newUser.id());
        if (this.securityTile.getOwner() == playerID) {
            return false;
        }

        for (var stack : this.storedItems) {
            if (stack.amount() > 0 && stack.what() instanceof AEItemKey itemKey) {
                final var thisUser = BiometricCardItem.getProfile(itemKey.toStack());
                if (Objects.equals(thisUser, newUser)) {
                    return false;
                }
            }
        }

        return true;
    }

    public List<GenericStack> getStoredItems() {
        return this.storedItems;
    }

    void clear() {
        this.storedItems.clear();
    }

    void load(List<GenericStack> stacks) {
        this.storedItems.addAll(stacks);
    }
}
