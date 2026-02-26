package com.mossman;

import com.mossman.gui.screens.ProjectListScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MossManGuiMod implements ClientModInitializer {
    public static final String MOD_ID = "mossman-gui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal("mossman")
                                .then(ClientCommandManager.literal("gui")
                                        .executes(ctx -> {
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            client.execute(() -> client.setScreen(new ProjectListScreen(null)));
                                            return 1;
                                        }))
                )
        );
        LOGGER.info("MossMan GUI initialized.");
    }
}
