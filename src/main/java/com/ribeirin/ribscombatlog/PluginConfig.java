package com.ribeirin.ribscombatlog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PluginConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // default value - needs to be changed in config.json in future
    private long combatTagDurationSeconds = 15;

    // default values
    private String combatEnterMessage = "[RibsCombatLog] You are in pvp";
    private String combatLeaveMessage = "[RibsCombatLog] You are not more in pvp";
    private String commandBlockedMessage = "[RibsCombatLog] You cannot use this command while in combat!";

    // default values - blocked commands (commands that cannot be used while in combat)
    private List<String> blockedCommands = List.of("spawn", "teleport", "warp", "home");

    public PluginConfig() {
    }

    public long getCombatTagDurationSeconds() {
        return combatTagDurationSeconds;
    }

    public void setCombatTagDurationSeconds(long combatTagDurationSeconds) {
        this.combatTagDurationSeconds = combatTagDurationSeconds;
    }

    public String getCombatEnterMessage() {
        return combatEnterMessage;
    }

    public void setCombatEnterMessage(String combatEnterMessage) {
        this.combatEnterMessage = combatEnterMessage;
    }

    public String getCombatLeaveMessage() {
        return combatLeaveMessage;
    }

    public void setCombatLeaveMessage(String combatLeaveMessage) {
        this.combatLeaveMessage = combatLeaveMessage;
    }

    public String getCommandBlockedMessage() {
        return commandBlockedMessage;
    }

    public void setCommandBlockedMessage(String commandBlockedMessage) {
        this.commandBlockedMessage = commandBlockedMessage;
    }

    public List<String> getBlockedCommands() {
        return blockedCommands;
    }

    public void setBlockedCommands(List<String> blockedCommands) {
        this.blockedCommands = blockedCommands;
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
