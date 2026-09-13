package net.styna.ae2securityterminal.menu;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.ClientActionKey;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.AppEngSlot;

import net.styna.ae2securityterminal.SecurityTerminal;
import net.styna.ae2securityterminal.api.SecurityPermissions;
import net.styna.ae2securityterminal.block.SecurityStationBlockEntity;
import net.styna.ae2securityterminal.item.BiometricCardItem;

public class SecurityStationMenu extends MEStorageMenu {

    public static final MenuType<SecurityStationMenu> TYPE = MenuTypeBuilder
            .<SecurityStationMenu, SecurityStationBlockEntity>create(SecurityStationMenu::new,
                    SecurityStationBlockEntity.class)
            .buildUnregistered(SecurityTerminal.id("security_station"));

    public static final SlotSemantic BIOMETRIC_CARD = SlotSemantics.register("SECURITYTERMINAL_BIOMETRIC_CARD",
            false);

    private static final ClientActionKey<String> TOGGLE_SETTING = new ClientActionKey<>("toggleSetting");

    private final AppEngSlot configSlot;

    private final SecurityStationBlockEntity securityBox;
    @GuiSync(0)
    public int permissionMode = 0;

    public SecurityStationMenu(MenuType<?> menuType, int id, Inventory ip, SecurityStationBlockEntity host) {
        super(menuType, id, ip, host, false);

        this.securityBox = host;

        this.configSlot = new AppEngSlot(this.securityBox.getConfigSlot(), 0);
        this.addSlot(this.configSlot, BIOMETRIC_CARD);

        this.createPlayerInventorySlots(ip);

        registerClientAction(TOGGLE_SETTING, ByteBufCodecs.STRING_UTF8, this::toggleSetting);
    }

    public void toggleSetting(String value) {
        if (isClientSide()) {
            sendClientAction(TOGGLE_SETTING, value);
            return;
        }

        if (!this.securityBox.canPlayerAccess(getPlayer())) {
            return;
        }
        try {
            final SecurityPermissions permission = SecurityPermissions.valueOf(value);

            final var a = this.configSlot.getItem();
            if (!a.isEmpty() && a.getItem() instanceof BiometricCardItem) {
                if (BiometricCardItem.hasPermission(a, permission)) {
                    BiometricCardItem.removePermission(a, permission);
                } else {
                    BiometricCardItem.addPermission(a, permission);
                }
                this.configSlot.setChanged();
            }
        } catch (IllegalArgumentException ex) {
            // :(
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && (isClientSide() || this.securityBox.canPlayerAccess(player));
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            if (!this.securityBox.canPlayerAccess(getPlayer())) {
                this.setValidMenu(false);
                return;
            }

            this.permissionMode = 0;

            final var a = this.configSlot.getItem();
            if (!a.isEmpty() && a.getItem() instanceof BiometricCardItem) {
                for (var sp : BiometricCardItem.getPermissions(a)) {
                    this.permissionMode |= 1 << sp.ordinal();
                }
            }
        }

        super.broadcastChanges();
    }

    public int getPermissionMode() {
        return this.permissionMode;
    }
}
