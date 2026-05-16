package com.mossman.network.payload;

import com.mossman.MossManager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client asks the server to create a new project owned by the requesting player. */
public record CreateProjectC2S(String id, String name) implements CustomPacketPayload {

    public static final Type<CreateProjectC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MossManager.MOD_ID, "create_project_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateProjectC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CreateProjectC2S::id,
            ByteBufCodecs.STRING_UTF8, CreateProjectC2S::name,
            CreateProjectC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
