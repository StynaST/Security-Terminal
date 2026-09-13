package net.styna.ae2securityterminal.api;

import java.util.Locale;

import net.minecraft.network.chat.Component;

public enum SecurityPermissions {
    /**
     * required to insert items into the network via terminal ( also used for machines based on the owner of the
     * network, which is determined by its Security Block. )
     */
    INJECT,

    /**
     * required to extract items from the network via terminal ( also used for machines based on the owner of the
     * network, which is determined by its Security Block. )
     */
    EXTRACT,

    /**
     * required to request crafting from the network via terminal.
     */
    CRAFT,

    /**
     * required to modify automation, and make modifications to the networks physical layout.
     */
    BUILD,

    /**
     * required to modify the security blocks settings.
     */
    SECURITY;

    private final String translationKey = "gui.securityterminal.security." + this.name().toLowerCase(Locale.ROOT);

    public Component getDisplayName() {
        return Component.translatable(this.translationKey + ".name");
    }

    public Component getTooltip() {
        return Component.translatable(this.translationKey + ".tip");
    }
}
