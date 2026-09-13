package net.styna.ae2securityterminal.item;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The player encoded on a biometric card.
 */
public record BiometricProfile(UUID id, String name) {
    public static final Codec<BiometricProfile> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(BiometricProfile::id),
            Codec.STRING.fieldOf("name").forGetter(BiometricProfile::name))
            .apply(builder, BiometricProfile::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BiometricProfile> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BiometricProfile::id,
            ByteBufCodecs.STRING_UTF8, BiometricProfile::name,
            BiometricProfile::new);

    public static BiometricProfile of(GameProfile profile) {
        return new BiometricProfile(profile.id(), profile.name());
    }
}
