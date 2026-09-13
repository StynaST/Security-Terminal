package net.styna.ae2securityterminal;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import appeng.client.InitScreens;
import appeng.client.render.ColorableBlockEntityBlockColor;

import net.styna.ae2securityterminal.client.BiometricCardItemModel;
import net.styna.ae2securityterminal.client.SecurityStationScreen;
import net.styna.ae2securityterminal.menu.SecurityStationMenu;

@Mod(value = SecurityTerminal.MODID, dist = Dist.CLIENT)
public class SecurityTerminalClient {
    public SecurityTerminalClient(IEventBus modEventBus) {
        modEventBus.addListener(SecurityTerminalClient::registerScreens);
        modEventBus.addListener(SecurityTerminalClient::registerBlockTintSources);
        modEventBus.addListener(SecurityTerminalClient::registerItemModels);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, SecurityStationMenu.TYPE, SecurityStationScreen::new,
                "/screens/securityterminal/security_station.json");
    }

    private static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(ColorableBlockEntityBlockColor.TINT_SOURCES, SecurityTerminal.SECURITY_STATION.get());
    }

    private static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(BiometricCardItemModel.Unbaked.ID, BiometricCardItemModel.Unbaked.MAP_CODEC);
    }
}
