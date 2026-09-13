package net.styna.ae2securityterminal.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import appeng.api.util.AEColor;
import appeng.client.render.CubeBuilder;
import appeng.client.render.ItemBaseModelWrapper;

import net.styna.ae2securityterminal.SecurityTerminal;
import net.styna.ae2securityterminal.item.BiometricCardItem;

/**
 * Renders the hash of the encoded player on top of the biometric card.
 */
public class BiometricCardItemModel implements ItemModel {
    private final ItemBaseModelWrapper baseModel;
    private final LoadingCache<Integer, List<BakedQuad>> hashModelCache;
    private final Matrix4fc transform;

    public BiometricCardItemModel(ItemBaseModelWrapper baseModel, Material.Baked hashSprite, Matrix4fc transform) {
        this.baseModel = baseModel;
        this.hashModelCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(new CacheLoader<>() {
                    @Override
                    public List<BakedQuad> load(Integer hash) {
                        return buildGeneralQuads(hashSprite, hash);
                    }
                });
        this.transform = transform;
    }

    @Override
    public void update(ItemStackRenderState renderState,
            ItemStack stack,
            ItemModelResolver itemModelResolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed) {
        if (!(stack.getItem() instanceof BiometricCardItem)) {
            return;
        }

        renderState.appendModelIdentityElement(this);

        var baseLayer = renderState.newLayer();
        baseModel.applyToLayer(baseLayer, displayContext);

        String username = "";
        final var gp = BiometricCardItem.getProfile(stack);
        if (gp != null) {
            if (gp.id() != null) {
                username = gp.id().toString();
            } else {
                username = gp.name();
            }
        }
        final int hash = !username.isEmpty() ? username.hashCode() : 0;

        var hashLayer = renderState.newLayer();
        hashLayer.setLocalTransform(transform);
        hashLayer.setExtents(baseModel.extents());
        baseModel.renderProperties().applyToLayer(hashLayer, displayContext);
        hashLayer.prepareQuadList().addAll(hashModelCache.getUnchecked(hash));
        renderState.appendModelIdentityElement(hash);
    }

    private static List<BakedQuad> buildGeneralQuads(Material.Baked texture, int hash) {
        var quads = new ArrayList<BakedQuad>();
        CubeBuilder builder = new CubeBuilder(quads::add);

        builder.setTexture(texture);

        AEColor col = AEColor.values()[Math.abs(3 + hash) % AEColor.values().length];
        if (hash == 0) {
            col = AEColor.BLACK;
        }

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 6; y++) {
                final boolean isLit;

                // This makes the border always use the darker color
                if (x == 0 || y == 0 || x == 7 || y == 5) {
                    isLit = false;
                } else {
                    isLit = (hash & (1 << x)) != 0 || (hash & (1 << y)) != 0;
                }

                if (isLit) {
                    builder.setColorRGB(col.mediumVariant);
                } else {
                    final float scale = 0.3f / 255.0f;
                    builder.setColorRGB(((col.blackVariant >> 16) & 0xff) * scale,
                            ((col.blackVariant >> 8) & 0xff) * scale,
                            (col.blackVariant & 0xff) * scale);
                }

                builder.addCube(4 + x, 6 + y, 7.5f, 4 + x + 1, 6 + y + 1, 8.5f);
            }
        }
        return quads;
    }

    public record Unbaked(Identifier baseModel, Identifier hashSprite) implements ItemModel.Unbaked {
        public static final Identifier ID = SecurityTerminal.id("biometric_card");

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
                builder -> builder.group(
                        Identifier.CODEC.fieldOf("base_model").forGetter(Unbaked::baseModel),
                        Identifier.CODEC.fieldOf("hash_sprite").forGetter(Unbaked::hashSprite))
                        .apply(builder, Unbaked::new));

        @Override
        public BiometricCardItemModel bake(BakingContext context, Matrix4fc transform) {
            ModelDebugName debugName = getClass()::toString;
            var hashMaterial = new Material(hashSprite);
            var sprite = context.blockModelBaker().materials().get(hashMaterial, debugName);

            var base = ItemBaseModelWrapper.bake(context.blockModelBaker(), this.baseModel, transform);
            return new BiometricCardItemModel(base, sprite, transform);
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(baseModel);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
