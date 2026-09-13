package net.styna.ae2securityterminal.item;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import appeng.api.features.IPlayerRegistry;
import appeng.items.AEBaseItem;

import net.styna.ae2securityterminal.SecurityTerminal;
import net.styna.ae2securityterminal.api.SecurityPermissions;

public class BiometricCardItem extends AEBaseItem {

    public BiometricCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            this.encode(player.getItemInHand(hand), player);
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack is, Player player, LivingEntity target,
            InteractionHand hand) {
        if (target instanceof Player targetPlayer && !player.isShiftKeyDown()) {
            if (player.getAbilities().instabuild) {
                is = player.getItemInHand(hand);
            }
            this.encode(is, targetPlayer);
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Component getName(ItemStack is) {
        final var username = getProfile(is);
        return username != null ? super.getName(is).copy().append(" - " + username.name()) : super.getName(is);
    }

    private void encode(ItemStack is, Player p) {
        final var username = getProfile(is);
        final var playerProfile = BiometricProfile.of(p.getGameProfile());

        if (username != null && username.equals(playerProfile)) {
            setProfile(is, null);
        } else {
            setProfile(is, playerProfile);
        }
    }

    public static void setProfile(ItemStack itemStack, @Nullable BiometricProfile profile) {
        if (profile != null) {
            itemStack.set(SecurityTerminal.BIOMETRIC_PROFILE.get(), profile);
        } else {
            itemStack.remove(SecurityTerminal.BIOMETRIC_PROFILE.get());
        }
    }

    @Nullable
    public static BiometricProfile getProfile(ItemStack is) {
        return is.get(SecurityTerminal.BIOMETRIC_PROFILE.get());
    }

    public static EnumSet<SecurityPermissions> getPermissions(ItemStack is) {
        final int mask = is.getOrDefault(SecurityTerminal.BIOMETRIC_PERMISSIONS.get(), 0);
        final EnumSet<SecurityPermissions> result = EnumSet.noneOf(SecurityPermissions.class);

        for (var sp : SecurityPermissions.values()) {
            if ((mask & (1 << sp.ordinal())) != 0) {
                result.add(sp);
            }
        }

        return result;
    }

    public static boolean hasPermission(ItemStack is, SecurityPermissions permission) {
        return getPermissions(is).contains(permission);
    }

    public static void removePermission(ItemStack itemStack, SecurityPermissions permission) {
        final int mask = itemStack.getOrDefault(SecurityTerminal.BIOMETRIC_PERMISSIONS.get(), 0)
                & ~(1 << permission.ordinal());
        if (mask == 0) {
            itemStack.remove(SecurityTerminal.BIOMETRIC_PERMISSIONS.get());
        } else {
            itemStack.set(SecurityTerminal.BIOMETRIC_PERMISSIONS.get(), mask);
        }
    }

    public static void addPermission(ItemStack itemStack, SecurityPermissions permission) {
        itemStack.set(SecurityTerminal.BIOMETRIC_PERMISSIONS.get(),
                itemStack.getOrDefault(SecurityTerminal.BIOMETRIC_PERMISSIONS.get(), 0) | (1 << permission.ordinal()));
    }

    public void registerPermissions(Map<Integer, EnumSet<SecurityPermissions>> registry, IPlayerRegistry pr,
            ItemStack is) {
        final var profile = getProfile(is);
        registry.put(profile == null ? -1 : pr.getPlayerId(profile.id()), getPermissions(is));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> lines, TooltipFlag advancedTooltips) {
        final EnumSet<SecurityPermissions> perms = getPermissions(stack);
        if (perms.isEmpty()) {
            lines.accept(Component.translatable("gui.securityterminal.NoPermissions"));
        } else {
            Component msg = null;

            for (var sp : perms) {
                if (msg == null) {
                    msg = sp.getDisplayName();
                } else {
                    msg = msg.copy().append(", ").append(sp.getDisplayName());
                }
            }
            lines.accept(msg);
        }
    }
}
