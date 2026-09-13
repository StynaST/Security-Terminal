package net.styna.ae2securityterminal.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import appeng.util.Icon;

import net.styna.ae2securityterminal.SecurityTerminal;
import net.styna.ae2securityterminal.api.SecurityPermissions;

/**
 * Toggle button using the security permission icons of the original terminal.
 */
public class PermissionToggleButton extends IconButton {

    private static final Identifier TEXTURE = SecurityTerminal.id("textures/guis/security_states.png");

    private final SecurityPermissions permission;
    private boolean state;

    public PermissionToggleButton(SecurityPermissions permission, Runnable onPress) {
        super(btn -> onPress.run());
        this.permission = permission;
        this.setMessage(permission.getDisplayName());
    }

    public void setState(boolean isOn) {
        this.state = isOn;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partial) {
        super.extractContents(guiGraphics, mouseX, mouseY, partial);
        if (this.visible) {
            var yOffset = isHovered() ? 1 : 0;
            Blitter.texture(TEXTURE, 80, 32)
                    .src(this.permission.ordinal() * 16, this.state ? 0 : 16, 16, 16)
                    .dest(getX(), getY() + 1 + yOffset)
                    .blit(guiGraphics);
        }
    }

    @Nullable
    @Override
    protected Icon getIcon() {
        return null;
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(this.permission.getDisplayName(), this.permission.getTooltip());
    }
}
