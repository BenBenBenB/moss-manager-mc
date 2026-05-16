package com.mossman.neoforge.client;

import com.mossman.MossManager;
import com.mossman.MossManagerClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = MossManager.MOD_ID, dist = Dist.CLIENT)
public final class MossManagerNeoForgeClient {
    public MossManagerNeoForgeClient() {
        MossManagerClient.init();
    }
}
