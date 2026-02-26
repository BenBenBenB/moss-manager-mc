package com.mossman;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side entry point for the MossMan GUI mod.
 * GUI screens and client-side event handlers will be registered here.
 * Access core use cases and repositories via {@link MossManApi}.
 */
public class MossManGuiMod implements ClientModInitializer {
    public static final String MOD_ID = "mossman-gui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("MossMan GUI initialized (scaffold — no screens registered yet).");
    }
}
