package com.ribeirin.ribscombatlog;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * a wrapper command that blocks execution when the player is in combat.
 * this is used to wrap existing commands and add combat checking.
 */
public class CombatAwareCommand extends AbstractCommand {

    private final AbstractCommand wrappedCommand;
    private final CombatLogPlugin combatLogPlugin;
    private final PluginConfig config;

    public CombatAwareCommand(AbstractCommand wrappedCommand, CombatLogPlugin combatLogPlugin, PluginConfig config) {
        super(wrappedCommand.getName(), wrappedCommand.getDescription());
        this.wrappedCommand = wrappedCommand;
        this.combatLogPlugin = combatLogPlugin;
        this.config = config;

        for (String alias : wrappedCommand.getAliases()) {
            this.addAliases(alias);
        }
    }

    @Override
    @Nullable
    public CompletableFuture<Void> acceptCall(@Nonnull CommandSender sender, @Nonnull ParserContext context, @Nonnull ParseResult parseResult) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            @SuppressWarnings("deprecation")
            PlayerRef playerRef = player.getPlayerRef();

            if (playerRef != null && combatLogPlugin.isInCombat(playerRef.getUuid())) {
                playerRef.sendMessage(Message.raw(config.getCommandBlockedMessage()));
                return CompletableFuture.completedFuture(null);
            }
        }

        return wrappedCommand.acceptCall(sender, context, parseResult);
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        // this should not be called directly since we override acceptCall
        // but if it is, delegate to the wrapped command's execute
        return null;
    }
}
