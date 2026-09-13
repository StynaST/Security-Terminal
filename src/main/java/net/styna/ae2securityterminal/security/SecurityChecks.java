package net.styna.ae2securityterminal.security;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartHost;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.InterfaceLogicHost;
import appeng.hooks.ticking.TickHandler;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.DriveMenu;
import appeng.menu.implementations.EnergyLevelEmitterMenu;
import appeng.menu.implementations.FormationPlaneMenu;
import appeng.menu.implementations.IOBusMenu;
import appeng.menu.implementations.IOPortMenu;
import appeng.menu.implementations.InterfaceMenu;
import appeng.menu.implementations.MEChestMenu;
import appeng.menu.implementations.MolecularAssemblerMenu;
import appeng.menu.implementations.PatternAccessTermMenu;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.implementations.PriorityMenu;
import appeng.menu.implementations.QNBMenu;
import appeng.menu.implementations.SetStockAmountMenu;
import appeng.menu.implementations.SpatialIOPortMenu;
import appeng.menu.implementations.StorageBusMenu;
import appeng.menu.implementations.StorageLevelEmitterMenu;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.implementations.WirelessAccessPointMenu;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.menu.me.crafting.CraftingStatusMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.AEBasePart;
import appeng.util.Platform;

import net.styna.ae2securityterminal.api.SecurityConnectionException;
import net.styna.ae2securityterminal.api.SecurityPermissions;
import net.styna.ae2securityterminal.block.SecurityStationBlockEntity;
import net.styna.ae2securityterminal.menu.SecurityStationMenu;

/**
 * The security checks that AE2 UEL performed throughout its code base.
 */
public final class SecurityChecks {

    private static Map<MenuType<?>, SecurityPermissions> openPermissions;

    private SecurityChecks() {
    }

    /**
     * Mirrors the required permissions of the GuiBridge entries.
     */
    private static Map<MenuType<?>, SecurityPermissions> getOpenPermissions() {
        if (openPermissions == null) {
            var map = new IdentityHashMap<MenuType<?>, SecurityPermissions>();
            map.put(QNBMenu.TYPE, SecurityPermissions.BUILD);
            map.put(MEChestMenu.TYPE, SecurityPermissions.BUILD);
            map.put(WirelessAccessPointMenu.TYPE, SecurityPermissions.BUILD);
            map.put(CraftingCPUMenu.TYPE, SecurityPermissions.CRAFT);
            map.put(DriveMenu.TYPE, SecurityPermissions.BUILD);
            map.put(InterfaceMenu.TYPE, SecurityPermissions.BUILD);
            map.put(PatternProviderMenu.TYPE, SecurityPermissions.BUILD);
            map.put(IOBusMenu.EXPORT_TYPE, SecurityPermissions.BUILD);
            map.put(IOBusMenu.IMPORT_TYPE, SecurityPermissions.BUILD);
            map.put(IOPortMenu.TYPE, SecurityPermissions.BUILD);
            map.put(StorageBusMenu.TYPE, SecurityPermissions.BUILD);
            map.put(FormationPlaneMenu.TYPE, SecurityPermissions.BUILD);
            map.put(PriorityMenu.TYPE, SecurityPermissions.BUILD);
            map.put(SecurityStationMenu.TYPE, SecurityPermissions.SECURITY);
            map.put(CraftingTermMenu.TYPE, SecurityPermissions.CRAFT);
            map.put(PatternEncodingTermMenu.TYPE, SecurityPermissions.CRAFT);
            map.put(StorageLevelEmitterMenu.TYPE, SecurityPermissions.BUILD);
            map.put(EnergyLevelEmitterMenu.TYPE, SecurityPermissions.BUILD);
            map.put(SpatialIOPortMenu.TYPE, SecurityPermissions.BUILD);
            map.put(CraftAmountMenu.TYPE, SecurityPermissions.CRAFT);
            map.put(CraftConfirmMenu.TYPE, SecurityPermissions.CRAFT);
            map.put(PatternAccessTermMenu.TYPE, SecurityPermissions.BUILD);
            map.put(CraftingStatusMenu.TYPE, SecurityPermissions.CRAFT);
            map.put(SetStockAmountMenu.TYPE, SecurityPermissions.BUILD);
            openPermissions = map;
        }
        return openPermissions;
    }

