# RibsCombatLog
RibsCombatLog is a Hytale plugin designed to prevent players from avoiding PvP consequences by disconnecting during combat. When a player leaves the server voluntarily while tagged, they are automatically killed and their items are dropped at their location.

  ## Features
  - **Combat Tagging** - Players are tagged when they hit or get hit by another player.
  - **Essentials Plugins Compatibility** - Prevents other plugins (like HyCommands) from overwriting blocked commands, ensuring they remain restricted during combat.
  - **Both Players Tagged** - Both attacker and victim are tagged in combat.
  - **Combat Log Punishment** - If a player disconnects while in combat, they are killed and their items are dropped.
  - **Configurable Duration** - Combat tag duration is configurable (default: 15 seconds).
  - **Combat Enter/Leave Message** - Shows messages when entering pvp and combat tag expires. This are customizable.
  - **Command Blocking** - Blocks specified commands while in combat (e.g., `/spawn`, `/teleport`, `/warp`).
  - **Configurable Blocked Commands** - List of blocked commands is configurable in `config.json`.

## Config
When the plugin starts, it will create the following directory and file: `<dataDirectory>/RibsCombatLog/config.json`. The default value for the duration is 15 seconds.

### Default config.json
```json
{
  "combatTagDurationSeconds": 15,
  "combatEnterMessage": "[RibsCombatLog] You are in pvp",
  "combatLeaveMessage": "[RibsCombatLog] You are not more in pvp",
  "commandBlockedMessage": "[RibsCombatLog] You cannot use this command while in combat!",
  "blockedCommands": [
    "spawn",
    "teleport",
    "warp",
    "home"
  ]
}
```

## Instruction
You can either download the pre-compiled .jar file directly, or modify the source code to suit your needs and compile it yourself.
### Compilation
To compile the plugin, use the following command:
```bash
mvn clean package -DskipTests
```

### Instalation
1. Locate the generated .jar file in the `target/` directory.
2. Copy the .jar file and paste it into the `mods/` folder of your Hytale server.
3. Restart the server to load the plugin.

## Changelog
### v1.0.0
* Plugin created with following drops: hotbar, storage, armor and backpack.
* Configuration file was created.

### v1.0.1
* Add enter/leave combat messages to notify players of combat status
* Add command blocking system to prevent teleport commands during combat
* Add configurable blocked commands list (spawn, teleport, warp, home)
* Add /rcombatlog reload command to reload configuration
* Add /rcombatlog status command to view current settings
* Tag both attacker and victim when PvP damage occurs
* Delay command wrapping until AllWorldsLoadedEvent for proper plugin load order

### v1.0.2
* Essentials Plugins Compatibility: Improved how the plugin handles commands like /spawn or /tpa to ensure they are always blocked during combat, even if other essentials plugins (like HyCommands) try to take control of them.
* Smart Re-checking: Added a background system that monitors and re-secures blocked commands shortly after the server starts.
* Platform Update: Updated internal dependencies to the latest (17/01/25) Hytale Server version (2026.01.17-4b0f30090).
* Performance: Optimized how the plugin tracks and restores original server commands.
