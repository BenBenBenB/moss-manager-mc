package com.mossman.fabric;

import com.mossman.MossManager;

import net.fabricmc.api.ModInitializer;

public final class MossManagerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MossManager.init();
    }
}
