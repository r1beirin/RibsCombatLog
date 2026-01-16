package com.ribeirin.ribscombatlog.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.ribeirin.ribscombatlog.RibsCombatLog;

import javax.annotation.Nonnull;

public class CombatLogCommand extends CommandBase {

    private final RibsCombatLog plugin;

    public CombatLogCommand(RibsCombatLog plugin) {
        super("rcombatlog", "server.commands.rcombatlog.desc");
        this.plugin = plugin;

        this.addSubCommand(new ReloadSubCommand());
        this.addSubCommand(new StatusSubCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw("[RibsCombatLog] Available commands:"));
        context.sendMessage(Message.raw("  /rcombatlog reload - Reload configuration"));
        context.sendMessage(Message.raw("  /rcombatlog status - Show plugin status"));
    }

    private class ReloadSubCommand extends CommandBase {
        ReloadSubCommand() {
            super("reload", "server.commands.combatlog.reload.desc");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            try {
                plugin.reloadPlugin();
                context.sendMessage(Message.raw("[RibsCombatLog] Configuration reloaded successfully!"));
            } catch (Exception e) {
                context.sendMessage(Message.raw("[RibsCombatLog] Failed to reload: " + e.getMessage()));
            }
        }
    }


    private class StatusSubCommand extends CommandBase {
        StatusSubCommand() {
            super("status", "server.commands.combatlog.status.desc");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            var config = plugin.getPluginConfig();
            var combatPlugin = plugin.getCombatLogPlugin();

            context.sendMessage(Message.raw("[RibsCombatLog] Plugin Status:"));
            context.sendMessage(Message.raw("  Combat tag duration: " + config.getCombatTagDurationSeconds() + " seconds"));
            context.sendMessage(Message.raw("  Blocked commands: " + config.getBlockedCommands()));
            context.sendMessage(Message.raw("  Enter message: " + config.getCombatEnterMessage()));
            context.sendMessage(Message.raw("  Leave message: " + config.getCombatLeaveMessage()));
        }
    }
}
