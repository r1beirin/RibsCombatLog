package com.ribeirin.ribscombatlog;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class CombatLogPlugin {

    private final RibsCombatLog main;
    private PluginConfig config;
    private PvpCombatTracker pvpTracker;
    private ScheduledExecutorService combatCheckScheduler;

    // store the last PVP hit time for each player (victim UUID -> last hit time)
    // maybe need another way to store this
    private final ConcurrentHashMap<UUID, Instant> pvpCombatMap = new ConcurrentHashMap<>();

    // track wrapped command registrations for cleanup
    private final ConcurrentHashMap<String, CommandRegistration> wrappedCommandRegistrations = new ConcurrentHashMap<>();

    // track original commands for restoration during reload
    private final ConcurrentHashMap<String, AbstractCommand> originalCommands = new ConcurrentHashMap<>();

    public CombatLogPlugin(RibsCombatLog main, PluginConfig config) {
        this.main = main;
        this.config = config;
    }

    public void reload(PluginConfig newConfig) {
        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Reloading plugin.");

        unwrapAllCommands();
        this.config = newConfig;
        wrapBlockedCommands();

        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Plugin reloaded. New combat tag duration: %d seconds",
                config.getCombatTagDurationSeconds());
        main.getLogger().at(Level.INFO).log("[RibsCombatLog] New blocked commands: %s", config.getBlockedCommands());
    }

    private void unwrapAllCommands() {
        CommandManager commandManager = CommandManager.get();
        if (commandManager == null) {
            return;
        }

        for (Map.Entry<String, AbstractCommand> entry : originalCommands.entrySet()) {
            String commandName = entry.getKey();
            AbstractCommand originalCommand = entry.getValue();

            commandManager.register(originalCommand);
            main.getLogger().at(Level.INFO).log("[RibsCombatLog] Restored original command: /%s", commandName);
        }

        wrappedCommandRegistrations.clear();
        originalCommands.clear();
    }

    public void enable() {
        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Plugin enabling");

        // register the PVP tracking system via DamageModule
        pvpTracker = new PvpCombatTracker();
        DamageModule.get().getInspectDamageGroup().getRegistry().registerSystem(pvpTracker);

        // register disconnect listener
        main.getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        // start the combat expiration checker
        combatCheckScheduler = Executors.newSingleThreadScheduledExecutor();
        combatCheckScheduler.scheduleAtFixedRate(this::checkCombatExpiration, 1, 1, TimeUnit.SECONDS);

        // register to wrap blocked commands after all plugins have loaded their commands
        // this ensures commands like /spawn, /teleport, /warp are registered before we wrap them
        main.getEventRegistry().registerGlobal(AllWorldsLoadedEvent.class, event -> {
            main.getLogger().at(Level.INFO).log("[RibsCombatLog] All worlds loaded, wrapping blocked commands...");
            wrapBlockedCommands();
        });

        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Plugin enabled. Combat tag duration: %d seconds",
                config.getCombatTagDurationSeconds());
        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Blocked commands will be wrapped after all plugins load: %s", config.getBlockedCommands());
    }

    /**
     * wraps the blocked commands with combat-aware versions
     */
    private void wrapBlockedCommands() {
        CommandManager commandManager = CommandManager.get();
        if (commandManager == null) {
            main.getLogger().at(Level.WARNING).log("[RibsCombatLog] CommandManager with problem. Skipping command wrapping");
            return;
        }

        Map<String, AbstractCommand> registeredCommands = commandManager.getCommandRegistration();

        for (String blockedCommandName : config.getBlockedCommands()) {
            String commandName = blockedCommandName.toLowerCase();

            // skip if already wrapped
            if (wrappedCommandRegistrations.containsKey(commandName)) {
                main.getLogger().at(Level.INFO).log("[RibsCombatLog] Command already wrapped: /%s", commandName);
                continue;
            }

            AbstractCommand originalCommand = registeredCommands.get(commandName);

            if (originalCommand != null) {
                // save the original command for restoration during reload/disable
                originalCommands.put(commandName, originalCommand);

                // create a combat-aware wrapper for this command
                CombatAwareCommand wrappedCommand = new CombatAwareCommand(originalCommand, this, config);

                // register the wrapped command (this replaces the original)
                CommandRegistration registration = commandManager.register(wrappedCommand);
                if (registration != null) {
                    wrappedCommandRegistrations.put(commandName, registration);
                    main.getLogger().at(Level.INFO).log("[RibsCombatLog] Wrapped command: /%s", commandName);
                }
            } else {
                main.getLogger().at(Level.WARNING).log("[RibsCombatLog] Command not found to wrap: /%s", commandName);
            }
        }
    }

    public void disable() {
        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Plugin disabling");

        if (combatCheckScheduler != null) {
            combatCheckScheduler.shutdown();
            try {
                combatCheckScheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                combatCheckScheduler.shutdownNow();
            }
        }

        // Restore original commands
        unwrapAllCommands();

        pvpCombatMap.clear();

        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Plugin disabled");
    }

    private void checkCombatExpiration() {
        Instant now = Instant.now();
        long combatDuration = config.getCombatTagDurationSeconds();

        Iterator<Map.Entry<UUID, Instant>> iterator = pvpCombatMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Instant> entry = iterator.next();
            UUID playerUuid = entry.getKey();
            Instant lastCombatTime = entry.getValue();

            Duration timeSinceCombat = Duration.between(lastCombatTime, now);
            if (timeSinceCombat.getSeconds() > combatDuration) {
                // combat tag expired, notify the player and remove from map
                iterator.remove();
                notifyPlayerLeftCombat(playerUuid);
            }
        }
    }

    private void notifyPlayerLeftCombat(UUID playerUuid) {
        PlayerRef playerRef = findPlayerByUuid(playerUuid);
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(config.getCombatLeaveMessage()));
            main.getLogger().at(Level.FINE).log("[RibsCombatLog] Player %s left combat", playerRef.getUsername());
        }
    }

    private void notifyPlayerEnteredCombat(UUID playerUuid) {
        PlayerRef playerRef = findPlayerByUuid(playerUuid);
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(config.getCombatEnterMessage()));
            main.getLogger().at(Level.FINE).log("[RibsCombatLog] Player %s entered combat", playerRef.getUsername());
        }
    }

    @Nullable
    private PlayerRef findPlayerByUuid(UUID uuid) {
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (playerRef.getUuid().equals(uuid)) {
                return playerRef;
            }
        }
        return null;
    }

    public boolean isCommandBlocked(UUID playerUuid, String command) {
        if (!isInCombat(playerUuid)) {
            return false;
        }

        String commandName = command.toLowerCase().split(" ")[0];
        for (String blockedCommand : config.getBlockedCommands()) {
            if (commandName.equals(blockedCommand.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void sendCommandBlockedMessage(PlayerRef playerRef) {
        playerRef.sendMessage(Message.raw(config.getCommandBlockedMessage()));
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        UUID playerUuid = playerRef.getUuid();
        String playerName = playerRef.getUsername();

        // check if player was in PVP combat
        Instant lastCombatTime = pvpCombatMap.get(playerUuid);
        if (lastCombatTime == null) {
            pvpCombatMap.remove(playerUuid);
            return;
        }

        Duration timeSinceCombat = Duration.between(lastCombatTime, Instant.now());
        if (timeSinceCombat.getSeconds() > config.getCombatTagDurationSeconds()) {
            // combat tag expired, player can leave safely
            pvpCombatMap.remove(playerUuid);
            return;
        }

        var reason = event.getDisconnectReason();
        String clientReason = (reason != null && reason.getClientDisconnectType() != null)
                ? reason.getClientDisconnectType().toString()
                : "Unknown";

        String serverReason = (reason != null && reason.getServerDisconnectReason() != null)
                ? reason.getServerDisconnectReason().toString()
                : "None";

        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Player %s disconnected while in combat", playerName);
        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Reason getClientDisconnectType: %s", clientReason);
        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Reason getServerDisconnectReason: %s", serverReason);

        // verify if are natural Disconnect
        // if not "Disconnect" (ex: timeout, kick, crash), just pass.
        if (!clientReason.equals("Disconnect")) {
            main.getLogger().at(Level.WARNING).log("[RibsCombatLog] Player %s was in combat but reason was NOT Disconnect",
                    playerName, clientReason);
            pvpCombatMap.remove(playerUuid);
            return;
        }

        main.getLogger().at(Level.INFO).log("[RibsCombatLog] Punishing player %s for combat logging! Killing and dropping items",
                playerName);

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            main.getLogger().at(Level.WARNING).log("[RibsCombatLog] Could not get entity reference for combat logger %s",
                    playerName);
            pvpCombatMap.remove(playerUuid);
            return;
        }

        // execute death and item drop
        Store<EntityStore> store = entityRef.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            try {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null || !ref.isValid()) {
                    return;
                }

                Player playerComponent = store.getComponent(ref, Player.getComponentType());
                if (playerComponent == null) {
                    return;
                }

                dropAllItems(ref, playerComponent, store);
                killPlayer(ref, store);

                main.getLogger().at(Level.INFO).log("[RibsCombatLog] Player %s has been killed and items dropped",
                        playerName);

            } catch (Exception e) {
                main.getLogger().at(Level.SEVERE).log("[RibsCombatLog] Error handling combat logger. Player %s: %s",
                        playerName, e.getMessage());
            }
        });

        pvpCombatMap.remove(playerUuid);
    }

    private void dropAllItems(Ref<EntityStore> ref, Player player, ComponentAccessor<EntityStore> accessor) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        dropContainerItems(ref, inventory.getHotbar(), accessor);
        dropContainerItems(ref, inventory.getStorage(), accessor);
        dropContainerItems(ref, inventory.getArmor(), accessor);

        ItemContainer utility = inventory.getUtility();
        if (utility != null) {
            dropContainerItems(ref, utility, accessor);
        }

        // drop tools items -> have extras drops from creative, so I just comment because
        // in my test this doesn't have influence
