package com.ribeirin.ribscombatlog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PluginConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // default value - needs to be changed in config.json in future
    private long combatTagDurationSeconds = 15;

    public PluginConfig() {
    }

    public long getCombatTagDurationSeconds() {
        return combatTagDurationSeconds;
    }

    public void setCombatTagDurationSeconds(long combatTagDurationSeconds) {
        this.combatTagDurationSeconds = combatTagDurationSeconds;
    }

    public static PluginConfig loadOrCreate(Path configDir) throws IOException {
        Path configFile = configDir.resolve("config.json");

        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                PluginConfig config = GSON.fromJson(reader, PluginConfig.class);
                if (config == null) {
                    config = new PluginConfig();
                    config.save(configFile);
                }
                return config;
            }
        } else {
            PluginConfig config = new PluginConfig();
            config.save(configFile);
            return config;
        }
    }

    public void save(Path configFile) throws IOException {
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(this, writer);
        }
    }
}