    /**
     * The permission a menu re-verifies while it is open.
     */
    @Nullable
    public static SecurityPermissions getVerifiedPermission(AEBaseMenu menu) {
        if (menu instanceof SecurityStationMenu) {
            return SecurityPermissions.SECURITY;
        }
        if (menu instanceof CraftConfirmMenu || menu instanceof CraftAmountMenu) {
            return SecurityPermissions.CRAFT;
        }
        if (menu instanceof UpgradeableMenu<?>
                || menu instanceof PriorityMenu
                || menu instanceof InterfaceMenu
                || menu instanceof PatternProviderMenu
                || menu instanceof SpatialIOPortMenu
                || menu instanceof MolecularAssemblerMenu
                || menu instanceof IOPortMenu) {
            return SecurityPermissions.BUILD;
        }
        return null;
    }

    @Nullable
    public static IGrid getGrid(@Nullable IGridNode node) {
        return node == null ? null : SecurityNode.of(node).securityterminal$getGridOrNull();
    }

    @Nullable
    private static IActionHost getActionHost(AEBaseMenu menu) {
        return menu.getTarget() instanceof IActionHost host ? host : null;
    }

    public static boolean hasAccess(AEBaseMenu menu, SecurityPermissions perm) {
        if (menu.getTarget() instanceof SecurityStationBlockEntity station) {
            return station.canPlayerAccess(menu.getPlayer());
        }
        final IActionHost host = getActionHost(menu);

        if (host != null) {
            var grid = getGrid(host.getActionableNode());
            if (grid != null) {
                return SecurityService.get(grid).hasPermission(menu.getPlayer(), perm);
            }
        }

        return false;
    }

    public static boolean canOpen(MenuType<?> menuType, Object host, Player player) {
        if (host instanceof IActionHost actionHost
                && !hasPlayerPermission(actionHost.getActionableNode(), player, SecurityPermissions.BUILD)) {
            return false;
        }
        if (host instanceof SecurityStationBlockEntity station) {
            return station.canPlayerAccess(player);
        }
        var requiredPermission = getOpenPermissions().get(menuType);
        if (host instanceof IActionHost actionHost) {
            var grid = getGrid(actionHost.getActionableNode());
            if (grid != null) {
                var security = SecurityService.get(grid);
                if (requiredPermission != null) {
                    return security.hasPermission(player, requiredPermission);
                }

                return hasAnyPermission(security, player);
            }

            return requiredPermission == null;
        }
        return true;
    }

