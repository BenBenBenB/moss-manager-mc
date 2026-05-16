package com.mossman.network;

import com.mossman.network.payload.CreateProjectC2S;
import com.mossman.network.payload.SyncProjectS2C;

import dev.architectury.networking.NetworkManager;

/** Registers every wire-format payload type the mod uses. */
public final class Packets {

    private Packets() {}

    public static void register() {
        NetworkManager.registerS2CPayloadType(SyncProjectS2C.TYPE, SyncProjectS2C.CODEC);
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                CreateProjectC2S.TYPE,
                CreateProjectC2S.CODEC,
                ServerProjectSync::handleCreateProject);
    }
}
