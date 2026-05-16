package com.mossman.fabric.client;

import com.mossman.MossManagerClient;

import net.fabricmc.api.ClientModInitializer;

public final class MossManagerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MossManagerClient.init();
    }
}