    /**
     * Terminals declare no required permission, so without this any player could open them on a secured network and
     * merely fail on every interaction inside.
     */
    private static boolean hasAnyPermission(SecurityService security, Player player) {
        if (!security.isAvailable()) {
            return true;
        }

        for (var permission : SecurityPermissions.values()) {
            if (security.hasPermission(player, permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Permission check of a network grid when a player uses a part on it. Without a grid, the check passes.
     */
    public static boolean hasPlayerPermission(@Nullable IGridNode node, Player player, SecurityPermissions perm) {
        var grid = getGrid(node);
        return grid == null || SecurityService.get(grid).hasPermission(player, perm);
    }

    public static boolean testPermission(SecurityService security, IActionSource src, SecurityPermissions permission) {
        if (src.player().isPresent()) {
            return !security.hasPermission(src.player().get(), permission);
        } else if (src.machine().isPresent()) {
            if (security.isAvailable()) {
                final IGridNode n = src.machine().get().getActionableNode();
                if (n == null) {
                    return true;
                }

                final IGrid gn = getGrid(n);
                if (gn != security.getGrid()) {
                    final int playerID = gn == null ? -1 : SecurityService.get(gn).getOwner();
                    return !security.hasPermission(playerID, permission);
                }
            }
        }

        return false;
    }

    public static boolean canAccess(@Nullable IGrid grid, IActionSource src) {
        if (grid == null) {
            return false;
        }
        var security = SecurityService.get(grid);
        if (src.player().isPresent()) {
            return security.hasPermission(src.player().get(), SecurityPermissions.BUILD);
        } else if (src.machine().isPresent()) {
            final IGridNode n = src.machine().get().getActionableNode();
            if (n == null) {
                return false;
            }

            return security.hasPermission(n.getOwningPlayerId(), SecurityPermissions.BUILD);
        } else {
            return false;
        }
    }

    public static boolean securityCheck(IGridNode a, IGridNode b) {
        var aKey = SecurityNode.of(a).securityterminal$getLastSecurityKey();
        var bKey = SecurityNode.of(b).securityterminal$getLastSecurityKey();
        if (aKey == -1 && bKey == -1) {
            return true;
        } else if (aKey == bKey) {
            return true;
        }

        final boolean aIsSecure = a.isPowered() && aKey != -1;
        final boolean bIsSecure = b.isPowered() && bKey != -1;

        // can't do that son...
        if (aIsSecure && bIsSecure) {
            return false;
        }

        if (!aIsSecure && bIsSecure) {
            return checkPlayerPermissions(getGrid(b), a.getOwningPlayerId());
        }

        if (aIsSecure) {
            return checkPlayerPermissions(getGrid(a), b.getOwningPlayerId());
        }

        return true;
    }

    private static boolean checkPlayerPermissions(@Nullable IGrid grid, int playerID) {
        if (grid == null) {
            return true;
        }

        final SecurityService gs = SecurityService.get(grid);

        if (!gs.isAvailable()) {
            return true;
        }

        return gs.hasPermission(playerID, SecurityPermissions.BUILD);
    }

    public static void checkConnection(IGridNode a, IGridNode b) {
        if (!securityCheck(a, b)) {
            throw new SecurityConnectionException();
        }
    }

    /**
     * Removes the machine hosting the node on the next tick, dropping it as an item.
     */
    public static void scheduleSecurityBreak(IGridNode node) {
        var level = node.getLevel();
        TickHandler.instance().addCallable(level, () -> securityBreak(node));
    }

    private static void securityBreak(IGridNode node) {
        if (node.getOwner() instanceof AEBasePart part) {
            var host = part.getHost();
            if (host != null && host.getPart(part.getSide()) == part) {
                var drops = new ArrayList<ItemStack>();
                drops.add(new ItemStack(part.getPartItem()));
                var be = host.getBlockEntity();
                host.removePart(part);
                Platform.spawnDrops(be.getLevel(), be.getBlockPos(), drops);
            }
        } else if (node.getOwner() instanceof BlockEntity blockEntity && !blockEntity.isRemoved()) {
            blockEntity.getLevel().destroyBlock(blockEntity.getBlockPos(), true);
        }
    }

    /**
     * Finds an AE2 interface facing the given position from the given side.
     */
    @Nullable
    public static InterfaceLogicHost findInterface(Level level, BlockPos pos, Direction side) {
        var be = level.getBlockEntity(pos);
        if (be instanceof InterfaceLogicHost host) {
            return host;
        }
        if (be instanceof IPartHost partHost && partHost.getPart(side) instanceof InterfaceLogicHost host) {
            return host;
        }
        return null;
    }

    @Nullable
    public static IGrid getInterfaceGrid(InterfaceLogicHost host) {
        return host instanceof IActionHost actionHost ? getGrid(actionHost.getActionableNode()) : null;
    }

    public static InterfaceLogic getLogic(InterfaceLogicHost host) {
        return host.getInterfaceLogic();
    }

    public static boolean isServer(@Nullable Level level) {
        return level instanceof ServerLevel;
    }
}
