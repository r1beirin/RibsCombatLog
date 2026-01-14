package com.ribeirin.ribscombatlog;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.nio.file.Path;
import java.util.logging.Level;

public class RibsCombatLog extends JavaPlugin {

    private CombatLogPlugin combatLogPlugin;
    private PluginConfig config;

    public RibsCombatLog(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[RibsCombatLog]  initializing...");

        try {
            Path configDir = getDataDirectory().resolve("RibsCombatLog");
            config = PluginConfig.loadOrCreate(configDir);
            getLogger().at(Level.INFO).log("[RibsCombatLog] Config loaded: combatTagDurationSeconds = %d", config.getCombatTagDurationSeconds());
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).log("[RibsCombatLog] Failed to load config, using defaults: %s", e.getMessage());
            config = new PluginConfig();
        }

        combatLogPlugin = new CombatLogPlugin(this, config);
        combatLogPlugin.enable();

        getLogger().at(Level.INFO).log("[RibsCombatLog] Plugin loaded successfully");
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
