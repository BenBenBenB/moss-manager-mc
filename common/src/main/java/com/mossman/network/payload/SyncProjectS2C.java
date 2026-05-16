package com.mossman.network.payload;

import com.mossman.MossManager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server pushes one project's full state to a client. The payload is the
 * GSON-encoded Project body — that lets us reuse the persistence codec rather
 * than maintaining a parallel StreamCodec graph for the whole domain.
 */
public record SyncProjectS2C(String projectJson) implements CustomPacketPayload {

    public static final Type<SyncProjectS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MossManager.MOD_ID, "sync_project_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncProjectS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncProjectS2C::projectJson,
            SyncProjectS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
