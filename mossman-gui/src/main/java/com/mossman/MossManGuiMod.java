package com.mossman;

import com.mossman.MossManApi;
import com.mossman.gui.screens.ProjectListScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MossManGuiMod implements ClientModInitializer {
    public static final String MOD_ID = "mossman-gui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // Register as a server-side command so it shares the /mossman tree with admin,
        // project, etc. rather than shadowing them with a client-side dispatcher.
        // This callback only fires in integrated-server contexts (the GUI mod is
        // environment: "client" so it never loads on a dedicated server).
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            MossManApi.registerSubcommand(dispatcher,
                    CommandManager.literal("gui")
                            .executes(ctx -> {
                                MinecraftClient client = MinecraftClient.getInstance();
                                if (client != null) {
                                    client.execute(() -> client.setScreen(new ProjectListScreen(null)));
                                }
                                return 1;
                            })));
        LOGGER.info("MossMan GUI initialized.");
    }
}
