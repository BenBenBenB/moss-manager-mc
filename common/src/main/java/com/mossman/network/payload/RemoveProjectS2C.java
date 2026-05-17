package com.mossman.network.payload;

import com.mossman.MossManager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server tells clients a project is gone — either deleted or no longer visible. */
public record RemoveProjectS2C(String projectId) implements CustomPacketPayload {

    public static final Type<RemoveProjectS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MossManager.MOD_ID, "remove_project_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveProjectS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RemoveProjectS2C::projectId,
            RemoveProjectS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
