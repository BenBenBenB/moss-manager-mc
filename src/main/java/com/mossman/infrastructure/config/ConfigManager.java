package com.mossman.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mossman.MossManMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "mossman.json");

    private static ModConfig config;

    public static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                MossManMod.LOGGER.error("Failed to load MossMan configuration", e);
            }
        }
        
        if (config == null) {
            config = new ModConfig(); // using defaults
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            MossManMod.LOGGER.error("Failed to save MossMan configuration", e);
        }
    }

    public static ModConfig getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public static class ModConfig {
        public boolean enableCommands = true;
        // future configuration flags can be added here
    }
}
