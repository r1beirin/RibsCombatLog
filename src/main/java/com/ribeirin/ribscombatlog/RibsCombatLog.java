package com.ribeirin.ribscombatlog;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.ribeirin.ribscombatlog.commands.CombatLogCommand;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

public class RibsCombatLog extends JavaPlugin {

    private CombatLogPlugin combatLogPlugin;
    private PluginConfig config;
    private Path configDir;

    public RibsCombatLog(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[RibsCombatLog] initializing");

        try {
            configDir = getDataDirectory().resolve("RibsCombatLog");
            config = PluginConfig.loadOrCreate(configDir);
            getLogger().at(Level.INFO).log("[RibsCombatLog] Config loaded: combatTagDurationSeconds = %d", config.getCombatTagDurationSeconds());
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).log("[RibsCombatLog] Failed to load config, using defaults: %s", e.getMessage());
            config = new PluginConfig();
        }

        combatLogPlugin = new CombatLogPlugin(this, config);
        combatLogPlugin.enable();

        getCommandRegistry().registerCommand(new CombatLogCommand(this));

        getLogger().at(Level.INFO).log("[RibsCombatLog] Plugin loaded successfully");
    }

    public void reloadPlugin() throws IOException {
        getLogger().at(Level.INFO).log("[RibsCombatLog] Reloading configuration");

        PluginConfig newConfig = PluginConfig.loadOrCreate(configDir);
        this.config = newConfig;
        combatLogPlugin.reload(newConfig);

        getLogger().at(Level.INFO).log("[RibsCombatLog] Configuration reloaded successfully");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[RibsCombatLog] Shutting down");

        if (combatLogPlugin != null) {
            combatLogPlugin.disable();
        }

        getLogger().at(Level.INFO).log("[RibsCombatLog] Disabled");
    }

    public CombatLogPlugin getCombatLogPlugin() {
        return combatLogPlugin;
    }

    public PluginConfig getPluginConfig() {
        return config;
    }
}
