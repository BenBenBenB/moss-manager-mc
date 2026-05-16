package com.mossman.neoforge;

import com.mossman.MossManager;

import net.neoforged.fml.common.Mod;

@Mod(MossManager.MOD_ID)
public final class MossManagerNeoForge {
    public MossManagerNeoForge() {
        MossManager.init();
    }
}
