package net.styna.ae2securityterminal.block;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.features.IPlayerRegistry;
import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.ISubMenuHost;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.MEStorage;
import appeng.api.util.AEColor;
import appeng.api.util.IConfigManager;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;

import net.styna.ae2securityterminal.api.ISecurityProvider;
import net.styna.ae2securityterminal.api.SecurityPermissions;
import net.styna.ae2securityterminal.item.BiometricCardItem;
import net.styna.ae2securityterminal.menu.SecurityStationMenu;
import net.styna.ae2securityterminal.security.SecurityService;
import net.styna.ae2securityterminal.security.StationProtection;

public class SecurityStationBlockEntity extends AENetworkedBlockEntity
        implements ITerminalHost, ISubMenuHost, InternalInventoryHost, IColorableBlockEntity, ISecurityProvider {

    private static int difference = 0;
    private final AppEngInternalInventory configSlot = new AppEngInternalInventory(this, 1);
    private final IConfigManager cm = IConfigManager.builder(this::saveChanges)
            .registerSetting(Settings.SORT_BY, SortOrder.NAME)
            .registerSetting(Settings.VIEW_MODE, ViewItems.ALL)
            .registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING)
            .build();
    private final SecurityStationInventory inventory = new SecurityStationInventory(this);
    private long securityKey;
    private int ownerId = -1;
    private AEColor paintedColor = AEColor.TRANSPARENT;
    private boolean isActive = false;

    public SecurityStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getMainNode().setIdlePowerUsage(2.0);
        difference++;

        this.securityKey = System.currentTimeMillis() * 10 + difference;
        if (difference > 10) {
            difference = 0;
        }

        this.configSlot.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return stack.getItem() instanceof BiometricCardItem;
            }
        });
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        this.saveChanges();
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        if (!this.configSlot.isEmpty()) {
            drops.add(this.configSlot.getStackInSlot(0));
        }

        for (var stack : this.inventory.getStoredItems()) {
            drops.add(stack.what() instanceof appeng.api.stacks.AEItemKey itemKey
                    ? itemKey.toStack((int) stack.amount())
                    : ItemStack.EMPTY);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.configSlot.clear();
        this.inventory.clear();
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);
        final boolean wasActive = this.isActive;
        this.isActive = data.readBoolean();

        final AEColor oldPaintedColor = this.paintedColor;
        this.paintedColor = AEColor.values()[data.readByte()];

        return oldPaintedColor != this.paintedColor || wasActive != this.isActive || c;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.getMainNode().isActive());
        data.writeByte(this.paintedColor.ordinal());
    }

    @Override
    public void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        this.cm.writeToNBT(data);
        data.putByte("paintedColor", (byte) this.paintedColor.ordinal());

        data.putLong("securityKey", this.securityKey);
        data.putInt("ownerId", this.getOwner());
        this.configSlot.writeToNBT(data, "config");

        data.store("storedItems", GenericStack.CODEC.listOf(), this.inventory.getStoredItems());
    }

    @Override
    public void loadTag(ValueInput data) {
        super.loadTag(data);
        this.cm.readFromNBT(data);
        this.paintedColor = AEColor.values()[data.getByteOr("paintedColor", (byte) AEColor.TRANSPARENT.ordinal())];

        this.securityKey = data.getLongOr("securityKey", this.securityKey);
        this.ownerId = data.getIntOr("ownerId", data.read("proxy", CompoundTag.CODEC)
                .map(proxy -> proxy.getIntOr("p", -1)).orElse(-1));
        this.configSlot.readFromNBT(data, "config");

        this.inventory.clear();
        data.read("storedItems", GenericStack.CODEC.listOf()).ifPresent(this.inventory::load);
        var security = SecurityService.get(this.getMainNode().getGrid());
        if (security != null) {
            security.updatePermissions();
        }
    }

    public void inventoryChanged() {
        this.saveChanges();
        var security = SecurityService.get(this.getMainNode().getGrid());
        if (security != null) {
            security.updatePermissions();
        }
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        this.markForUpdate();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.isActive = false;
    }

    @Override
    public void onReady() {
        super.onReady();
        if (!isClientSide()) {
            this.ownerId = this.getOwner();
            if (this.ownerId >= 0) {
                this.getMainNode().setOwningPlayerId(this.ownerId);
            }
            this.inventoryChanged();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.isActive = false;
    }

    public boolean isActive() {
        return isClientSide() ? this.isActive : this.getMainNode().isActive();
    }

    @Override
    public MEStorage getInventory() {
        return this.inventory;
    }

    @Override
    public ILinkStatus getLinkStatus() {
        return ILinkStatus.ofManagedNode(this.getMainNode());
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.cm;
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(this.getBlockState().getBlock());
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(SecurityStationMenu.TYPE, player, MenuLocators.forBlockEntity(this));
    }

    @Override
    public long getSecurityKey() {
        return this.securityKey;
    }

    @Override
    public void readPermissions(Map<Integer, EnumSet<SecurityPermissions>> playerPerms) {
        final var pr = IPlayerRegistry.getMapping(this.getLevel());

        // read permissions
        if (pr != null) {
            for (var stack : this.inventory.getStoredItems()) {
                if (stack.what() instanceof appeng.api.stacks.AEItemKey itemKey
                        && itemKey.getItem() instanceof BiometricCardItem bc) {
                    bc.registerPermissions(playerPerms, pr, itemKey.toStack());
                }
            }
        }

        if (this.getOwner() >= 0) {
            playerPerms.put(this.getOwner(), EnumSet.allOf(SecurityPermissions.class));
        }
    }

    @Override
    public boolean isSecurityEnabled() {
        return !this.isRemoved() && this.getMainNode().isActive();
    }

    public boolean canPlayerAccess(@Nullable Player player) {
        if (player == null || player instanceof FakePlayer || this.isRemoved()) {
            return false;
        }
        var pr = IPlayerRegistry.getMapping(this.getLevel());
        if (pr == null) {
            return false;
        }
        int playerId = pr.getPlayerId(player.getGameProfile());
        if (this.getOwner() >= 0 && playerId == this.getOwner()) {
            return true;
        }
        var permissions = new HashMap<Integer, EnumSet<SecurityPermissions>>();
        this.readPermissions(permissions);
        var granted = permissions.get(playerId);
        if (granted == null) {
            granted = permissions.get(-1);
        }
        return granted != null && granted.contains(SecurityPermissions.SECURITY);
    }

    @Override
    public void setOwner(Player owner) {
        this.getMainNode().setOwningPlayer(owner);
        var registry = IPlayerRegistry.getMapping(owner.level());
        if (registry != null && !(owner instanceof FakePlayer)) {
            this.ownerId = registry.getPlayerId(owner.getGameProfile());
            this.inventoryChanged();
        }
    }

    @Override
    public int getOwner() {
        var node = this.getMainNode().getNode();
        return this.ownerId >= 0 ? this.ownerId : node == null ? -1 : node.getOwningPlayerId();
    }

    @Override
    public AEColor getColor() {
        return this.paintedColor;
    }

    @Override
    public boolean recolourBlock(Direction side, AEColor newPaintedColor, Player who) {
        if (!this.canPlayerAccess(who) || this.paintedColor == newPaintedColor) {
            return false;
        }

        this.paintedColor = newPaintedColor;
        this.saveChanges();
        this.markForUpdate();
        return true;
    }

    @Override
    public InteractionResult disassembleWithWrench(Player player, Level level, BlockHitResult hitResult,
            ItemStack wrench) {
        if (!level.isClientSide() && !this.canPlayerAccess(player)) {
            return InteractionResult.FAIL;
        }
        var result = new InteractionResult[1];
        StationProtection.removeByPlayer(level, hitResult.getBlockPos(), () -> {
            result[0] = super.disassembleWithWrench(player, level, hitResult, wrench);
            return true;
        });
        return result[0];
    }

    public AppEngInternalInventory getConfigSlot() {
        return this.configSlot;
    }
}