//        ItemContainer tools = inventory.getTools();
//        if (tools != null) {
//            dropContainerItems(ref, tools, accessor);
//        }

        // drop backpack items if present
        ItemContainer backpack = inventory.getBackpack();
        if (backpack != null) {
            dropContainerItems(ref, backpack, accessor);
        }
    }

    /**
     * drops all items from a specific container
     */
    private void dropContainerItems(Ref<EntityStore> ref, ItemContainer container, ComponentAccessor<EntityStore> accessor) {
        if (container == null) {
            return;
        }

        // use the container dropAllItemStacks method which returns the items and clears slots
        List<ItemStack> droppedItems = container.dropAllItemStacks();

        // spawn each item in the world
        for (ItemStack itemStack : droppedItems) {
            if (itemStack != null && !itemStack.isEmpty()) {
                ItemUtils.dropItem(ref, itemStack, accessor);
            }
        }
    }


    private void killPlayer(Ref<EntityStore> ref, Store<EntityStore> store) {
        Damage damage = new Damage(
            Damage.NULL_SOURCE,
            DamageCause.PHYSICAL,
            Float.MAX_VALUE
        );

        DeathComponent.tryAddComponent(store, ref, damage);
    }

    public void markInCombat(UUID playerUuid) {
        boolean wasInCombat = isInCombat(playerUuid);
        pvpCombatMap.put(playerUuid, Instant.now());

        // Only notify if player was not already in combat
        if (!wasInCombat) {
            notifyPlayerEnteredCombat(playerUuid);
        }
    }

    public boolean isInCombat(UUID playerUuid) {
        Instant lastCombatTime = pvpCombatMap.get(playerUuid);
        if (lastCombatTime == null) {
            return false;
        }
        Duration timeSinceCombat = Duration.between(lastCombatTime, Instant.now());
        return timeSinceCombat.getSeconds() <= config.getCombatTagDurationSeconds();
    }

    public long getCombatTagDuration() {
        return config.getCombatTagDurationSeconds();
    }

    public PluginConfig getConfig() {
        return config;
    }

    /**
     * damageEventSystem that tracks PVP combat
     */
    public class PvpCombatTracker extends DamageEventSystem {

        @Override
        @Nullable
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getInspectDamageGroup();
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }

        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                          @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                          @Nonnull Damage damage) {

            if (damage.isCancelled() || damage.getAmount() <= 0) {
                return;
            }

            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource)) {
                return;
            }

            Damage.EntitySource entitySource = (Damage.EntitySource) source;
            Ref<EntityStore> attackerRef = entitySource.getRef();

            if (!attackerRef.isValid()) {
                return;
            }

            PlayerRef attackerPlayerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (attackerPlayerRef == null) {
                return;
            }

            PlayerRef victimPlayerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (victimPlayerRef == null) {
                return;
            }

            UUID victimUuid = victimPlayerRef.getUuid();
            UUID attackerUuid = attackerPlayerRef.getUuid();

            // Tag both the victim and the attacker
            markInCombat(victimUuid);
            markInCombat(attackerUuid);

            main.getLogger().at(Level.FINE).log("[RibsCombatLog] Players %s and %s are now combat tagged",
                victimPlayerRef.getUsername(), attackerPlayerRef.getUsername());
        }
    }
}
