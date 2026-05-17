package com.mossman.network;

import com.mossman.network.payload.CreateProjectC2S;
import com.mossman.network.payload.RemoveProjectS2C;
import com.mossman.network.payload.SyncProjectS2C;

import dev.architectury.networking.NetworkManager;

/**
 * Registers every wire-format payload type the mod uses.
 *
 * <p>{@code registerReceiver(Side.S2C, ...)} already registers the payload
 * type with the play-S2C registry; the client receiver is only installed
 * when the runtime env is CLIENT. So this one call covers both "server can
 * send" and "client can receive" — calling {@code registerS2CPayloadType}
 * separately would duplicate the type registration and crash on launch.
 */
public final class Packets {

    private Packets() {}

    public static void register() {
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                CreateProjectC2S.TYPE,
                CreateProjectC2S.CODEC,
                ServerProjectSync::handleCreateProject);
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                SyncProjectS2C.TYPE,
                SyncProjectS2C.CODEC,
                ClientProjects::onSyncProject);
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                RemoveProjectS2C.TYPE,
                RemoveProjectS2C.CODEC,
                ClientProjects::onRemoveProject);
    }
}
