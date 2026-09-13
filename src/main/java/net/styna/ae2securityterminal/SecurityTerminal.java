package net.styna.ae2securityterminal;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.api.AECapabilities;
import appeng.api.ids.AECreativeTabIds;
import appeng.api.movable.BlockEntityMoveStrategies;
import appeng.api.movable.IBlockEntityMoveStrategy;
import appeng.api.networking.GridServices;
import appeng.blockentity.AEBaseBlockEntity;

import net.styna.ae2securityterminal.api.ISecurityService;
import net.styna.ae2securityterminal.block.SecurityStationBlock;
import net.styna.ae2securityterminal.block.SecurityStationBlockEntity;
import net.styna.ae2securityterminal.item.BiometricCardItem;
import net.styna.ae2securityterminal.item.BiometricProfile;
import net.styna.ae2securityterminal.menu.SecurityStationMenu;
import net.styna.ae2securityterminal.security.SecurityService;
import net.styna.ae2securityterminal.security.StationProtection;

@Mod(SecurityTerminal.MODID)
public class SecurityTerminal {
    public static final String MODID = "securityterminal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister
            .createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BiometricProfile>> BIOMETRIC_PROFILE = COMPONENTS
            .registerComponentType("biometric_profile", builder -> builder
                    .persistent(BiometricProfile.CODEC)
                    .networkSynchronized(BiometricProfile.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BIOMETRIC_PERMISSIONS = COMPONENTS
            .registerComponentType("biometric_permissions", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredBlock<SecurityStationBlock> SECURITY_STATION = BLOCKS.registerBlock("security_station",
            SecurityStationBlock::new);
    public static final DeferredItem<BlockItem> SECURITY_STATION_ITEM = ITEMS.registerSimpleBlockItem(
            "security_station", SECURITY_STATION);
    public static final DeferredItem<BiometricCardItem> BIOMETRIC_CARD = ITEMS.registerItem("biometric_card",
            BiometricCardItem::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SecurityStationBlockEntity>> SECURITY_STATION_BE = BLOCK_ENTITIES
            .register("security_station", () -> {
                var typeHolder = new AtomicReference<BlockEntityType<SecurityStationBlockEntity>>();
                var type = new BlockEntityType<>(
                        (pos, state) -> new SecurityStationBlockEntity(typeHolder.get(), pos, state),
                        SECURITY_STATION.get());
                typeHolder.setPlain(type);
                AEBaseBlockEntity.registerBlockEntityItem(type, SECURITY_STATION_ITEM.get());
                SECURITY_STATION.get().setBlockEntity(SecurityStationBlockEntity.class, type, null, null);
                return type;
            });

    public SecurityTerminal(IEventBus modEventBus) {
        MENUS.register("security_station", () -> SecurityStationMenu.TYPE);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        COMPONENTS.register(modEventBus);

        GridServices.register(ISecurityService.class, SecurityService.class);
        BlockEntityMoveStrategies.add(new SecurityStationMoveStrategy());

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.register(new StationProtection());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, SECURITY_STATION_BE.get(),
                (station, context) -> station);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == AECreativeTabIds.MAIN) {
            event.accept(SECURITY_STATION_ITEM);
            event.accept(BIOMETRIC_CARD);
        }
    }

    /**
     * Spatial IO leaves active security stations in place.
     */
    private static class SecurityStationMoveStrategy implements IBlockEntityMoveStrategy {
        @Override
        public boolean canHandle(BlockEntityType<?> type) {
            return type == SECURITY_STATION_BE.get();
        }

        @Override
        public CompoundTag beginMove(BlockEntity blockEntity, HolderLookup.Provider registries) {
            if (blockEntity instanceof SecurityStationBlockEntity station && station.isSecurityEnabled()) {
                return null;
            }
            return BlockEntityMoveStrategies.getDefault().beginMove(blockEntity, registries);
        }

        @Override
        public boolean completeMove(BlockEntity entity, BlockState state, CompoundTag savedData, Level newLevel,
                BlockPos newPosition) {
            return BlockEntityMoveStrategies.getDefault().completeMove(entity, state, savedData, newLevel,
                    newPosition);
        }
    }
}
