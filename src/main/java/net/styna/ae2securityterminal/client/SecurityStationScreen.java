package net.styna.ae2securityterminal.client;

import java.util.EnumMap;
import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;

import net.styna.ae2securityterminal.api.SecurityPermissions;
import net.styna.ae2securityterminal.menu.SecurityStationMenu;

public class SecurityStationScreen extends MEStorageScreen<SecurityStationMenu> {

    private final EnumMap<SecurityPermissions, PermissionToggleButton> toggles = new EnumMap<>(
            SecurityPermissions.class);

    public SecurityStationScreen(SecurityStationMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        for (var permission : SecurityPermissions.values()) {
            var button = new PermissionToggleButton(permission, () -> menu.toggleSetting(permission.name()));
            this.toggles.put(permission, button);
            this.widgets.add("permission_" + permission.name().toLowerCase(Locale.ROOT), button);
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        for (var entry : this.toggles.entrySet()) {
            entry.getValue().setState((menu.getPermissionMode() & (1 << entry.getKey().ordinal())) > 0);
        }
    }
}
